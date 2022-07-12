package hu.montlikadani.tablist.config.constantsLoader;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.configuration.file.FileConfiguration;

import hu.montlikadani.tablist.tablist.TabText;

public final class TabConfigValues {

	private static boolean enabled, rememberToggledTablistToFile, hideTabWhenPlayerVanished, random;

	private static int updateInterval;

	private static List<String> disabledWorlds, blackListedPlayers;

	private static TabText[] defaultHeader, defaultFooter;

	private static final Set<String> perWorldKeys = new HashSet<>();
	private static final Map<String, String> permissionKeys = new HashMap<>();

	public static void loadValues(FileConfiguration c) {
		perWorldKeys.clear();
		permissionKeys.clear();

		enabled = c.getBoolean("enabled", true);
		rememberToggledTablistToFile = c.getBoolean("remember-toggled-tablist-to-file", true);
		hideTabWhenPlayerVanished = c.getBoolean("hide-tab-when-player-vanished", false);
		random = c.getBoolean("random", false);
		disabledWorlds = c.getStringList("disabled-worlds");
		blackListedPlayers = c.getStringList("blacklisted-players");

		if ((updateInterval = c.getInt("interval", 4)) > 10000) {
			updateInterval = 10000;
		}

		defaultHeader = objectToArrayConversion(c.get("header", null));
		defaultFooter = objectToArrayConversion(c.get("footer", null));

		org.bukkit.configuration.ConfigurationSection section = c.getConfigurationSection("per-world");
		if (section != null) {
			perWorldKeys.addAll(section.getKeys(false));
		}

		if ((section = c.getConfigurationSection("permissions")) != null) {
			for (String key : section.getKeys(false)) {
				permissionKeys.put(key, section.getString(key + ".name", "tablist.permissionName"));
			}
		}
	}

	public static TabText[] objectToArrayConversion(Object obj) {
		if (obj instanceof List) {
			String[] array = ((List<?>) obj).toArray(new String[0]);
			TabText[] tt = new TabText[array.length];

			for (int i = 0; i < array.length; i++) {
				TabText text = new TabText();
				text.setPlainText(array[i]);
				tt[i] = text;
			}

			return tt;
		}

		if (obj instanceof String) {
			TabText text = new TabText();
			text.setPlainText((String) obj);

			return new TabText[] { text };
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

	public static TabText[] getDefaultHeader() {
		return defaultHeader;
	}

	public static TabText[] getDefaultFooter() {
		return defaultFooter;
	}

	public static Set<String> getPerWorldkeys() {
		return perWorldKeys;
	}

	public static Map<String, String> getPermissionkeys() {
		return permissionKeys;
	}
}
