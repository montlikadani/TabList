package hu.montlikadani.tablist.config;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hu.montlikadani.tablist.Misc;
import hu.montlikadani.tablist.logicalOperators.LogicalNode;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.config.Configuration;

public final class ConfigConstants {

	private static String timeZone;

	private static boolean useSystemZone, isTabEnabled, isGroupsEnabled;
	private static int tabRefreshInterval, groupsRefreshInterval;

	private static List<String> defaultHeader, defaultFooter, disabledServers, restrictedPlayers;

	private static Configuration perServerSection, perPlayerSection;
	private static DateTimeFormatter timeFormat, dateFormat;

	public static final Map<String, String> CUSTOM_VARIABLES = new HashMap<>();
	public static final Map<String, TabSetting> TAB_SETTINGS = new HashMap<>();

	public static final java.util.Set<GroupSettings> GROUP_SETTINGS = new java.util.HashSet<>();

	private static final List<LogicalNode> LOGICAL_NODES = new ArrayList<>();

	public static void load(Configuration conf) {
		CUSTOM_VARIABLES.clear();
		TAB_SETTINGS.clear();
		GROUP_SETTINGS.clear();
		LOGICAL_NODES.clear();

		String tf = conf.getString("placeholder-format.time.time-format", "mm:HH");

		if (!tf.isEmpty()) {
			try {
				timeFormat = DateTimeFormatter.ofPattern(tf);
			} catch (IllegalArgumentException e) {
			}
		}

		tf = conf.getString("placeholder-format.time.date-format", "dd/MM/yyyy");

		if (!tf.isEmpty()) {
			try {
				dateFormat = DateTimeFormatter.ofPattern(tf);
			} catch (IllegalArgumentException e) {
			}
		}

		timeZone = conf.getString("placeholder-format.time.time-zone", "GMT0");
		useSystemZone = conf.getBoolean("placeholder-format.time.use-system-zone", false);
		isTabEnabled = conf.getBoolean("tablist.enable", true);
		isGroupsEnabled = conf.getBoolean("tablist-groups.enabled", false);
		tabRefreshInterval = conf.getInt("tablist.refresh-interval", 180);
		groupsRefreshInterval = conf.getInt("tablist-groups.refresh-time", 160);

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
			for (String key : perServerSection.getKeys()) {
				TAB_SETTINGS.put(key, new TabSetting(perServerSection, key));
			}
		}

		if ((perPlayerSection = conf.getSection("tablist.per-player")) != null) {
			for (String key : perPlayerSection.getKeys()) {
				TAB_SETTINGS.put(key, new TabSetting(perPlayerSection, key));
			}
		}

		if ((section = conf.getSection("groups")) != null) {
			for (String key : section.getKeys()) {
				GROUP_SETTINGS.add(new GroupSettings(section, key));
			}
		}

		for (MessageKeys key : MessageKeys.values()) {
			if (key.type == String.class) {
				String value = conf.getString(key.path, "");

				key.value = value.isEmpty() ? Misc.EMPTY_COMPONENT : Misc.getComponentOfText(Misc.colorMsg(value));
			} else if (key.type == List.class) {
				List<String> list = conf.getStringList(key.path);
				List<BaseComponent> formatted = new ArrayList<>(list.size());

				for (String one : list) {
					formatted.add(Misc.getComponentOfText(Misc.colorMsg(one)));
				}

				key.value = formatted;
			}
		}

		for (String f : conf.getStringList("placeholder-format.ping-formatting")) {
			if (f.isEmpty()) {
				continue;
			}

			LogicalNode node = LogicalNode.newNode(LogicalNode.NodeType.PING).parseInput(f);

			if (node.getCondition() != null) {
				LOGICAL_NODES.add(node);
			}
		}

		LogicalNode.reverseOrderOfArray(LOGICAL_NODES);
	}

	public static String formatPing(int ping) {
		if (LOGICAL_NODES.isEmpty()) {
			return Integer.toString(ping);
		}

		return LogicalNode.parseCondition(ping, LogicalNode.NodeType.PING, LOGICAL_NODES).toString();
	}

	public static final class GroupSettings {

		public final String permission;
		public final String[] texts;

		public GroupSettings(Configuration section, String key) {
			permission = section.getString(key + ".permission", "");

			List<?> list = section.getList(key + ".name", java.util.Collections.EMPTY_LIST);

			texts = list.size() != 0 ? list.toArray(new String[0])
					: new String[] { section.getString(key + ".name", "") };

			for (int a = 0; a < texts.length; a++) {
				texts[a] = hu.montlikadani.tablist.Global.setSymbols(texts[a]);
			}
		}
	}

	public static final class TabSetting {

		public final String[] names;
		public final List<String> header, footer;

		public TabSetting(Configuration section, String key) {
			names = key.split(", ");
			header = section.getStringList(key + ".header");
			footer = section.getStringList(key + ".footer");
		}
	}

	public static BaseComponent getMessage(MessageKeys key) {
		return key.<BaseComponent>getValue();
	}

	public static List<BaseComponent> getMessageList(MessageKeys key) {
		return key.<List<BaseComponent>>getValue();
	}

	public static String getTimeZone() {
		return timeZone;
	}

	public static DateTimeFormatter getTimeFormat() {
		return timeFormat;
	}

	public static DateTimeFormatter getDateFormat() {
		return dateFormat;
	}

	public static boolean isUseSystemZone() {
		return useSystemZone;
	}

	public static Configuration getPerServerSection() {
		return perServerSection;
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

	public enum MessageKeys {
		RELOAD_CONFIG(false), NO_PERMISSION(false),

		TOGGLE_NO_CONSOLE(true), TOGGLE_NO_PLAYER(true), TOGGLE_ENABLED(true), TOGGLE_DISABLED(true),
		TOGGLE_NO_PLAYERS_AVAILABLE(true),

		CHAT_MESSAGES(false, List.class);

		public final String path;

		private Class<?> type;
		private Object value;

		MessageKeys(boolean keyed) {
			this(keyed, String.class);
		}

		MessageKeys(boolean keyed, Class<?> type) {
			String name;

			if (keyed) {
				String mainPath = name().substring(0, name().indexOf('_') - 1);

				name = mainPath + '.' + name().substring(mainPath.length() + 1);
			} else {
				name = name();
			}

			path = "messages." + name.replace('_', '-').toLowerCase(java.util.Locale.ENGLISH);
			this.type = type;
		}

		@SuppressWarnings("unchecked")
		public <T> T getValue() {
			return (T) value;
		}
	}
}
