package hu.montlikadani.tablist.bukkit.utils;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Containers.CMIUser;
import com.earth2me.essentials.Essentials;

import de.myzelyam.api.vanish.VanishAPI;
import hu.montlikadani.ragemode.gameUtils.GameUtils;
import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.API.TabListAPI;
import hu.montlikadani.tablist.bukkit.config.constantsLoader.ConfigValues;

public final class PluginUtils {

	private static final TabList PLUGIN = TabListAPI.getPlugin();

	public static boolean isAfk(Player p) {
		if (PLUGIN.isPluginEnabled("Essentials")) {
			return JavaPlugin.getPlugin(Essentials.class).getUser(p).isAfk();
		}

		if (PLUGIN.isPluginEnabled("CMI")) {
			CMIUser user = CMI.getInstance().getPlayerManager().getUser(p);
			return user != null && user.isAfk();
		}

		return false;
	}

	public static boolean isVanished(Player p) {
		if (PLUGIN.isPluginEnabled("SuperVanish") || PLUGIN.isPluginEnabled("PremiumVanish")) {
			return VanishAPI.isInvisible(p);
		}

		if (PLUGIN.isPluginEnabled("Essentials")) {
			return JavaPlugin.getPlugin(Essentials.class).getUser(p).isVanished();
		}

		if (PLUGIN.isPluginEnabled("CMI")) {
			CMIUser user = CMI.getInstance().getPlayerManager().getUser(p);
			return user != null && user.isVanished();
		}

		return false;
	}

	public static int countVanishedPlayers() {
		final int plSize = org.bukkit.Bukkit.getOnlinePlayers().size();

		if (!ConfigValues.isIgnoreVanishedPlayers()) {
			return plSize;
		}

		int vanishedPlayers = getVanishedPlayers();
		return vanishedPlayers == 0 ? plSize : plSize - vanishedPlayers;
	}

	public static int getVanishedPlayers() {
		if (PLUGIN.isPluginEnabled("Essentials")) {
			return JavaPlugin.getPlugin(Essentials.class).getVanishedPlayers().size();
		}

		if (PLUGIN.isPluginEnabled("SuperVanish") || PLUGIN.isPluginEnabled("PremiumVanish")) {
			return VanishAPI.getInvisiblePlayers().size();
		}

		if (PLUGIN.isPluginEnabled("CMI") && CMI.getInstance() != null) {
			return CMI.getInstance().getVanishManager().getAllVanished().size();
		}

		return 0;
	}

	public static boolean hasPermission(Player player, String perm) {
		if (perm.isEmpty())
			return false;

		if (PLUGIN.isPluginEnabled("PermissionsEx")) {
			try {
				return ru.tehkode.permissions.bukkit.PermissionsEx.getPermissionManager().has(player, perm);
			} catch (Throwable e) {
				// Pex2 supports bukkit provided "hasPermission" check
			}
		}

		return player.isPermissionSet(perm) && player.hasPermission(perm);
	}

	public static boolean isInGame(Player p) {
		return PLUGIN.isPluginEnabled("RageMode")
				&& (hu.montlikadani.ragemode.config.ConfigValues.isTabEnabled()
						|| hu.montlikadani.ragemode.config.ConfigValues.isTabFormatEnabled())
				&& GameUtils.isPlayerPlaying(p) && GameUtils.getGameByPlayer(p).isGameRunning();
	}
}
