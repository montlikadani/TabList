package hu.montlikadani.tablist.bukkit.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.utils.ServerVersion.Version;

public class Util {

	public static void logConsole(String msg) {
		logConsole(msg, true);
	}

	public static void logConsole(String msg, boolean loaded) {
		logConsole(Level.INFO, msg, loaded);
	}

	public static void logConsole(Level level, String msg) {
		logConsole(level, msg, true);
	}

	public static void logConsole(Level level, String msg, boolean loaded) {
		if ((!loaded || TabList.getInstance().getC().getBoolean("logconsole", true)) && msg != null
				&& !msg.trim().isEmpty()) {
			Bukkit.getLogger().log(level != null ? level : Level.INFO, "[TabList] " + msg);
		}
	}

	public static String colorMsg(String msg) {
		if (msg == null) {
			return "";
		}

		if (msg.contains("#") && Version.isCurrentEqualOrHigher(Version.v1_16_R1)) {
			for (String s : matchColorRegex(msg)) {
				msg = msg.replace("<" + s + ">", net.md_5.bungee.api.ChatColor.of(s).toString());
			}
		}

		return ChatColor.translateAlternateColorCodes('&', msg);
	}

	private static List<String> matchColorRegex(String s) {
		List<String> matches = new ArrayList<>();
		Matcher matcher = Pattern.compile("<(.*?)>").matcher(s);
		while (matcher.find()) {
			for (int i = 1; i <= matcher.groupCount(); i++) {
				matches.add(matcher.group(i));
			}
		}

		return matches;
	}

	public static void sendMsg(CommandSender sender, String s) {
		if (s != null && !s.isEmpty()) {
			if (s.contains("\n")) {
				for (String msg : s.split("\n")) {
					sender.sendMessage(msg);
				}
			} else {
				sender.sendMessage(s);
			}
		}
	}

	public static String stripColor(String str) {
		if (str.contains("&a"))
			str = str.replace("&a", "");

		if (str.contains("&b"))
			str = str.replace("&b", "");

		if (str.contains("&c"))
			str = str.replace("&c", "");

		if (str.contains("&d"))
			str = str.replace("&d", "");

		if (str.contains("&e"))
			str = str.replace("&e", "");

		if (str.contains("&f"))
			str = str.replace("&f", "");

		for (int i = 0; i < 10; i++) {
			if (str.contains("&" + i))
				str = str.replace("&" + i, "");
		}

		if (str.contains("&n"))
			str = str.replace("&n", "");

		if (str.contains("&o"))
			str = str.replace("&o", "");

		if (str.contains("&m"))
			str = str.replace("&m", "");

		if (str.contains("&k"))
			str = str.replace("&k", "");

		if (str.contains("&l"))
			str = str.replace("&l", "");

		str = ChatColor.stripColor(str);
		return str;
	}
}
