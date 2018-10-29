package hu.montlikadani.TabList.bukkit.API;

import java.lang.reflect.Field;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import hu.montlikadani.TabList.bukkit.TabList;
import hu.montlikadani.TabList.bukkit.TabTitle;

public class TabListAPI {

	/**
	 * Returns TabListAPI as a plugin
	 *
	 * @return Plugin
	 */
	public static Plugin getPlugin() {
		return TabList.getPlugin(TabList.class);
	}

	/**
	 * Sending the tab header and footer to player if the boolean false
	 * @param player Player
	 */
	public static void sendTabList(Player p, String header, String footer) {
		if (p == null) {
			throw new IllegalArgumentException("player is null");
		}
		if (header == null || header == "") {
			TabTitle.sendTabTitle(p, "", footer);
			return;
		}
		if (footer == null || footer == "") {
			TabTitle.sendTabTitle(p, header, "");
			return;
		}
		if (TabList.getInstance().getConfig() != null) {
			if (!TabList.getInstance().getConfig().getBoolean("tablist.enable")) {
				TabTitle.sendTabTitle(p, header, footer);
			}
		} else {
			TabTitle.sendTabTitle(p, header, footer);
		}
	}

	/**
	 * Clear the tab header and footer from player
	 * @param player Player
	 */
	public static void clearTabList(Player p) {
		if (p == null) {
			throw new IllegalArgumentException("player is null");
		}
		TabTitle.sendEmptyTabTitle(p);
	}

	/**
	 * Gets the current ping of player
	 * 
	 * @throws Exception
	 * @param player Player
	 * @return Ping integer
	 */
	public static int getPing(Player p) throws Exception {
		if (p == null) {
			throw new IllegalArgumentException("player is null");
		}
		int pingInt = 0;
		Object nmsPlayer = getNMSPlayer(p);
		try {
			Class.forName("org.spigotmc.SpigotConfig");
			Field ping = nmsPlayer.getClass().getField("ping");
			ping.setAccessible(true);
			pingInt = ping.getInt(nmsPlayer);
		} catch (ClassNotFoundException ex) {
			Field field = nmsPlayer.getClass().getDeclaredField("ping");
			field.setAccessible(true);
			pingInt = field.getInt(nmsPlayer);
		}
		return pingInt;
	}

	private static Object getNMSPlayer(Player p) {
		try {
			return p.getClass().getMethod("getHandle", new Class[0]).invoke(p, new Object[0]);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
