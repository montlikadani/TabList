package hu.montlikadani.tablist.bukkit.utils;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Containers.CMIUser;

import de.myzelyam.api.vanish.VanishAPI;
import hu.montlikadani.ragemode.gameUtils.GameUtils;
import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.api.TabListAPI;
import hu.montlikadani.tablist.bukkit.config.constantsLoader.ConfigValues;
import me.xtomyserrax.StaffFacilities.SFAPI;
import net.ess3.api.IEssentials;

public final class PluginUtils {

	private static final TabList PLUGIN = TabListAPI.getPlugin();
	private static boolean rmEnabled = false;

	static {
		rmEnabled = PLUGIN.isPluginEnabled("RageMode");
	}

	public static boolean isAfk(Player player) {
		if (player == null) {
			return false;
		}

		Plugin ess = PLUGIN.getServer().getPluginManager().getPlugin("Essentials");
		if (ess != null && ess.isEnabled()) {
			return ((IEssentials) ess).getUser(player).isAfk();
		}

		if (PLUGIN.isPluginEnabled("CMI")) {
			CMIUser user = CMI.getInstance().getPlayerManager().getUser(player);
			return user != null && user.isAfk();
		}

		return false;
	}

	public static boolean isVanished(Player player) {
		Plugin ess = PLUGIN.getServer().getPluginManager().getPlugin("Essentials");
		if (ess != null && ess.isEnabled()) {
			return ((IEssentials) ess).getUser(player).isVanished();
		}

		if (PLUGIN.isPluginEnabled("SuperVanish") || PLUGIN.isPluginEnabled("PremiumVanish")) {
			return VanishAPI.isInvisible(player);
		}

		if (PLUGIN.isPluginEnabled("CMI")) {
			CMIUser user = CMI.getInstance().getPlayerManager().getUser(player);
			return user != null && user.isVanished();
		}

		if (PLUGIN.isPluginEnabled("StaffFacilities")) {
			return SFAPI.isPlayerVanished(player);
		}

		return false;
	}

	public static int countVanishedPlayers() {
		final int plSize = PLUGIN.getServer().getOnlinePlayers().size();

		if (!ConfigValues.isIgnoreVanishedPlayers()) {
			return plSize;
		}

		int vanishedPlayers = getVanishedPlayers();
		return vanishedPlayers == 0 ? plSize : plSize - vanishedPlayers;
	}

	public static int getVanishedPlayers() {
		Plugin ess = PLUGIN.getServer().getPluginManager().getPlugin("Essentials");
		if (ess != null && ess.isEnabled()) {
			return ((IEssentials) ess).getVanishedPlayersNew().size();
		}

		if (PLUGIN.isPluginEnabled("SuperVanish") || PLUGIN.isPluginEnabled("PremiumVanish")) {
			return VanishAPI.getInvisiblePlayers().size();
		}

		if (PLUGIN.isPluginEnabled("CMI")) {
			return CMI.getInstance().getVanishManager().getAllVanished().size();
		}

		if (PLUGIN.isPluginEnabled("StaffFacilities")) {
			return SFAPI.vanishedPlayersList().size();
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

		return player.hasPermission(perm);
	}

	public static boolean isInGame(Player p) {
		if (rmEnabled && (hu.montlikadani.ragemode.config.ConfigValues.isTabEnabled()
				|| hu.montlikadani.ragemode.config.ConfigValues.isTabFormatEnabled())) {
			hu.montlikadani.ragemode.gameLogic.Game game = GameUtils.getGameByPlayer(p);

			return game != null && game.isGameRunning();
		}

		return false;
	}
}
