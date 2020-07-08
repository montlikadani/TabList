package hu.montlikadani.tablist.bukkit.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Containers.CMIUser;
import com.earth2me.essentials.Essentials;

import ca.stellardrift.permissionsex.bukkit.PermissionsExPlugin;
import de.myzelyam.api.vanish.VanishAPI;
import hu.montlikadani.tablist.bukkit.ConfigValues;
import hu.montlikadani.tablist.bukkit.TabList;
import ru.tehkode.permissions.bukkit.PermissionsEx;

public class PluginUtils {

	public static boolean isAfk(Player p) {
		if (TabList.getInstance().isPluginEnabled("Essentials")) {
			return JavaPlugin.getPlugin(Essentials.class).getUser(p).isAfk();
		}

		if (TabList.getInstance().isPluginEnabled("CMI")) {
			CMIUser user = CMI.getInstance().getPlayerManager().getUser(p);
			return user != null && user.isVanished();
		}

		return false;
	}

	public static boolean isVanished(Player p) {
		if (TabList.getInstance().isPluginEnabled("SuperVanish")) {
			return VanishAPI.isInvisible(p);
		}

		if (TabList.getInstance().isPluginEnabled("Essentials")) {
			return JavaPlugin.getPlugin(Essentials.class).getUser(p).isVanished();
		}

		if (TabList.getInstance().isPluginEnabled("CMI")) {
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

		if (TabList.getInstance().isPluginEnabled("SuperVanish")) {
			return VanishAPI.getInvisiblePlayers().isEmpty() ? plSize : plSize - VanishAPI.getInvisiblePlayers().size();
		}

		if (TabList.getInstance().isPluginEnabled("Essentials")) {
			Essentials ess = JavaPlugin.getPlugin(Essentials.class);
			return ess.getVanishedPlayers().isEmpty() ? ess.getOnlinePlayers().size()
					: ess.getOnlinePlayers().size() - ess.getVanishedPlayers().size();
		}

		if (TabList.getInstance().isPluginEnabled("CMI") && CMI.getInstance() != null) {
			CMI cmi = CMI.getInstance();
			return cmi.getVanishManager().getAllVanished().isEmpty() ? plSize
					: plSize - cmi.getVanishManager().getAllVanished().size();
		}

		return plSize;
	}

	public static int getVanishedPlayers() {
		if (TabList.getInstance().isPluginEnabled("Essentials")) {
			return JavaPlugin.getPlugin(Essentials.class).getVanishedPlayers().size();
		}

		if (TabList.getInstance().isPluginEnabled("SuperVanish")) {
			return VanishAPI.getInvisiblePlayers().size();
		}

		if (TabList.getInstance().isPluginEnabled("CMI") && CMI.getInstance() != null) {
			return CMI.getInstance().getVanishManager().getAllVanished().size();
		}

		return 0;
	}

	public static String getNickName(Player player) {
		if (TabList.getInstance().isPluginEnabled("Essentials")) {
			return JavaPlugin.getPlugin(Essentials.class).getUser(player).getNickname();
		}

		if (TabList.getInstance().isPluginEnabled("CMI") && CMI.getInstance() != null) {
			return CMI.getInstance().getNickNameManager().getNickNameFormat();
		}

		return "";
	}

	public static boolean hasPermission(Player player, String perm) {
		if (perm.isEmpty())
			return false;

		if (TabList.getInstance().isPluginEnabled("PermissionsEx")) {
			try {
				return PermissionsEx.getPermissionManager().has(player, perm);
			} catch (Exception e) {
				return JavaPlugin.getPlugin(PermissionsExPlugin.class).getUserSubjects()
						.get(player.getUniqueId().toString()).thenAccept(u -> u.hasPermission(perm))
						.completeExceptionally(e.getCause());
			}
		}

		return player.isPermissionSet(perm) && player.hasPermission(perm);
	}
}
