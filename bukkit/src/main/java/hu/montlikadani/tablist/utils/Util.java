package hu.montlikadani.tablist.utils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public final class Util {

	private static final boolean MINIMESSAGE_SUPPORTED;

	static {
		boolean supported;

		try {
			Class.forName("net.kyori.adventure.text.minimessage.MiniMessage");
			Class.forName("net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer");
			supported = true;
		} catch (ClassNotFoundException cn) {
			supported = false;
		}

		MINIMESSAGE_SUPPORTED = supported;
	}

	public static void logConsole(String msg) {
		logConsole(Level.INFO, msg);
	}

	public static void logConsole(Level level, String msg) {
		if (hu.montlikadani.tablist.config.constantsLoader.ConfigValues.isLogConsole()) {
			org.bukkit.Bukkit.getServer().getLogger().log(level, "[TabList] " + msg);
		}
	}

	public static String applyMinimessageFormat(String value) {
		return applyMinimessageFormat(value, true);
	}

	@SuppressWarnings("deprecation")
	public static String applyMinimessageFormat(String value, boolean applyLegacyColours) {
		if (MINIMESSAGE_SUPPORTED) {
			value = value.replace("&", "-{-}-").replace("§", "-{-}-");

			value = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand()
					.serialize(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(value))
					.replace("-{-}-", "&");
		}

		return applyLegacyColours ? org.bukkit.ChatColor.translateAlternateColorCodes('&', value) : value;
	}

	public static List<String> applyMinimessageFormat(List<String> list) {
		return applyMinimessageFormat(list, true);
	}

	public static List<String> applyMinimessageFormat(List<String> list, boolean applyLegacyColours) {
		list.replaceAll(value -> applyMinimessageFormat(value, applyLegacyColours));

		return list;
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
