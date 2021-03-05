package hu.montlikadani.tablist.bukkit.config.constantsLoader;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hu.montlikadani.tablist.bukkit.config.CommentedConfig;

public final class TabConfigValues {

	private static boolean enabled, rememberToggledTablistToFile, hideTabWhenPlayerVanished, random;

	private static int updateInterval;

	private static List<String> disabledWorlds, blackListedPlayers;

	public static final Map<String, String> CUSTOM_VARIABLES = new HashMap<>();

	public static void loadValues(CommentedConfig c) {
		CUSTOM_VARIABLES.clear();

		enabled = c.get("enabled", true);
		rememberToggledTablistToFile = c.get("remember-toggled-tablist-to-file", true);
		hideTabWhenPlayerVanished = c.get("hide-tab-when-player-vanished", false);
		random = c.get("random", false);
		updateInterval = c.get("interval", 4);
		disabledWorlds = c.get("disabled-worlds", Arrays.asList());
		blackListedPlayers = c.get("blacklisted-players", Arrays.asList());

		if (c.isConfigurationSection("custom-variables")) {
			for (String name : c.getConfigurationSection("custom-variables").getKeys(true)) {
				CUSTOM_VARIABLES.put(name, c.get("custom-variables." + name, ""));
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
}
