package hu.montlikadani.tablist.config;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import org.spongepowered.configurate.CommentedConfigurationNode;

import hu.montlikadani.tablist.tablist.objects.ObjectType;

public final class ConfigValues {

	private static String customObject, timeZone;
	private static DateTimeFormatter timeFormat, dateFormat;

	private static ObjectType tablistObjectsType;

	private static boolean useSystemZone, pingFormatEnabled, tablistEnabled, randomTablist, tablistGroups;

	private static int tablistUpdateTime, objectsRefreshInterval;

	private static List<String> pingColorFormats, tabDisabledWorlds, tabRestrictedPlayers;

	public static void loadValues(ConfigManager cm) {
		CommentedConfigurationNode node = cm.getNode("placeholder-format");
		CommentedConfigurationNode setting;

		cm.setComment(node.node("time", "time-format"),
				"Formats/examples: https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html\nUsed for %server-time% placeholder");
		try {
			timeFormat = DateTimeFormatter.ofPattern(cm.getString(node.node("time", "time-format", "time"), "mm:HH"));
		} catch (IllegalArgumentException e) {
		}

		cm.setComment(node.node("time", "date-format"), "Used for %date% placeholder");
		try {
			dateFormat = DateTimeFormatter.ofPattern(cm.getString(node.node("time", "date-format", "format"), "dd/MM/yyyy"));
		} catch (IllegalArgumentException e) {
		}

		cm.setComment(node.node("time", "time-zone"),
				"Time zones: https://www.mkyong.com/java/java-display-list-of-timezone-with-gmt/\n"
						+ "Or google it: \"what is my time zone\"");
		timeZone = cm.getString(node.node("time", "time-zone", "zone"), "GMT0");
		cm.setComment(setting = node.node("time", "time-zone", "use-system-zone"),
				"Use system default time zone instead of searching for that?");
		useSystemZone = setting.getBoolean(false);

		cm.setComment(node.node("ping"), "Ping color format for %player-ping% placeholder");
		pingFormatEnabled = node.node("ping", "enabled").getBoolean(true);

		cm.setComment(setting = node.node("ping", "formats"),
				"Operators usage: https://github.com/montlikadani/TabList/wiki/Ping-formatting-Sponge");
		pingColorFormats = cm.getAsList(setting, Arrays.asList("200 <= &a", "400 >= &6", "500 > &c"));

		node = cm.getNode("tablist");

		cm.setComment(node,
				"Tablist, header & footer with animation.\nUse %anim:animationName% placeholder to make an animation.");

		cm.setComment(setting = node.node("enabled"), "Does the tablist enabled?");
		tablistEnabled = setting.getBoolean(true);
		cm.setComment(setting = node.node("random"),
				"Define if the header & footer should be randomized.\nAnimation placeholders won't be affected.");
		randomTablist = setting.getBoolean(false);
		cm.setComment(setting = node.node("update-time"), "Tablist refresh rate in milliseconds.");
		tablistUpdateTime = setting.getInt(4);
		cm.setComment(setting = node.node("disabled-worlds"), "The listed worlds where the tablist will not show");
		tabDisabledWorlds = cm.getAsList(setting);
		cm.setComment(setting = node.node("restricted-players"), "Restricted players, who's not see the tab.");
		tabRestrictedPlayers = cm.getAsList(setting);

		node = cm.getNode("tablist-groups");

		cm.setComment(node, "Tablist groups that shows up on player list (prefix/suffix).");
		tablistGroups = node.node("enabled").getBoolean(true);

		node = cm.getNode("tablist-objects");

		cm.setComment(setting = node.node("type"),
				"Tablist object type to display in tablist after player name.\n" + "Available types: none, ping, custom, hearth");
		tablistObjectsType = ObjectType.getByName(setting.getString("none"));
		cm.setComment(setting = node.node("settings", "custom-value"),
				"Custom objective setting, use any placeholder that returns an integer.");
		customObject = setting.getString("%level%");
		cm.setComment(setting = node.node("refresh-interval"), "The refresh interval when the objects are refreshing.");
		objectsRefreshInterval = setting.getInt(3);

		cm.save();
	}

	public static DateTimeFormatter getTimeFormat() {
		return timeFormat;
	}

	public static DateTimeFormatter getDateFormat() {
		return dateFormat;
	}

	public static String getTimeZone() {
		return timeZone;
	}

	public static ObjectType getTablistObjectsType() {
		return tablistObjectsType;
	}

	public static int getObjectsRefreshInterval() {
		return objectsRefreshInterval;
	}

	public static String getCustomObject() {
		return customObject;
	}

	public static boolean isTablistGroups() {
		return tablistGroups;
	}

	public static boolean isTablistEnabled() {
		return tablistEnabled;
	}

	public static boolean isRandomTablist() {
		return randomTablist;
	}

	public static int getTablistUpdateTime() {
		return tablistUpdateTime;
	}

	public static boolean isUseSystemZone() {
		return useSystemZone;
	}

	public static boolean isPingFormatEnabled() {
		return pingFormatEnabled;
	}

	public static List<String> getPingColorFormats() {
		return pingColorFormats;
	}

	public static List<String> getTabDisabledWorlds() {
		return tabDisabledWorlds;
	}

	public static List<String> getTabRestrictedPlayers() {
		return tabRestrictedPlayers;
	}
}
