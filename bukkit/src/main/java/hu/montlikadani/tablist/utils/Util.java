package hu.montlikadani.tablist.utils;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public final class Util {

	public static void logConsole(String msg) {
		logConsole(Level.INFO, msg);
	}

	public static void logConsole(Level level, String msg) {
		if (hu.montlikadani.tablist.config.constantsLoader.ConfigValues.isLogConsole()) {
			org.bukkit.Bukkit.getServer().getLogger().log(level, "[TabList] " + msg);
		}
	}

	@SuppressWarnings("deprecation")
	public static String colorText(String msg) {
		return org.bukkit.ChatColor.translateAlternateColorCodes('&', msg);
	}

	public static String applyMinimessageFormat(String value) {
		value = value.replace("&", "-{-}-").replace("§", "-{-}-");

		try {
			value = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(net.kyori.adventure.text.minimessage.MiniMessage
					.miniMessage().deserialize(value));
		} catch (Error ignored) {
		}

		return value.replace("-{-}-", "&");
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
}
