package hu.montlikadani.tablist.bukkit.utils;

import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import hu.montlikadani.tablist.Global;
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
			msg = Global.matchColorRegex(msg);
		}

		return ChatColor.translateAlternateColorCodes('&', msg);
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
		for (ChatColor color : ChatColor.values()) {
			if (str.contains(("&" + color.getChar()))) {
				str = str.replace("&" + color.getChar(), "");
			}
		}

		str = ChatColor.stripColor(str);
		return str;
	}

	public static boolean isRealUUID(String uuid) {
		if (uuid == null || uuid.trim().isEmpty()) {
			return true;
		}

		try {
			UUID.fromString(uuid);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}
}
