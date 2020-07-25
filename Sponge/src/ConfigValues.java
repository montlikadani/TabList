package hu.montlikadani.tablist.sponge;

public class ConfigValues {

	private static String tablistObjectsType, customObject, timeFormat, dateFormat, timeZone, goodPingColor,
			mediumPingColor, badPingColor;

	private static boolean useSystemZone, pingFormatEnabled, tablistEnabled, randomTablist, tablistGroups,
			useOwnScoreboard;

	private static int tablistUpdateTime, objectsRefreshInterval, goodPingAmount, mediumPingAmount;

	public static void loadValues() {
		ConfigManager c = TabList.get().getC().getConfig();

		c.setComment("Placeholder formatting", "placeholder-format");
		c.setComment("Formats/examples: https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html\n"
				+ "Used for %server-time% placeholder", "placeholder-format", "time", "time-format");
		timeFormat = c.getString("mm:HH", "placeholder-format", "time", "time-format", "time");
		c.setComment("Used for %date% placeholder", "placeholder-format", "time", "date-format");
		dateFormat = c.getString("dd/MM/yyyy", "placeholder-format", "time", "date-format", "format");
		c.setComment("Time zones: https://www.mkyong.com/java/java-display-list-of-timezone-with-gmt/\n"
				+ "Or google it: \"what is my time zone\"", "placeholder-format", "time", "time-zone");
		timeZone = c.getString("GMT0", "placeholder-format", "time", "time-zone", "zone");
		c.setComment("Use system default time zone instead of searching for that?", "placeholder-format", "time",
				"time-zone", "use-system-zone");
		useSystemZone = c.getBoolean(false, "placeholder-format", "time", "time-zone", "use-system-zone");
		c.setComment("Ping color format for %player-ping% placeholder.", "placeholder-format", "ping");
		pingFormatEnabled = c.getBoolean(true, "placeholder-format", "ping", "enabled");
		goodPingColor = c.getString("&a", "placeholder-format", "ping", "good-ping", "color");
		goodPingAmount = c.getInt(200, "placeholder-format", "ping", "good-ping", "amount");
		mediumPingColor = c.getString("&6", "placeholder-format", "ping", "medium-ping", "color");
		mediumPingAmount = c.getInt(200, "placeholder-format", "ping", "medium-ping", "amount");
		badPingColor = c.getString("&c", "placeholder-format", "ping", "bad-ping");

		c.setComment("Tablist, header & footer with animation.\n"
				+ "Use %anim:animationName% placeholder to make an animation.\n"
				+ "How do I use this tab section? Usage: https://github.com/montlikadani/TabList/wiki/TabList-Usage",
				"tablist");

		c.setComment("Does the tablist enabled?", "tablist", "enabled");
		tablistEnabled = c.getBoolean(true, "tablist", "enabled");
		c.setComment(
				"Define if the header & footer should be randomized.\n" + "Animation placeholders won't be affected.",
				"tablist", "random");
		randomTablist = c.getBoolean(false, "tablist", "random");
		c.setComment("Tablist refresh rate in milliseconds.", "tablist", "update-time");
		tablistUpdateTime = c.getInt(4, "tablist", "update-time");
		c.setComment("Tablist groups that shows up on player list (prefix/suffix).", "tablist-groups");
		tablistGroups = c.getBoolean(false, "tablist-groups", "enabled");
		c.setComment("This option allows you to use a different scoreboard to run groups if there is a problem\n"
				+ "with other scoreboard plugins. If it doesn’t happen that the scoreboard disappears,\n"
				+ "you don’t need to change it.", "tablist-groups", "use-own-scoreboard");
		useOwnScoreboard = c.getBoolean(false, "tablist-groups", "use-own-scoreboard");

		c.setComment("Tablist object type to display in tablist after player name.\n"
				+ "Available types: none, ping, custom, health", "tablist-objects", "type");
		tablistObjectsType = c.getString("none", "tablist-objects", "type");
		c.setComment("Custom objective setting, use any placeholder that returns an integer.", "tablist-objects",
				"settings", "custom-value");
		customObject = c.getString("%level%", "tablist-objects", "settings", "custom-value");

		c.setComment(
				"The refresh interval when the objects are refreshing.\n"
						+ "Note: The health is not updating auto due to display issues.",
				"tablist-objects", "refresh-interval");
		objectsRefreshInterval = c.getInt(3, "tablist-objects", "refresh-interval");

		c.save();
	}

	public static String getTimeFormat() {
		return timeFormat;
	}

	public static String getDateFormat() {
		return dateFormat;
	}

	public static String getTimeZone() {
		return timeZone;
	}

	public static String getTablistObjectsType() {
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

	public static boolean isUseOwnScoreboard() {
		return useOwnScoreboard;
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

	public static int getGoodPingAmount() {
		return goodPingAmount;
	}

	public static String getGoodPingColor() {
		return goodPingColor;
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
}
