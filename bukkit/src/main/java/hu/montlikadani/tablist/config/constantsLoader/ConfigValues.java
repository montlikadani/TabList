package hu.montlikadani.tablist.config.constantsLoader;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;

import hu.montlikadani.tablist.Global;
import hu.montlikadani.tablist.Objects;
import hu.montlikadani.tablist.config.CommentedConfig;

public final class ConfigValues {

	private static boolean logConsole = true, placeholderAPI, perWorldPlayerList, fakePlayers, countFakePlayersToOnlinePlayers,
			removeGrayColorFromTabInSpec, ignoreVanishedPlayers, countVanishedStaff, hidePlayerFromTabAfk, hidePlayersFromTab,
			afkStatusEnabled, afkStatusShowInRightLeftSide, afkStatusShowPlayerGroup, afkSortLast, useSystemZone,
			pingFormatEnabled, tpsFormatEnabled, prefixSuffixEnabled, useDisabledWorldsAsWhiteList, syncPluginsGroups,
			hideGroupInVanish, preferPrimaryVaultGroup, assignGlobalGroup, followNameTagVisibility;

	private static String afkFormatYes, afkFormatNo, timeZone, customObjectSetting, memoryBarChar, memoryBarUsedColor,
			memoryBarFreeColor, memoryBarAllocationColor, memoryBarReleasedColor;

	private static DateTimeFormatter timeFormat, dateFormat;

	private static Objects.ObjectTypes objectType = Objects.ObjectTypes.PING;

	private static List<String> tpsColorFormats, pingColorFormats, groupsDisabledWorlds, healthObjectRestricted,
			objectsDisabledWorlds;

	private static int tpsDigits, groupsRefreshInterval, objectRefreshInterval, memoryBarSize;

	private static double tpsPerformanceObservationValue;

	public static final List<List<String>> PER_WORLD_LIST_NAMES = new java.util.ArrayList<>();

	@SuppressWarnings("serial")
	public static void loadValues(CommentedConfig c) {
		org.bukkit.configuration.file.YamlConfigurationOptions options = c.options();
		options.copyDefaults(true);

		try {
			options.parseComments(false);
		} catch (NoSuchMethodError e) {
		}

		PER_WORLD_LIST_NAMES.clear();

		c.addComment("hook.placeholderapi", "Hook to PlaceholderAPI to use custom placeholders.");
		c.addComment("tps-performance-observation-value",
				"This option monitors server performance. If the server's TPS is less than the set value,",
				"TabList will cancels all currently running schedulers to improve server performance.",
				"TabList will not restart these schedulers (ie animations, group updates, etc.),",
				"so you have to do it manually, by reconnecting to the server or by reloading the plugin.",
				"At values below 8-5, TabList is almost unable to stop its own running processes,",
				"as the server is already under a very heavy load.", "The values should be between 5-18",
				"If the value is below 5 or above 18, the default value will be 16.0", "To disable this feature set to -1");

		c.addComment("fake-players", "Fake players that can be added to the player list.");
		c.addComment("fake-players.count-fake-players-to-online-players",
				"Do we count the added fake players to the %online-players% placeholder?");
		c.addComment("remove-gray-color-from-tab-in-spectator",
				"If enabled, the gray color will not appear to other players when the player's game mode is spectator.",
				"The gray color will only show for the spectator player.", "Requires ProtocolLib!");
		c.addComment("ignore-vanished-players-in-online-players",
				"true - does not count vanished players in %online-players% placeholder.",
				"Requires Essentials, SuperVanish, PremiumVanish or CMI plugin!");
		c.addComment("count-vanished-staffs", "true - count vanished staff in %staff-online% placeholder,",
				"but they need to have \"tablist.onlinestaff\" permission set.",
				"false - does not count vanished staff in the %staff-online% placeholder",
				"Requires Essentials, SuperVanish, PremiumVanish or CMI plugin!");
		c.addComment("hide-player-from-tab-when-afk", "Hide player from player list when a player is AFK?",
				"Requires Essentials or CMI plugin!");
		c.addComment("hide-players-from-tablist", "Hide all players from the player list?",
				"This removes all players from the player list, but the player that has the",
				"group set is retained as it is not changed during removal, so your group",
				"will be restored if this option is disabled.",
				"Requires ProtocolLib to fix view distance issue! (https://github.com/montlikadani/TabList/issues/147)");

		c.addComment("per-world-player-list", "Different player list in different world.");
		c.addComment("per-world-player-list.world-groups", "You can specify worlds, which will share the same list of players");
		c.addComment("per-world-player-list.world-groups.example1", "The key name, can be anything");

		c.addComment("placeholder-format", "Placeholders formatting");
		c.addComment("placeholder-format.afk-status", "When the player changes the AFK status, change his tablist name format?");
		c.addComment("placeholder-format.afk-status.show-in-right-or-left-side",
				"Should the AFK format display in right or left side?", "true - displays in right side",
				"false - displays in left side");
		c.addComment("placeholder-format.afk-status.show-player-group", "Show player's group if the player is AFK?");
		c.addComment("placeholder-format.afk-status.format-yes", "Format when the player is AFK.");
		c.addComment("placeholder-format.afk-status.format-no", "Format when the player is not AFK.");
		c.addComment("placeholder-format.afk-status.sort-last", "Sort AFK players to the bottom of the player list?");
		c.addComment("placeholder-format.time.time-zone",
				"Time zones: https://www.mkyong.com/java/java-display-list-of-timezone-with-gmt/",
				"Or google it: \"what is my time zone\"");
		c.addComment("placeholder-format.time.use-system-zone", "Use system default time zone instead of searching for that?");
		c.addComment("placeholder-format.time.time-format",
				"Formats/examples: https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html",
				"Format of %server-time% placeholder.");
		c.addComment("placeholder-format.time.date-format", "Format of %date% placeholder.");
		c.addComment("placeholder-format.ping", "Ping color format for %ping% placeholder.");
		c.addComment("placeholder-format.ping.formats", "https://github.com/montlikadani/TabList/wiki/Ping-or-tps-formatting");
		c.addComment("placeholder-format.tps", "TPS color format for %tps% placeholder.");
		c.addComment("placeholder-format.tps.formats", "https://github.com/montlikadani/TabList/wiki/Ping-or-tps-formatting");
		c.addComment("placeholder-format.tps.size", "How many numbers do you want to display after \".\" in %tps% placeholder?",
				"The number should be higher than 0.", "Example: 3 = 19.14");
		c.addComment("placeholder-format.memory-bar", "Memory bar settings for %memory_bar% variable");
		c.addComment("placeholder-format.memory-bar.colors.allocation", "When the server memory less than 80");
		c.addComment("placeholder-format.memory-bar.colors.released",
				"When the server memory is on critical level (less than 40) and some resource need memory to run.");

		c.addComment("change-prefix-suffix-in-tablist", "Enable changing of prefix & suffix in player list?");
		c.addComment("change-prefix-suffix-in-tablist.refresh-interval", "Refresh interval in server ticks.",
				"Set to 0 if you don't want to refresh the groups.",
				"If 0, then you will need to execute the /tl reload command to refresh the groups.");
		c.addComment("change-prefix-suffix-in-tablist.disabled-worlds", "Disable groups in these worlds.");
		c.addComment("change-prefix-suffix-in-tablist.disabled-worlds.use-as-whitelist", "Use the list as whitelist?");
		c.addComment("change-prefix-suffix-in-tablist.sync-plugins-groups-with-tablist",
				"Automatically add groups from another plugins to the tablist groups.yml on every reload?",
				"If a plugin does not support Vault, it will not be added.");
		c.addComment("change-prefix-suffix-in-tablist.hide-group-when-player-vanished",
				"Hide player's group in player list when the player is vanished?",
				"Requires Essentials, SuperVanish, PremiumVanish or CMI plugin!");
		c.addComment("change-prefix-suffix-in-tablist.assign-global-group-to-normal",
				"Do you want to assign global group to normal groups?", "true - \"globalGroupPrefix + normalGroupPrefix\"",
				"false - \"normalGroupPrefix\"");
		c.addComment("change-prefix-suffix-in-tablist.prefer-primary-vault-group",
				"Prefer player's primary Vault group when assigning tablist group from groups.yml?",
				"true - player will be assigned their primary vault group where possible",
				"false - applies the group that has the higher priority in the permission plugin");
		c.addComment("change-prefix-suffix-in-tablist.followNameTagVisibility",
				"Follow the name tag visibility for players to show the name tag above player or not,",
				"depending if a scoreboard team with visibility 'hidden' is exist.",
				"true - Follows the name tag visibility and hides if there is a scoreboard team created with visibility 'hidden'",
				"false - Always shows the name tag above player");

		c.addComment("tablist-object-type", "Tablist objective types",
				"Shows your current health (with life indicator), your current ping or any NUMBER placeholder",
				"after the player's name (before the ping indicator).");
		c.addComment("tablist-object-type.type", "Types:", "none - disables tablist objects", "ping - player's ping",
				"health - player's health", "custom - custom placeholder");
		c.addComment("tablist-object-type.refresh-interval", "How often should it refresh the values in seconds?");
		c.addComment("tablist-object-type.disabled-worlds", "In these worlds the objects will not be displayed");
		c.addComment("tablist-object-type.object-settings", "Objective settings");
		c.addComment("tablist-object-type.object-settings.health", "The player's health - displayed after the player's name.");
		c.addComment("tablist-object-type.object-settings.health.restricted-players",
				"For these players the health will not be displayed");
		c.addComment("tablist-object-type.object-settings.custom",
				"Custom placeholder - accepts only number-ending placeholders, like %level%");

		c.addComment("check-update", "Check for updates?");
		c.addComment("download-updates", "Download new releases to \"releases\" folder?",
				"This only works if the \"check-update\" is true.");
		c.addComment("logconsole", "Log plugin messages to console?");

		placeholderAPI = c.get("hook.placeholderapi", true);
		tpsPerformanceObservationValue = c.get("tps-performance-observation-value", -1.0);

		if (tpsPerformanceObservationValue != -1.0
				&& (tpsPerformanceObservationValue < 5.0 || tpsPerformanceObservationValue > 18.0)) {
			tpsPerformanceObservationValue = 16.0;
		}

		fakePlayers = c.get("fake-players.enabled", c.getBoolean("enable-fake-players"));
		countFakePlayersToOnlinePlayers = c.get("fake-players.count-fake-players-to-online-players", false);
		removeGrayColorFromTabInSpec = c.get("remove-gray-color-from-tab-in-spectator", false);
		ignoreVanishedPlayers = c.get("ignore-vanished-players-in-online-players", false);
		countVanishedStaff = c.get("count-vanished-staffs", true);
		hidePlayerFromTabAfk = c.get("hide-player-from-tab-when-afk", false);
		hidePlayersFromTab = c.get("hide-players-from-tablist", false);
		perWorldPlayerList = c.get("per-world-player-list.enabled", c.getBoolean("per-world-player-list"));

		ConfigurationSection section = c.getConfigurationSection("per-world-player-list.world-groups");

		if (section == null) {
			section = c.createSection("per-world-player-list.world-groups", new java.util.HashMap<String, List<String>>() {
				{
					put("exampleGroup2", Arrays.asList("exampleWorld2", "exampleAnotherWorld2"));
					put("example1", Arrays.asList("exampleWorld", "exampleAnotherWorld"));
				}
			});
		} else {
			c.set(section.getCurrentPath(), section);
		}

		if (perWorldPlayerList) {
			for (String key : section.getKeys(false)) {
				List<String> list = section.getStringList(key);

				if (!list.isEmpty()) {
					PER_WORLD_LIST_NAMES.add(list);
				}
			}
		}

		afkStatusEnabled = c.get("placeholder-format.afk-status.enable", false);
		afkStatusShowInRightLeftSide = c.get("placeholder-format.afk-status.show-in-right-or-left-side", true);
		afkStatusShowPlayerGroup = c.get("placeholder-format.afk-status.show-player-group", true);
		afkSortLast = c.get("placeholder-format.afk-status.sort-last", false);
		useSystemZone = c.get("placeholder-format.time.use-system-zone", false);
		pingFormatEnabled = c.get("placeholder-format.ping.enable", true);
		tpsFormatEnabled = c.get("placeholder-format.tps.enable", true);
		prefixSuffixEnabled = c.get("change-prefix-suffix-in-tablist.enable", false);
		useDisabledWorldsAsWhiteList = c.get("change-prefix-suffix-in-tablist.disabled-worlds.use-as-whitelist", false);
		syncPluginsGroups = c.get("change-prefix-suffix-in-tablist.sync-plugins-groups-with-tablist", true);
		hideGroupInVanish = c.get("change-prefix-suffix-in-tablist.hide-group-when-player-vanished", false);
		assignGlobalGroup = c.get("change-prefix-suffix-in-tablist.assign-global-group-to-normal", false);
		preferPrimaryVaultGroup = c.get("change-prefix-suffix-in-tablist.prefer-primary-vault-group", false);
		followNameTagVisibility = c.get("change-prefix-suffix-in-tablist.followNameTagVisibility", false);

		afkFormatYes = Global.setSymbols(c.get("placeholder-format.afk-status.format-yes", "&7 [AFK]&r "));
		afkFormatNo = Global.setSymbols(c.get("placeholder-format.afk-status.format-no", ""));
		timeZone = c.get("placeholder-format.time.time-zone", "GMT0");

		String old = "placeholder-format.time.time-format.format";
		String last = c.getString(old, null);

		if (last != null) {
			c.set(old, null); // Need to remove as this still exists so it will returns memorySection
			c.set("placeholder-format.time.time-format", last);
		}

		String tf = c.get("placeholder-format.time.time-format", "mm:HH");

		if (!tf.isEmpty()) {
			try {
				timeFormat = DateTimeFormatter.ofPattern(tf);
			} catch (IllegalArgumentException e) {
			}
		}

		if ((last = c.getString(old = "placeholder-format.time.date-format.format", null)) != null) {
			c.set(old, null);
			c.set("placeholder-format.time.date-format", last);
		}

		if (!(tf = c.get("placeholder-format.time.date-format", "dd/MM/yyyy")).isEmpty()) {
			try {
				dateFormat = DateTimeFormatter.ofPattern(tf);
			} catch (IllegalArgumentException e) {
			}
		}

		memoryBarChar = c.get("placeholder-format.memory-bar.char", "|");
		memoryBarUsedColor = c.get("placeholder-format.memory-bar.colors.used", "&c");
		memoryBarFreeColor = c.get("placeholder-format.memory-bar.colors.free", "&a");
		memoryBarAllocationColor = c.get("placeholder-format.memory-bar.colors.allocation", "&e");
		memoryBarReleasedColor = c.get("placeholder-format.memory-bar.colors.released", "&6");
		customObjectSetting = c.get("tablist-object-type.object-settings.custom.value", "%level%");

		if (!c.getBoolean("tablist-object-type.enable", true)) {
			c.set("tablist-object-type.type", "none");

			objectType = Objects.ObjectTypes.NONE;
		} else {
			try {
				objectType = Objects.ObjectTypes
						.valueOf(c.get("tablist-object-type.type", "ping").toUpperCase(java.util.Locale.ENGLISH));
			} catch (IllegalArgumentException e) {
			}
		}

		c.set("tablist-object-type.enable", null);

		tpsColorFormats = c.get("placeholder-format.tps.formats",
				Arrays.asList("&a%tps% > 18.0", "&6%tps% == 16.0", "&c%tps% < 16.0"));
		pingColorFormats = c.get("placeholder-format.ping.formats",
				Arrays.asList("&a%ping% <= 200", "&6%ping% >= 200", "&c%ping% > 500"));
		for (String f : pingColorFormats) { // TODO remove in the future
			if (!f.contains("%ping%")) {
				c.set("placeholder-format.ping.formats", Arrays.asList("&a%ping% <= 200", "&6%ping% >= 200", "&c%ping% > 500"));
				break;
			}
		}

		tpsDigits = 10;

		int size = c.get("placeholder-format.tps.size", 2);

		if (size < 1) {
			size = 2;
		} else if (size > 2) {
			size -= 2;

			for (int i = 0; i < size; i++) {
				tpsDigits *= 10;
			}
		}

		groupsDisabledWorlds = c.get("change-prefix-suffix-in-tablist.disabled-worlds.list", Arrays.asList("myWorldWithUpper"));
		healthObjectRestricted = c.get("tablist-object-type.object-settings.health.restricted-players",
				Arrays.asList("exampleplayer", "players"));
		objectsDisabledWorlds = c.get("tablist-object-type.disabled-worlds", Arrays.asList("testingWorld"));

		memoryBarSize = c.get("placeholder-format.memory-bar.size", 80);
		groupsRefreshInterval = c.get("change-prefix-suffix-in-tablist.refresh-interval", 30);
		objectRefreshInterval = c.get("tablist-object-type.refresh-interval", 3) * 20;

		// Just set if missing
		c.get("check-update", true);
		c.get("download-updates", false);

		logConsole = c.get("logconsole", true);

		// Here comes the options that removed
		c.set("change-prefix-suffix-in-tablist.hide-group-when-player-afk", null);

		c.save();
	}

	public static boolean isLogConsole() {
		return logConsole;
	}

	public static String getMemoryBarChar() {
		return memoryBarChar;
	}

	public static String getMemoryBarUsedColor() {
		return memoryBarUsedColor;
	}

	public static String getMemoryBarFreeColor() {
		return memoryBarFreeColor;
	}

	public static String getMemoryBarAllocationColor() {
		return memoryBarAllocationColor;
	}

	public static String getMemoryBarReleasedColor() {
		return memoryBarReleasedColor;
	}

	public static int getMemoryBarSize() {
		return memoryBarSize;
	}

	public static boolean isPlaceholderAPI() {
		return placeholderAPI;
	}

	public static boolean isPerWorldPlayerList() {
		return perWorldPlayerList;
	}

	public static boolean isFakePlayers() {
		return fakePlayers;
	}

	public static boolean isCountFakePlayersToOnlinePlayers() {
		return countFakePlayersToOnlinePlayers;
	}

	public static boolean isRemoveGrayColorFromTabInSpec() {
		return removeGrayColorFromTabInSpec;
	}

	public static boolean isIgnoreVanishedPlayers() {
		return ignoreVanishedPlayers;
	}

	public static boolean isCountVanishedStaff() {
		return countVanishedStaff;
	}

	public static boolean isHidePlayerFromTabAfk() {
		return hidePlayerFromTabAfk;
	}

	public static boolean isHidePlayersFromTab() {
		return hidePlayersFromTab;
	}

	public static boolean isAfkStatusEnabled() {
		return afkStatusEnabled;
	}

	public static boolean isAfkStatusShowInRightLeftSide() {
		return afkStatusShowInRightLeftSide;
	}

	public static boolean isAfkStatusShowPlayerGroup() {
		return afkStatusShowPlayerGroup;
	}

	public static boolean isAfkSortLast() {
		return afkSortLast;
	}

	public static String getAfkFormatYes() {
		return afkFormatYes;
	}

	public static String getAfkFormatNo() {
		return afkFormatNo;
	}

	public static String getTimeZone() {
		return timeZone;
	}

	public static boolean isUseSystemZone() {
		return useSystemZone;
	}

	public static DateTimeFormatter getTimeFormat() {
		return timeFormat;
	}

	public static DateTimeFormatter getDateFormat() {
		return dateFormat;
	}

	public static boolean isPingFormatEnabled() {
		return pingFormatEnabled;
	}

	public static boolean isTpsFormatEnabled() {
		return tpsFormatEnabled;
	}

	public static boolean isPrefixSuffixEnabled() {
		return prefixSuffixEnabled;
	}

	public static int getGroupsRefreshInterval() {
		return groupsRefreshInterval;
	}

	public static boolean isUseDisabledWorldsAsWhiteList() {
		return useDisabledWorldsAsWhiteList;
	}

	public static boolean isSyncPluginsGroups() {
		return syncPluginsGroups;
	}

	public static boolean isHideGroupInVanish() {
		return hideGroupInVanish;
	}

	public static boolean isPreferPrimaryVaultGroup() {
		return preferPrimaryVaultGroup;
	}

	public static Objects.ObjectTypes getObjectType() {
		return objectType;
	}

	public static int getObjectRefreshInterval() {
		return objectRefreshInterval;
	}

	public static String getCustomObjectSetting() {
		return customObjectSetting;
	}

	public static int getTpsDigits() {
		return tpsDigits;
	}

	public static List<String> getTpsColorFormats() {
		return tpsColorFormats;
	}

	public static List<String> getPingColorFormats() {
		return pingColorFormats;
	}

	public static List<String> getGroupsDisabledWorlds() {
		return groupsDisabledWorlds;
	}

	public static List<String> getHealthObjectRestricted() {
		return healthObjectRestricted;
	}

	public static List<String> getObjectsDisabledWorlds() {
		return objectsDisabledWorlds;
	}

	public static boolean isAssignGlobalGroup() {
		return assignGlobalGroup;
	}

	public static boolean isFollowNameTagVisibility() {
		return followNameTagVisibility;
	}

	public static double getTpsPerformanceObservationValue() {
		return tpsPerformanceObservationValue;
	}
}
