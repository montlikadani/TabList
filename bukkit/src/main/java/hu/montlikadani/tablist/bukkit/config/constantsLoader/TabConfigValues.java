package hu.montlikadani.tablist.bukkit.config.constantsLoader;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.configuration.file.FileConfiguration;

import hu.montlikadani.tablist.bukkit.utils.Util;

public final class TabConfigValues {

	private static boolean enabled, rememberToggledTablistToFile, hideTabWhenPlayerVanished, random;

	private static int updateInterval;

	private static List<String> disabledWorlds, blackListedPlayers;

	private static String[] defaultHeader, defaultFooter;

	private static final Set<String> perWorldKeys = new HashSet<>(), permissionKeys = new HashSet<>();

	public static final Map<String, String> CUSTOM_VARIABLES = new HashMap<>();

	public static void loadValues(FileConfiguration c) {
		CUSTOM_VARIABLES.clear();
		perWorldKeys.clear();
		permissionKeys.clear();

		enabled = c.getBoolean("enabled", true);
		rememberToggledTablistToFile = c.getBoolean("remember-toggled-tablist-to-file", true);
		hideTabWhenPlayerVanished = c.getBoolean("hide-tab-when-player-vanished");
		random = c.getBoolean("random");
		updateInterval = c.getInt("interval", 4);
		disabledWorlds = c.getStringList("disabled-worlds");
		blackListedPlayers = c.getStringList("blacklisted-players");

		defaultHeader = c.isList("header") ? c.getStringList("header").toArray(new String[0])
				: c.isString("header") ? Util.toArray(c.getString("header")) : null;
		defaultFooter = c.isList("footer") ? c.getStringList("footer").toArray(new String[0])
				: c.isString("footer") ? Util.toArray(c.getString("footer")) : null;

		org.bukkit.configuration.ConfigurationSection section = c.getConfigurationSection("per-world");
		if (section != null) {
			perWorldKeys.addAll(section.getKeys(false));
		}

		if ((section = c.getConfigurationSection("permissions")) != null) {
			permissionKeys.addAll(section.getKeys(false));
		}

		if ((section = c.getConfigurationSection("custom-variables")) != null) {
			for (String name : section.getKeys(true)) {
				CUSTOM_VARIABLES.put(name, section.getString(name, ""));
			}
		}
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static boolean isRememberToggledTablistToFile() {
		return rememberToggledTablistToFile;
	}

	public static boolean isHideTabWhenPlayerVanished() {
		return hideTabWhenPlayerVanished;
	}

	public static boolean isRandom() {
		return random;
	}

	public static int getUpdateInterval() {
		return updateInterval;
	}

	public static List<String> getDisabledWorlds() {
		return disabledWorlds;
	}

	public static List<String> getBlackListedPlayers() {
		return blackListedPlayers;
	}

	public static String[] getDefaultHeader() {
		return defaultHeader;
	}

	public static String[] getDefaultFooter() {
		return defaultFooter;
	}

	public static Set<String> getPerWorldkeys() {
		return perWorldKeys;
	}

	public static Set<String> getPermissionkeys() {
		return permissionKeys;
	}
}
