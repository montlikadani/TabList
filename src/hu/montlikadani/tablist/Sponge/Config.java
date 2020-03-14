package hu.montlikadani.tablist.Sponge;

import java.util.Arrays;
import java.util.List;

public class Config {

	public static double configVersion = 1.0;

	private static String timeZone;
	private static String timeFormat;
	private static String dateFormat;

	private static int tabUpdateTime;

	private static boolean useSystemTimeZone;
	private static boolean tabEnabled;
	private static boolean randomTexts;

	private static List<String> disabledWorlds;
	private static List<String> blacklistedPlayers;

	static void load() {
		ConfigManager config = TabList.get().getConfig().getConfig();

		//config.setComment("Placeholder formatting", "placeholder-format");
		config.setComment("Time zones: https://www.mkyong.com/java/java-display-list-of-timezone-with-gmt/\r\n"
				+ "Or google it: what is my time zone", new Object[] { "placeholder-format", "time", "time-zone" });
		timeZone = config.getString("GMT0", "placeholder-format", "time", "time-zone");
		config.setComment("Use system default time zone instead of searching for that?", "placeholder-format", "time",
				"use-system-zone");
		useSystemTimeZone = config.getBoolean(false, "placeholder-format", "time", "use-system-zone");
		config.setComment(
				"Formats/examples: https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html\r\n"
						+ "Used for %server-time% placeholder.",
				"placeholder-format", "time", "time-format", "format");
		timeFormat = config.getString(new Object[] { "placeholder-format", "time", "time-format", "format" });
		config.setComment("Used for %date% placeholder", "placeholder-format", "time", "date-format", "format");
		dateFormat = config.getString(new Object[] { "placeholder-format", "time", "date-format", "format" });

		config.setComment(
				"Tablist, header & footer with animation.\r\nUse %anim:animationName% placeholder to make an animation."
						+ "\r\nHow do I use this tab section? Usage: https://github.com/montlikadani/TabList/wiki/TabList-Usage",
				"tablist");
		config.setComment("Does the tablist enabled?", "tablist", "enabled");
		tabEnabled = config.getBoolean(true, "tablist", "enabled");
		config.setComment("Tablist refresh rate in milliseconds.", "tablist", "update-time");
		tabUpdateTime = config.getInt(4, "tablist", "update-time");
		config.setComment("The listed worlds where the tablist will not show", "tablist", "disabled-worlds");
		disabledWorlds = config.getStringList(Arrays.asList("myWorld"), "tablist", "disabled-worlds");
		config.setComment("Blacklisted players, where the player not see the tab.", "tablist", "blacklisted-players");
		blacklistedPlayers = config.getStringList(Arrays.asList(""), "tablist", "blacklisted-players");
		config.setComment(
				"Define if the header & footer should be randomized.\r\nAnimation placeholders won't be affected.",
				"tablist", "random");
		randomTexts = config.getBoolean(false, "tablist", "random");

		if (config.getString(new Object[] { "tablist", "header" }) == null) {
			config.set("&6&lWelcome&a %player%", "tablist", "header");
		}
		if (config.getStringList("tablist", "footer") == null) {
			config.set(Arrays.asList("&e&lPlayers:&6 %online-players%&7/&6%max-players%"), "tablist", "footer");
		}
		config.save();
	}

	public static String getTimeZone() {
		return timeZone;
	}

	public static boolean isUsingSystemTimeZone() {
		return useSystemTimeZone;
	}

	public static String getTimeFormat() {
		return timeFormat;
	}

	public static String getDateFormat() {
		return dateFormat;
	}

	public static boolean isTabEnabled() {
		return tabEnabled;
	}

	public static int getTabUpdateTime() {
		return tabUpdateTime;
	}

	public static List<String> getDisabledWorlds() {
		return disabledWorlds;
	}

	public static List<String> getBlackListedPlayers() {
		return blacklistedPlayers;
	}

	public static boolean isTabTextRandom() {
		return randomTexts;
	}
}
