package hu.montlikadani.tablist.utils;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

import hu.montlikadani.tablist.Global;
import hu.montlikadani.tablist.config.constantsLoader.ConfigValues;

public final class Util {

	public static void logConsole(String msg) {
		logConsole(Level.INFO, msg);
	}

	public static void logConsole(Level level, String msg) {
		if (ConfigValues.isLogConsole()) {
			org.bukkit.Bukkit.getServer().getLogger().log(level, "[TabList] " + msg);
		}
	}

	public static String colorText(String msg) {
		if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_16_R1) && msg.indexOf('#') != -1) {
			msg = Global.matchHexColour(msg);
		}

		return org.bukkit.ChatColor.translateAlternateColorCodes('&', msg);
	}

	public static void sendMsg(org.bukkit.command.CommandSender sender, String s) {
		if (s.isEmpty()) {
			return;
		}

		String[] split = s.split("\n");

		if (split.length != 0) {
			for (String msg : split) {
				sender.sendMessage(msg);
			}
		} else {
			sender.sendMessage(s);
		}
	}

	public static Optional<UUID> tryParseId(String uuid) {
		if (uuid == null || uuid.length() < 36) {
			return Optional.empty();
		}

		try {
			return Optional.of(UUID.fromString(uuid));
		} catch (IllegalArgumentException e) {
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
