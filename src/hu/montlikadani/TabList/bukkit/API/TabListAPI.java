package hu.montlikadani.tablist.API;

import java.lang.reflect.Field;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import hu.montlikadani.tablist.ReflectionUtils;
import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.TabTitle;

public class TabListAPI {

	/**
	 * Returns TabListAPI as a plugin
	 *
	 * @return Plugin
	 */
	public static Plugin getPlugin() {
		if (TabList.getPlugin(TabList.class) == null) {
			throw new NullPointerException("plugin is null");
		}
		return TabList.getPlugin(TabList.class);
	}

	/**
	 * Sends the tab header and footer to player
	 * 
	 * @param player Player
	 * @param string Header
	 * @param string Footer
	 */
	public static void sendTabList(Player p, String header, String footer) {
		if (p == null) {
			throw new IllegalArgumentException("player is null");
		}
		TabTitle.sendTabTitle(p, header, footer);
	}

	/**
	 * Sends the tab header and footer to all online players
	 * 
	 * @param string Header
	 * @param string Footer
	 */
	public static void sendTabList(String header, String footer) {
		for (Player pls : Bukkit.getOnlinePlayers()) {
			TabTitle.sendTabTitle(pls, header, footer);
		}
	}

	/**
	 * Gets the current ping of player
	 * 
	 * @throws Throwable
	 * @param player Player
	 * @return Ping integer
	 */
	public static int getPing(Player p) throws Throwable {
		if (p == null) {
			throw new IllegalArgumentException("player is null");
		}
		int pingInt = 0;
		Object nmsPlayer = ReflectionUtils.getNMSPlayer(p);
		Field ping = ReflectionUtils.getField(nmsPlayer.getClass(), "ping", false);
		pingInt = ping.getInt(nmsPlayer);
		return pingInt;
	}
}
