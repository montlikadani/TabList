package hu.montlikadani.tablist.bukkit;

import org.bukkit.configuration.file.FileConfiguration;

public class ConfigValues {

	private static boolean placeholderAPI, ragemodeHook, perWorldPlayerList, fakePlayers, removeGrayColorFromTabInSpec,
			ignoreVanishedPlayers, countVanishedStaff, hidePlayerFromTabAfk, hidePlayersFromTab, afkStatusEnabled,
			afkStatusShowInRightLeftSide, afkStatusShowPlayerGroup, afkSortLast, useSystemZone, pingFormatEnabled,
			tpsFormatEnabled, prefixSuffixEnabled/*, groupAnimationEnabled*/, useDisabledWorldsAsWhiteList,
			syncPluginsGroups/*, useOwnScoreboard*/, hideGroupInVanish, hideGroupWhenAfk, usePluginNickName, useTabName,
			preferPrimaryVaultGroup, tablistObjectiveEnabled, tabNameEnabled, tabNameUsePluginNickName,
			clearTabNameOnQuit, tabNameColorCodeEnabled, defaultColorEnabled;

	private static String afkFormatYes, afkFormatNo, timeZone, timeFormat, dateFormat, goodPingColor, mediumPingColor,
			badPingColor, goodTpsColor, mediumTpsColor, badTpsColor, objectType, customObjectSetting,
			defaultTabNameColor;

	private static int goodPingAmount, mediumPingAmount, tpsSize, groupsRefreshInterval, objectRefreshInterval,
			tabNameMaxLength;

	private static double goodTpsAmount;
	private static double mediumTpsAmount;

	public static void loadValues() {
		FileConfiguration c = TabList.getInstance().getC();

		placeholderAPI = c.contains("placeholderapi") ? c.getBoolean("placeholderapi")
				: c.getBoolean("hook.placeholderapi", false);
		ragemodeHook = c.getBoolean("hook.RageMode", false);
		perWorldPlayerList = c.getBoolean("per-world-player-list", false);
		fakePlayers = c.getBoolean("enable-fake-players", false);
		removeGrayColorFromTabInSpec = c.getBoolean("remove-gray-color-from-tab-in-spectator", false);
		ignoreVanishedPlayers = c.getBoolean("ignore-vanished-players-in-online-players", false);
		countVanishedStaff = c.getBoolean("count-vanished-staffs", true);
		hidePlayerFromTabAfk = c.getBoolean("hide-player-from-tab-when-afk", false);
		hidePlayersFromTab = c.getBoolean("hide-players-from-tablist", false);
		afkStatusEnabled = c.getBoolean("placeholder-format.afk-status.enable", false);
		afkStatusShowInRightLeftSide = c.getBoolean("placeholder-format.afk-status.show-in-right-or-left-side", true);
		afkStatusShowPlayerGroup = c.getBoolean("placeholder-format.afk-status.show-player-group", true);
		afkSortLast = c.getBoolean("placeholder-format.afk-status.sort-last", false);
		useSystemZone = c.getBoolean("placeholder-format.time.use-system-zone", false);
		pingFormatEnabled = c.getBoolean("placeholder-format.ping.enable", true);
		tpsFormatEnabled = c.getBoolean("placeholder-format.tps.enable", true);
		prefixSuffixEnabled = c.getBoolean("change-prefix-suffix-in-tablist.enable", false);
		//groupAnimationEnabled = c.getBoolean("change-prefix-suffix-in-tablist.enable-animation", false);
		useDisabledWorldsAsWhiteList = c.getBoolean("change-prefix-suffix-in-tablist.disabled-worlds.use-as-whitelist",
				false);
		syncPluginsGroups = c.getBoolean("change-prefix-suffix-in-tablist.sync-plugins-groups-with-tablist", true);
		//useOwnScoreboard = c.getBoolean("change-prefix-suffix-in-tablist.use-own-scoreboard", false);
		hideGroupInVanish = c.getBoolean("change-prefix-suffix-in-tablist.hide-group-when-player-vanished", false);
		hideGroupWhenAfk = c.getBoolean("change-prefix-suffix-in-tablist.hide-group-when-player-afk", false);
		if (c.contains("change-prefix-suffix-in-tablist.use-essentials-nickname")) {
			usePluginNickName = c.getBoolean("change-prefix-suffix-in-tablist.use-essentials-nickname", false);
		} else {
			usePluginNickName = c.getBoolean("change-prefix-suffix-in-tablist.use-plugin-nickname", false);
		}
		useTabName = c.getBoolean("change-prefix-suffix-in-tablist.use-tab-name", false);
		preferPrimaryVaultGroup = c.getBoolean("change-prefix-suffix-in-tablist.prefer-primary-vault-group", true);
		tablistObjectiveEnabled = c.getBoolean("tablist-object-type.enable", false);
		tabNameEnabled = c.getBoolean("tabname.enable", false);
		tabNameUsePluginNickName = c
				.getBoolean(c.contains("tabname.use-essentials-nickname") ? "tabname.use-essentials-nickname"
						: "tabname.use-plugin-nickname", false);
		clearTabNameOnQuit = c.getBoolean("tabname.clear-player-tabname-on-quit", false);
		tabNameColorCodeEnabled = c.getBoolean("tabname.enable-color-code", false);
		defaultColorEnabled = c.getBoolean("tabname.default-color.enable", false);

		afkFormatYes = c.getString("placeholder-format.afk-status.format-yes", "&7 [AFK]&r ");
		afkFormatNo = c.getString("placeholder-format.afk-status.format-no", "");
		timeZone = c.getString("placeholder-format.time.time-zone", "GMT0");
		timeFormat = c.getString("placeholder-format.time.time-format.format", "mm:HH");
		dateFormat = c.getString("placeholder-format.time.date-format.format", "dd/MM/yyyy");
		goodPingColor = c.getString("placeholder-format.ping.good-ping.color", "&a");
		mediumPingColor = c.getString("placeholder-format.ping.medium-ping.color", "&6");
		badPingColor = c.getString("placeholder-format.ping.bad-ping", "&c");
		goodTpsColor = c.getString("placeholder-format.tps.good-tps.color", "&a");
		mediumTpsColor = c.getString("placeholder-format.tps.medium-tps.color", "&6");
		badTpsColor = c.getString("placeholder-format.tps.bad-ping", "&c");
		objectType = c.getString("tablist-object-type.type", "ping");
		customObjectSetting = c.getString("tablist-object-type.object-settings.custom.value", "%level%");
		defaultTabNameColor = c.getString("tabname.default-color.color", "&6");

		goodPingAmount = c.getInt("placeholder-format.ping.good-ping.amount", 200);
		mediumPingAmount = c.getInt("placeholder-format.ping.medium-ping.amount", 500);
		tpsSize = c.getInt("placeholder-format.tps.size", 2);
		groupsRefreshInterval = c.getInt("change-prefix-suffix-in-tablist.refresh-interval", 30);
		objectRefreshInterval = c.getInt("tablist-object-type.refresh-interval", 3);
		tabNameMaxLength = c.getInt("tabname.max-name-length", 200);

		goodTpsAmount = c.getDouble("placeholder-format.tps.good-tps.amount", 18.0);
		mediumTpsAmount = c.getDouble("placeholder-format.tps.medium-tps.amount", 16.0);
	}

	public static boolean isPlaceholderAPI() {
		return placeholderAPI;
	}

	public static boolean isRagemodeHook() {
		return ragemodeHook;
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

	public static String getGoodPingColor() {
		return goodPingColor;
	}

	public static int getGoodPingAmount() {
		return goodPingAmount;
	}

	public static String getMediumPingColor() {
		return mediumPingColor;
	}

	public static int getMediumPingAmount() {
		return mediumPingAmount;
	}

	public static String getBadPingColor() {
		return badPingColor;
	}

	public static boolean isTpsFormatEnabled() {
		return tpsFormatEnabled;
	}

	public static double getGoodTpsAmount() {
		return goodTpsAmount;
	}

	public static double getMediumTpsAmount() {
		return mediumTpsAmount;
	}

	public static String getGoodTpsColor() {
		return goodTpsColor;
	}

	public static String getMediumTpsColor() {
		return mediumTpsColor;
	}

	public static String getBadTpsColor() {
		return badTpsColor;
	}

	public static boolean isPrefixSuffixEnabled() {
		return prefixSuffixEnabled;
	}

	public static int getGroupsRefreshInterval() {
		return groupsRefreshInterval;
	}

	@Deprecated
	public static boolean isGroupAnimationEnabled() {
		return true;
	}

	public static boolean isUseDisabledWorldsAsWhiteList() {
		return useDisabledWorldsAsWhiteList;
	}

	public static boolean isSyncPluginsGroups() {
		return syncPluginsGroups;
	}

	@Deprecated
	public static boolean isUseOwnScoreboard() {
		return false;
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
}
