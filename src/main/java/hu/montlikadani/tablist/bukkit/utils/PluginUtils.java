package hu.montlikadani.tablist.bukkit.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Containers.CMIUser;
import com.earth2me.essentials.Essentials;

import de.myzelyam.api.vanish.VanishAPI;
import hu.montlikadani.ragemode.gameUtils.GameUtils;
import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.config.ConfigValues;
import ru.tehkode.permissions.bukkit.PermissionsEx;

public class PluginUtils {

	private static TabList plugin = TabList.getInstance();

	public static boolean isAfk(Player p) {
		if (plugin.isPluginEnabled("Essentials")) {
			return JavaPlugin.getPlugin(Essentials.class).getUser(p).isAfk();
		}

		if (plugin.isPluginEnabled("CMI")) {
			CMIUser user = CMI.getInstance().getPlayerManager().getUser(p);
			return user != null && user.isVanished();
		}

		return false;
	}

	public static boolean isVanished(Player p) {
		if (plugin.isPluginEnabled("SuperVanish") || plugin.isPluginEnabled("PremiumVanish")) {
			return VanishAPI.isInvisible(p);
		}

		if (plugin.isPluginEnabled("Essentials")) {
			return JavaPlugin.getPlugin(Essentials.class).getUser(p).isVanished();
		}

		if (plugin.isPluginEnabled("CMI")) {
			CMIUser user = CMI.getInstance().getPlayerManager().getUser(p);
			return user != null && user.isAfk();
		}

		return false;
	}

	public static int countVanishedPlayers() {
		final int plSize = Bukkit.getOnlinePlayers().size();

		if (!ConfigValues.isIgnoreVanishedPlayers()) {
			return plSize;
		}

		if (plugin.isPluginEnabled("SuperVanish") || plugin.isPluginEnabled("PremiumVanish")) {
			return VanishAPI.getInvisiblePlayers().isEmpty() ? plSize : plSize - VanishAPI.getInvisiblePlayers().size();
		}

		if (plugin.isPluginEnabled("Essentials")) {
			Essentials ess = JavaPlugin.getPlugin(Essentials.class);
			return ess.getVanishedPlayers().isEmpty() ? plSize : plSize - ess.getVanishedPlayers().size();
		}

		if (plugin.isPluginEnabled("CMI") && CMI.getInstance() != null) {
			CMI cmi = CMI.getInstance();
			return cmi.getVanishManager().getAllVanished().isEmpty() ? plSize
					: plSize - cmi.getVanishManager().getAllVanished().size();
		}

		return plSize;
	}

	public static int getVanishedPlayers() {
		if (plugin.isPluginEnabled("Essentials")) {
			return JavaPlugin.getPlugin(Essentials.class).getVanishedPlayers().size();
		}

		if (plugin.isPluginEnabled("SuperVanish") || plugin.isPluginEnabled("PremiumVanish")) {
			return VanishAPI.getInvisiblePlayers().size();
		}

		if (plugin.isPluginEnabled("CMI") && CMI.getInstance() != null) {
			return CMI.getInstance().getVanishManager().getAllVanished().size();
		}

		return 0;
	}

	public static String getNickName(Player player) {
		String nick = player.getName();

		if (plugin.isPluginEnabled("Essentials")) {
			nick = JavaPlugin.getPlugin(Essentials.class).getUser(player).getNickname();
			if (nick != null) {
				nick = nick.replace("§x", "#").replace("§", "");
			}
		}

		if (plugin.isPluginEnabled("CMI") && CMI.getInstance() != null) {
			nick = CMI.getInstance().getNickNameManager().getNickNameFormat();
		}

		return nick == null ? player.getName() : nick;
	}

	public static boolean hasPermission(Player player, String perm) {
		if (perm.isEmpty())
			return false;

		if (plugin.isPluginEnabled("PermissionsEx")) {
			try {
				return PermissionsEx.getPermissionManager().has(player, perm);
			} catch (Throwable e) {
				// Pex2 supports bukkit provided "hasPermission" check
			}
		}

		return player.isPermissionSet(perm) && player.hasPermission(perm);
	}

	public static boolean isInGame(Player p) {
		return ConfigValues.isRagemodeHook() && plugin.isPluginEnabled("RageMode") && GameUtils.isPlayerPlaying(p)
				&& GameUtils.getGameByPlayer(p).isGameRunning();
	}
}
