package hu.montlikadani.tablist.bukkit.config;

import java.util.Arrays;
import java.util.List;

import hu.montlikadani.tablist.bukkit.TabList;

public class ConfigValues {

	private static boolean placeholderAPI, perWorldPlayerList, fakePlayers, removeGrayColorFromTabInSpec,
			ignoreVanishedPlayers, countVanishedStaff, hidePlayerFromTabAfk, hidePlayersFromTab, afkStatusEnabled,
			afkStatusShowInRightLeftSide, afkStatusShowPlayerGroup, afkSortLast, useSystemZone, pingFormatEnabled,
			tpsFormatEnabled, prefixSuffixEnabled, useDisabledWorldsAsWhiteList, syncPluginsGroups, hideGroupInVanish,
			hideGroupWhenAfk, usePluginNickName, useTabName, preferPrimaryVaultGroup, tablistObjectiveEnabled,
			tabNameEnabled, tabNameUsePluginNickName, clearTabNameOnQuit, tabNameColorCodeEnabled, defaultColorEnabled,
			tpsCanBeHigher;

	private static String afkFormatYes, afkFormatNo, timeZone, timeFormat, dateFormat, objectType, customObjectSetting,
			defaultTabNameColor, memoryBarChar, memoryBarUsedColor, memoryBarFreeColor, memoryBarAllocationColor,
			memoryBarReleasedColor;

	private static List<String> tpsColorFormats, pingColorFormats, restrictedTabNames;

	private static int tpsSize, groupsRefreshInterval, objectRefreshInterval, tabNameMaxLength, memoryBarSize;

	public static void loadValues() {
		CommentedConfig c = TabList.getInstance().getConf().getConfig();
		c.copyDefaults(true);

		c.addComment("hook.placeholderapi", "Hook to PlaceholderAPI to use custom placeholders.");
		c.addComment("hook.RageMode", "Hook to my RageMode plugin. (https://www.spigotmc.org/resources/69169/)",
				"If true, then tablist, groups and tablist objects will be disabled while", "running a game.");
		c.addComment("per-world-player-list", "Different player list in different world.");
		c.addComment("enable-fake-players", "Fake players that can be added to the player list.");
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
		c.addComment("hide-player-from-tab-when-afk", "Hide player from player list when he is AFK?",
				"Requires Essentials or CMI plugin!");
		c.addComment("hide-players-from-tablist", "Hide all players from the player list?",
				"This removes all players from the player list, but the player that has the",
				"group set is retained as it is not changed during removal, so your group",
				"will be restored if this option is disabled.",
				"The \"per-world-player-list\" will not work if this is enabled.",
				"Requires ProtocolLib to fix view distance issue! (https://github.com/montlikadani/TabList/issues/147)");
		c.addComment("placeholder-format", "Placeholders formatting");
		c.addComment("placeholder-format.afk-status",
				"When the player changes the AFK status, change his tablist name format?");
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
		c.addComment("placeholder-format.time.use-system-zone",
				"Use system default time zone instead of searching for that?");
		c.addComment("placeholder-format.time.time-format",
				"Formats/examples: https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html",
				"Format of %server-time% placeholder.");
		c.addComment("placeholder-format.time.date-format", "Format of %date% placeholder.");
		c.addComment("placeholder-format.ping", "Ping color format for %ping% placeholder.");
		c.addComment("placeholder-format.ping.formats", "Operators usage:",
				"> - highest value, \"17 > &e\" this will be yellow color",
				">= - highest & equal value, \"5 >= &6\" gold color",
				"<= - less than & equal value, \"16 <= &c\" red color", "< - less value, \"8 < &4\" dark red color",
				"== - equal value, \"20 == &a\" green color if 20 is equal to current ping amount");
		c.addComment("placeholder-format.tps", "TPS color format for %tps% placeholder.");
		c.addComment("placeholder-format.tps.size",
				"How many numbers do you want to display after \".\" in %tps% placeholder?",
				"The number should be higher than 0.", "Example: 3 = 20.14");
		c.addComment("placeholder-format.tps.value-can-be-higher",
				"Adds ability to the tps value can be highest than default 20.0", "This client-dependent");
		c.addComment("placeholder-format.tps.formats", "Operators usage:",
				"> - highest value, \"17 > &e\" this will be yellow color",
				">= - highest & equal value, \"5 >= &6\" gold color",
				"<= - less than & equal value, \"16 <= &c\" red color", "< - less value, \"8 < &4\" dark red color",
				"== - equal value, \"20 == &a\" green color if 20 is equal to current tps amount");
		c.addComment("placeholder-format.memory-bar", "Memory bar settings for %memory_bar% variable");
		c.addComment("placeholder-format.memory-bar.colors.allocation", "When the server memory less than 80");
		c.addComment("placeholder-format.memory-bar.colors.released",
				"When the server memory is on critical level (less than 40) and some resource need memory to run.");
		c.addComment("change-prefix-suffix-in-tablist", "Enable changing of prefix & suffix in player list?",
				"This option generates the groups.yml file.");
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
		c.addComment("change-prefix-suffix-in-tablist.hide-group-when-player-afk",
				"Hide player's group in player list when the player is AFK?", "Requires Essentials or CMI plugin!");
		c.addComment("change-prefix-suffix-in-tablist.use-plugin-nickname",
				"Use another plugin nickname instead of player's original name?",
				"true - Using this format: \"prefix playerNickName suffix\"",
				"false - Using this format: \"prefix playerOriginalName suffix\"",
				"Requires Essentials or CMI plugin!");
		c.addComment("change-prefix-suffix-in-tablist.use-tab-name", "Use tab name instead of player's original name?");
		c.addComment("change-prefix-suffix-in-tablist.prefer-primary-vault-group",
				"Prefer player's primary Vault group when assigning tablist group from groups.yml?",
				"true - player will be assigned their primary vault group where possible, after that",
				"groups from groups.yml will be applied in descending order.",
				"false - groups from groups.yml will be applied in descending order.");
		c.addComment("tablist-object-type", "Tablist objective types",
				"Shows your current health (with life indicator), your current ping or any NUMBER placeholder",
				"after the player's name (before the ping indicator).");
		c.addComment("tablist-object-type.type", "Types:", "ping - player's ping", "health - player's health",
				"custom - custom placeholder");
		c.addComment("tablist-object-type.refresh-interval", "Interval for objects refreshing. In seconds.",
				"Note: The health is not updating auto due to display issues.");
		c.addComment("tablist-object-type.disabled-worlds", "In these worlds the objects will not be displayed");
		c.addComment("tablist-object-type.object-settings", "Objective settings");
		c.addComment("tablist-object-type.object-settings.health",
				"The player's health - displayed after the player's name.");
		c.addComment("tablist-object-type.object-settings.health.restricted-players",
				"For these players the health will not be displayed");
		c.addComment("tablist-object-type.object-settings.custom",
				"Custom placeholder - accepts only number-ending placeholders, like %level%.",
				"If it is double-number ending, it will remove the dot \".\" from the number.");
		c.addComment("tabname", "Tabname settings for /tabname command.",
				"Changing player's tablist name with unlimited characters.");
		c.addComment("tabname.use-plugin-nickname", "Use another plugin nickname instead of tab name?",
				"Requires Essentials or CMI plugin!");
		c.addComment("tabname.max-name-length", "How many characters should be allowed?");
		c.addComment("tabname.clear-player-tabname-on-quit", "When the player lefts the server, delete his tab name?");
		c.addComment("tabname.enable-color-code", "Enable color codes in tab names?",
				"false - disables all placeholders, symbols and color codes.");
		c.addComment("tabname.default-color",
				"Enable default color? (If the player has not written a color code, this color will be given by default.)");
		c.addComment("tabname.disabled-worlds", "In these worlds you can't change the tab name");
		c.addComment("tabname.restricted-names", "Which names can not be used? Supports regexes",
				"For example if you have \"[^name]\" every character are ignored except the name.");
		c.addComment("check-update", "Check for updates?");
		c.addComment("download-updates", "Download new releases to \"releases\" folder?",
				"This only works if the \"check-update\" is true.");
		c.addComment("logconsole", "Log plugin messages to console?");

		placeholderAPI = c.get("hook.placeholderapi", false);
		perWorldPlayerList = c.get("per-world-player-list", false);
		fakePlayers = c.get("enable-fake-players", false);
		removeGrayColorFromTabInSpec = c.get("remove-gray-color-from-tab-in-spectator", false);
		ignoreVanishedPlayers = c.get("ignore-vanished-players-in-online-players", false);
		countVanishedStaff = c.get("count-vanished-staffs", true);
		hidePlayerFromTabAfk = c.get("hide-player-from-tab-when-afk", false);
		hidePlayersFromTab = c.get("hide-players-from-tablist", false);
		afkStatusEnabled = c.get("placeholder-format.afk-status.enable", false);
		afkStatusShowInRightLeftSide = c.get("placeholder-format.afk-status.show-in-right-or-left-side", true);
		afkStatusShowPlayerGroup = c.get("placeholder-format.afk-status.show-player-group", true);
		afkSortLast = c.get("placeholder-format.afk-status.sort-last", false);
		useSystemZone = c.get("placeholder-format.time.use-system-zone", false);
		pingFormatEnabled = c.get("placeholder-format.ping.enable", true);
		tpsCanBeHigher = c.get("placeholder-format.tps.value-can-be-higher", false);
		tpsFormatEnabled = c.get("placeholder-format.tps.enable", true);
		prefixSuffixEnabled = c.get("change-prefix-suffix-in-tablist.enable", false);
		useDisabledWorldsAsWhiteList = c.get("change-prefix-suffix-in-tablist.disabled-worlds.use-as-whitelist", false);
		syncPluginsGroups = c.get("change-prefix-suffix-in-tablist.sync-plugins-groups-with-tablist", true);
		hideGroupInVanish = c.get("change-prefix-suffix-in-tablist.hide-group-when-player-vanished", false);
		hideGroupWhenAfk = c.get("change-prefix-suffix-in-tablist.hide-group-when-player-afk", false);
		usePluginNickName = c.get("change-prefix-suffix-in-tablist.use-plugin-nickname", false);
		useTabName = c.get("change-prefix-suffix-in-tablist.use-tab-name", false);
		preferPrimaryVaultGroup = c.get("change-prefix-suffix-in-tablist.prefer-primary-vault-group", true);
		tablistObjectiveEnabled = c.get("tablist-object-type.enable", false);
		tabNameEnabled = c.get("tabname.enable", false);
		tabNameUsePluginNickName = c.get("tabname.use-plugin-nickname", false);
		clearTabNameOnQuit = c.get("tabname.clear-player-tabname-on-quit", false);
		tabNameColorCodeEnabled = c.get("tabname.enable-color-code", false);
		defaultColorEnabled = c.get("tabname.default-color.enable", false);

		c.get("check-update", true);
		c.get("download-updates", false);
		c.get("logconsole", true);

		afkFormatYes = c.get("placeholder-format.afk-status.format-yes", "&7 [AFK]&r ");
		afkFormatNo = c.get("placeholder-format.afk-status.format-no", "");
		timeZone = c.get("placeholder-format.time.time-zone", "GMT0");
		timeFormat = c.get("placeholder-format.time.time-format.format", "mm:HH");
		dateFormat = c.get("placeholder-format.time.date-format.format", "dd/MM/yyyy");

		memoryBarChar = c.get("placeholder-format.memory-bar.char", "|");
		memoryBarUsedColor = c.get("placeholder-format.memory-bar.colors.used", "&c");
		memoryBarFreeColor = c.get("placeholder-format.memory-bar.colors.free", "&a");
		memoryBarAllocationColor = c.get("placeholder-format.memory-bar.colors.allocation", "&e");
		memoryBarReleasedColor = c.get("placeholder-format.memory-bar.colors.released", "&6");
		objectType = c.get("tablist-object-type.type", "ping");
		customObjectSetting = c.get("tablist-object-type.object-settings.custom.value", "%level%");
		defaultTabNameColor = c.get("tabname.default-color.color", "&6");

		tpsColorFormats = c.get("placeholder-format.tps.formats",
				Arrays.asList("18.0 > &a", "16.0 == &6", "16.0 < &c"));
		pingColorFormats = c.get("placeholder-format.ping.formats",
				Arrays.asList("200 <= &a", "400 >= &6", "500 > &c"));

		c.get("change-prefix-suffix-in-tablist.disabled-worlds.list", Arrays.asList("myWorldWithUpper"));
		c.get("tablist-object-type.object-settings.health.restricted-players",
				Arrays.asList("exampleplayer", "players"));
		c.get("tabname.disabled-worlds", Arrays.asList("myWorldWithUpper"));
		restrictedTabNames = c.get("tabname.restricted-names", Arrays.asList("[^tabname]", "namesHere"));

		tpsSize = c.get("placeholder-format.tps.size", 2);
		memoryBarSize = c.get("placeholder-format.memory-bar.size", 80);
		groupsRefreshInterval = c.get("change-prefix-suffix-in-tablist.refresh-interval", 30);
		objectRefreshInterval = c.get("tablist-object-type.refresh-interval", 3);
		tabNameMaxLength = c.get("tabname.max-name-length", 200);

		c.cleanUp();
		c.save();
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

	public static boolean isAfkSortLast() { return afkSortLast; }

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

	public static String getTimeFormat() {
		return timeFormat;
	}

	public static String getDateFormat() {
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

	public static boolean isHideGroupWhenAfk() {
		return hideGroupWhenAfk;
	}

	public static boolean isUsePluginNickName() {
		return usePluginNickName;
	}

	public static boolean isUseTabName() {
		return useTabName;
	}

	public static boolean isPreferPrimaryVaultGroup() { return preferPrimaryVaultGroup;}

	public static boolean isTablistObjectiveEnabled() {
		return tablistObjectiveEnabled;
	}

	public static String getObjectType() {
		return objectType;
	}

	public static int getObjectRefreshInterval() {
		return objectRefreshInterval;
	}

	public static String getCustomObjectSetting() {
		return customObjectSetting;
	}

	public static boolean isTabNameEnabled() {
		return tabNameEnabled;
	}

	public static boolean isTabNameUsePluginNickName() {
		return tabNameUsePluginNickName;
	}

	public static int getTabNameMaxLength() {
		return tabNameMaxLength;
	}

	public static boolean isClearTabNameOnQuit() {
		return clearTabNameOnQuit;
	}

	public static boolean isTabNameColorCodeEnabled() {
		return tabNameColorCodeEnabled;
	}

	public static boolean isDefaultColorEnabled() {
		return defaultColorEnabled;
	}

	public static String getDefaultTabNameColor() {
		return defaultTabNameColor;
	}

	public static int getTpsSize() {
		return tpsSize;
	}

	public static List<String> getTpsColorFormats() {
		return tpsColorFormats;
	}

	public static List<String> getPingColorFormats() {
		return pingColorFormats;
	}

	public static boolean isTpsCanBeHigher() {
		return tpsCanBeHigher;
	}

	public static List<String> getRestrictedTabNames() {
		return restrictedTabNames;
	}
}
