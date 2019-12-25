package hu.montlikadani.tablist.API;

import java.lang.reflect.Field;
import java.util.UUID;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import hu.montlikadani.tablist.Commands;
import hu.montlikadani.tablist.ReflectionUtils;
import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.TabTitle;

public class TabListAPI {

	/**
	 * Returns TabListAPI as a plugin
	 * @return Plugin
	 */
	public static Plugin getPlugin() {
		return JavaPlugin.getPlugin(TabList.class);
	}

	/**
	 * Gets the plugin instance.
	 * @return TabList instance
	 */
	public static TabList getInstance() {
		return TabList.getInstance();
	}

	/**
	 * Checks whatever the tablist toggled for the specified player uuid.
	 * This is much slower to respond because it converts the string to uuid.
	 * @param uuid Player UUID
	 * @return true if toggled
	 */
	public static boolean isTabListToggledForPlayer(String uuid) {
		Validate.notNull(uuid, "Player UUID can't be null!");
		Validate.notEmpty(uuid, "Player UUID can't be empty!");

		return isTabListToggledForPlayer(UUID.fromString(uuid));
	}

	/**
	 * Checks whatever the tablist toggled for the specified player uuid.
	 * @param uuid Player UUID
	 * @return true if toggled
	 */
	public static boolean isTabListToggledForPlayer(UUID uuid) {
		Validate.notNull(uuid, "Player UUID can't be null!");

		return Commands.enabled != null && Commands.enabled.containsKey(uuid) && Commands.enabled.get(uuid);
	}

	/**
	 * Sends the tab header and footer to the given player
	 * @param p Player
	 * @param string Header
	 * @param string Footer
	 */
	public static void sendTabList(Player p, String header, String footer) {
		Validate.notNull(p, "Player can't be null!");

		TabTitle.sendTabTitle(p, header, footer);
	}

	/**
	 * Sends the tab header and footer to all online players
	 * @param string Header
	 * @param string Footer
	 */
	public static void sendTabList(String header, String footer) {
		Bukkit.getOnlinePlayers().forEach(pls -> TabTitle.sendTabTitle(pls, header, footer));
	}

	/**
	 * Gets the current ping of player
	 * @throws Throwable
	 * @param player Player
	 * @return Ping integer
	 */
	public static int getPing(Player p) throws Throwable {
		Validate.notNull(p, "Player can't be null!");

		int pingInt = 0;
		Object nmsPlayer = ReflectionUtils.getNMSPlayer(p);
		Field ping = ReflectionUtils.getField(nmsPlayer.getClass(), "ping", false);
		pingInt = ping.getInt(nmsPlayer);
		return pingInt;
	}
}
