package hu.montlikadani.tablist.bukkit.utils;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import hu.montlikadani.tablist.Global;
import hu.montlikadani.tablist.bukkit.config.constantsLoader.ConfigValues;

public class Util {

	public static void logConsole(String msg) {
		logConsole(Level.INFO, msg);
	}

	public static void logConsole(Level level, String msg) {
		if (ConfigValues.isLogConsole() && msg != null && !msg.trim().isEmpty()) {
			Bukkit.getLogger().log(level != null ? level : Level.INFO, "[TabList] " + msg);
		}
	}

	public static String colorMsg(String msg) {
		if (msg == null) {
			return "";
		}

		if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_16_R1) && msg.contains("#")) {
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

	public static Optional<UUID> tryParseId(String uuid) {
		if (uuid == null || uuid.trim().isEmpty()) {
			return Optional.empty();
		}

		try {
			return Optional.of(UUID.fromString(uuid));
		} catch (NumberFormatException e) {
		}

		return Optional.empty();
	}

	public static Optional<Integer> tryParse(String parseable) {
		try {
			return Optional.of(Integer.parseInt(parseable));
		} catch (NumberFormatException e) {
		}

		return Optional.empty();
	}
}
