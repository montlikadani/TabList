package hu.montlikadani.tablist.utils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

import hu.montlikadani.tablist.config.ConfigValues;
import hu.montlikadani.tablist.utils.operators.ExpressionNode;
import hu.montlikadani.tablist.utils.operators.OperatorNodes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public final class Variables {

	private final List<ExpressionNode> nodes = new ArrayList<>();
	private final java.util.Set<String> symbols = new java.util.HashSet<>();

	private DecimalFormat tpsFormat, healthFormat;

	public void loadExpressions() {
		nodes.clear();

		if (symbols.isEmpty()) {
			symbols.addAll(java.util.Arrays.asList("•", "➤", "™", "↑", "→", "↓", "∞", "░", "▲", "▶", "◀", "●", "★", "☆", "☐", "☑",
					"☠", "☢", "☣", "☹", "☺", "✓", "✔", "✘", "✚", "℻", "✠", "✡", "✦", "✧", "✩", "✪", "✮", "✯", "㋡", "❝", "❞", "ツ",
					"♩", "♪", "♫", "♬", "♭", "♮", "♯", "¶", "\u00A9", "\u00AE", "⏎", "⇧", "⇪", "ᴴᴰ", "☒", "♠", "♣", "☻", "▓", "➾",
					"➔", "➳", "➧", "《", "》", "︾", "︽", "☃", "¹", "²", "³", "≈", "℠", "\u2665", "✬", "↔", "«", "»", "☀", "♦", "₽",
					"☎", "☂", "←", "↖", "↗", "↘", "↙", "➲", "✐", "✎", "✏", "✆", "◄", "☼", "►", "↕", "▼", "①", "②", "③", "④", "⑤",
					"⑥", "⑦", "⑧", "⑨", "⑩", "⑪", "⑫", "⑬", "⑭", "⑮", "⑯", "⑰", "⑱", "⑲", "⑳", "♨", "✑", "✖", "✰", "✶", "╗", "╣",
					"◙", "○", "╠", "┤", "║", "╝", "⌂", "┐", "❉", "⌲", "½", "¼", "¾", "⅓", "⅔", "№", "†", "‡", "µ", "¢", "£", "∅",
					"≤", "≥", "≠", "∧", "∨", "∩", "∪", "∈", "∀", "∃", "∄", "∑", "∏", "↺", "↻", "Ω"));
		}

		if (!ConfigValues.isPingFormatEnabled()) {
			return;
		}

		for (String f : ConfigValues.getPingColorFormats()) {
			ExpressionNode node = new OperatorNodes(f);

			if (node.getCondition() != null) {
				nodes.add(node);
			}
		}

		int size = nodes.size();
		int start = size - 1;

		// Sort ping in descending order
		for (int i = 0; i < size; i++) {
			for (int j = start; j > i; j--) {
				ExpressionNode node = nodes.get(i), node2 = nodes.get(j);

				if (node.getCondition().getSecondCondition() < node2.getCondition().getSecondCondition()) {
					nodes.set(i, node2);
					nodes.set(j, node);
				}
			}
		}

		DecimalFormat numberInst = (DecimalFormat) NumberFormat.getNumberInstance(Locale.US);

		tpsFormat = numberInst;
		tpsFormat.applyPattern("#0.00");

		healthFormat = numberInst;
		healthFormat.applyPattern("#0.0");
	}

	public String replaceIntegerVariables(ServerPlayer player, String str) {
		if (str.indexOf("%player-ping%") != -1) {
			str = str.replace("%player-ping%", formatPing(player.connection().latency()));
		}

		if (str.indexOf("%player-level%") != -1) {
			str = str.replace("%player-level%", Integer.toString(player.get(Keys.EXPERIENCE_LEVEL).orElse(0)));
		}

		if (str.indexOf("%player-total-level%") != -1) {
			str = str.replace("%player-total-level%",
					Integer.toString(player.get(Keys.EXPERIENCE_FROM_START_OF_LEVEL).orElse(0)));
		}

		if (str.indexOf("%online-players%") != -1)
			str = str.replace("%online-players%", Integer.toString(Sponge.game().server().onlinePlayers().size()));

		if (str.indexOf("%max-players%") != -1)
			str = str.replace("%max-players%", Integer.toString(Sponge.game().server().maxPlayers()));

		if (str.indexOf("%staff-online%") != -1) {
			int staffs = 0;

			for (ServerPlayer all : Sponge.game().server().onlinePlayers()) {
				if (all.hasPermission("tablist.onlinestaff")) {
					staffs++;
				}
			}

			if (staffs != 0) {
				str = str.replace("%staff-online%", Integer.toString(staffs));
			}
		}

		return str;
	}

	public Component replaceVariables(ServerPlayer player, String str) {
		if (str.isEmpty()) {
			return Component.empty();
		}

		str = str.replace("%player%", player.name());
		str = str.replace("%servertype%", Sponge.platform().type().name());
		str = str.replace("%mc-version%", Sponge.platform().minecraftVersion().name());

		str = replaceIntegerVariables(player, str);

		if (str.indexOf("%motd%") != -1) {
			str = str.replace("%motd%", PlainTextComponentSerializer.plainText().serialize(Sponge.game().server().motd()));
		}

		if (str.indexOf("%world%") != -1) {
			str = str.replace("%world%", player.world().context().getKey());
		}

		if (str.indexOf("%player-uuid%") != -1) {
			str = str.replace("%player-uuid%", player.uniqueId().toString());
		}

		if (str.indexOf("%player-health%") != -1) {
			str = str.replace("%player-health%", healthFormat.format(player.health().get()));
		}

		if (str.indexOf("%player-max-health%") != -1) {
			str = str.replace("%player-max-health%", Double.toString(player.maxHealth().get()));
		}

		Runtime runtime = Runtime.getRuntime();

		if (str.indexOf("%server-ram-free%") != -1) {
			str = str.replace("%server-ram-free%", Long.toString(runtime.freeMemory() / 1048576L));
		}

		if (str.indexOf("%server-ram-max%") != -1) {
			str = str.replace("%server-ram-max%", Long.toString(runtime.maxMemory() / 1048576L));
		}

		if (str.indexOf("%server-ram-used%") != -1) {
			str = str.replace("%server-ram-used%", Long.toString((runtime.totalMemory() - runtime.freeMemory()) / 1048576L));
		}

		if (str.indexOf("%server-time%") != -1) {
			str = str.replace("%server-time%", getTimeAsString(ConfigValues.getTimeFormat()));
		}

		if (str.indexOf("%date%") != -1) {
			str = str.replace("%date%", getTimeAsString(ConfigValues.getDateFormat()));
		}

		if (str.indexOf("%ip-address%") != -1) {
			java.net.InetAddress inetAddress = player.connection().address().getAddress();

			if (inetAddress != null) {
				str = str.replace("%ip-address%", inetAddress.toString().replace("/", ""));
			}
		}

		if (str.indexOf("%tps%") != -1) {
			str = str.replace("%tps%", tpsFormat.format(Sponge.game().server().ticksPerSecond()));
		}

		return LegacyComponentSerializer.legacyAmpersand().deserialize(str);
	}

	private String formatPing(int ping) {
		if (!ConfigValues.isPingFormatEnabled() || ConfigValues.getPingColorFormats().isEmpty()) {
			return Integer.toString(ping);
		}

		return parseExpression(ping);
	}

	private String parseExpression(int value) {
		String color = "";

		for (ExpressionNode node : nodes) {
			if (node.parse(value)) {
				color = node.getCondition().getColor();
			}
		}

		StringBuilder builder = new StringBuilder();
		if (!color.isEmpty()) {
			builder.append(color.replace('&', '\u00a7'));
		}

		return builder.append(value).toString();
	}

	private String getTimeAsString(DateTimeFormatter formatter) {
		TimeZone zone = ConfigValues.isUseSystemZone() ? TimeZone.getTimeZone(java.time.ZoneId.systemDefault())
				: TimeZone.getTimeZone(ConfigValues.getTimeZone());
		LocalDateTime now = zone == null ? LocalDateTime.now() : LocalDateTime.now(zone.toZoneId());

		return now.format(formatter);
	}

	public String setSymbols(String s) {
		if (s.indexOf('<') == -1) {
			return s;
		}

		int i = -1;

		for (String symbol : symbols) {
			s = s.replace('<' + Integer.toString(++i) + '>', symbol);
		}

		return s;
	}
}
