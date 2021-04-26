package hu.montlikadani.tablist.bungee.config;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.md_5.bungee.config.Configuration;

public final class ConfigConstants {

	private static String timeZone, timeFormat, dateFormat;

	private static boolean useSystemZone, isTabEnabled, isGroupsEnabled;
	private static int tabRefreshInterval, groupsRefreshInterval;

	private static Collection<String> perServerColl, perPlayerColl, groupsKeys;
	private static List<String> defaultHeader, defaultFooter, disabledServers, restrictedPlayers;

	private static Configuration perServerSection, perPlayerSection;

	public static final Map<String, String> CUSTOM_VARIABLES = new HashMap<>();
	public static final Map<String, TabSetting> TAB_SETTINGS = new HashMap<>();
	public static final Map<String, GroupSettings> GROUP_SETTINGS = new HashMap<>();

	public static void load(Configuration conf) {
		CUSTOM_VARIABLES.clear();

		timeZone = conf.getString("placeholder-format.time.time-zone", "GMT0");
		timeFormat = conf.getString("placeholder-format.time.time-format", "mm:HH");
		dateFormat = conf.getString("placeholder-format.time.date-format", "dd/MM/yyyy");
		useSystemZone = conf.getBoolean("placeholder-format.time.use-system-zone");
		isTabEnabled = conf.getBoolean("tablist.enable", true);
		isGroupsEnabled = conf.getBoolean("tablist-groups.enabled");
		tabRefreshInterval = conf.getInt("tablist.refresh-interval", 180);
		groupsRefreshInterval = conf.getInt("tablist-groups.refresh-time");

		defaultHeader = conf.getStringList("tablist.header");
		defaultFooter = conf.getStringList("tablist.footer");
		disabledServers = conf.getStringList("tablist.disabled-servers");
		restrictedPlayers = conf.getStringList("tablist.restricted-players");

		Configuration section = conf.getSection("custom-variables");
		if (section != null) {
			for (String name : section.getKeys()) {
				CUSTOM_VARIABLES.put(name, section.getString(name, ""));
			}
		}

		if ((perServerSection = conf.getSection("tablist.per-server")) != null) {
			perServerColl = perServerSection.getKeys();

			for (String key : perServerColl) {
				TAB_SETTINGS.put(key, new TabSetting(perServerSection, key));
			}
		}

		if ((perPlayerSection = conf.getSection("tablist.per-player")) != null) {
			perPlayerColl = perPlayerSection.getKeys();

			for (String key : perPlayerColl) {
				TAB_SETTINGS.put(key, new TabSetting(perPlayerSection, key));
			}
		}

		if ((section = conf.getSection("groups")) != null) {
			groupsKeys = section.getKeys();

			for (String key : groupsKeys) {
				GROUP_SETTINGS.put(key, new GroupSettings(section, key));
			}
		}
	}

	public static final class GroupSettings {

		private String permission;
		private String[] nameArray = new String[0];

		public GroupSettings(Configuration section, String key) {
			permission = section.getString(key + ".permission", "");
			nameArray = (!section.getList(key + ".name").isEmpty() ? section.getStringList(key + ".name")
					: java.util.Arrays.asList(section.getString(key + ".name"))).toArray(new String[0]);
		}

		public String getPermission() {
			return permission;
		}

		public String[] getTextArray() {
			return nameArray;
		}
	}

	public static final class TabSetting {

		private String[] names = new String[0];

		private List<String> header, footer;

		public TabSetting(Configuration section, String key) {
			names = key.split(", ");
			header = section.getStringList(key + ".header");
			footer = section.getStringList(key + ".footer");
		}

		public String[] getNames() {
			return names;
		}

		public List<String> getHeader() {
			return header;
		}

		public List<String> getFooter() {
			return footer;
		}
	}

	public static String getTimeZone() {
		return timeZone;
	}

	public static String getTimeFormat() {
		return timeFormat;
	}

	public static String getDateFormat() {
		return dateFormat;
	}

	public static boolean isUseSystemZone() {
		return useSystemZone;
	}

	public static Collection<String> getPerServerColl() {
		return perServerColl;
	}

	public static Configuration getPerServerSection() {
		return perServerSection;
	}

	public static Collection<String> getPerPlayerColl() {
		return perPlayerColl;
	}

	public static Configuration getPerPlayerSection() {
		return perPlayerSection;
	}

	public static List<String> getDefaultHeader() {
		return defaultHeader;
	}

	public static List<String> getDefaultFooter() {
		return defaultFooter;
	}

	public static List<String> getDisabledServers() {
		return disabledServers;
	}

	public static List<String> getRestrictedPlayers() {
		return restrictedPlayers;
	}

	public static boolean isTabEnabled() {
		return isTabEnabled;
	}

	public static int getTabRefreshInterval() {
		return tabRefreshInterval;
	}

	public static boolean isGroupsEnabled() {
		return isGroupsEnabled;
	}

	public static int getGroupsRefreshInterval() {
		return groupsRefreshInterval;
	}

	public static Collection<String> getGroupsKeys() {
		return groupsKeys;
	}
}
