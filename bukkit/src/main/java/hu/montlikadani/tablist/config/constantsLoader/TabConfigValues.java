package hu.montlikadani.tablist.config.constantsLoader;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.configuration.file.FileConfiguration;

public final class TabConfigValues {

	private static boolean enabled, rememberToggledTablistToFile, hideTabWhenPlayerVanished, random;

	private static int updateInterval;

	private static List<String> disabledWorlds, blackListedPlayers;

	private static String[] defaultHeader, defaultFooter;

	private static final Set<String> perWorldKeys = new HashSet<>();
	private static final Map<String, String> permissionKeys = new HashMap<>();

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

		defaultHeader = stringToArrayConversion(c.get("header", null));
		defaultFooter = stringToArrayConversion(c.get("footer", null));

		org.bukkit.configuration.ConfigurationSection section = c.getConfigurationSection("per-world");
		if (section != null) {
			perWorldKeys.addAll(section.getKeys(false));
		}

		if ((section = c.getConfigurationSection("permissions")) != null) {
			for (String key : section.getKeys(false)) {
				permissionKeys.put(key, section.getString(key + ".name", "tablist.permissionName"));
			}
		}

		if ((section = c.getConfigurationSection("custom-variables")) != null) {
			for (String name : section.getKeys(true)) {
				CUSTOM_VARIABLES.put(name, section.getString(name, ""));
			}
		}
	}

	public static String[] stringToArrayConversion(Object obj) {
		if (obj instanceof List) {
			List<?> list = (List<?>) obj;

			return list.toArray(new String[list.size()]);
		}

		if (obj instanceof String) {
			return new String[] { (String) obj };
		}

		return null;
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

	public static Map<String, String> getPermissionkeys() {
		return permissionKeys;
	}
}
