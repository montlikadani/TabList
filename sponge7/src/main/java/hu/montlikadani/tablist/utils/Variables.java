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
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

import hu.montlikadani.tablist.config.ConfigValues;
import hu.montlikadani.tablist.utils.operators.ExpressionNode;
import hu.montlikadani.tablist.utils.operators.OperatorNodes;

public final class Variables {

	private final List<ExpressionNode> nodes = new ArrayList<>();
	private final java.util.Set<String> symbols = new java.util.HashSet<>();

	private DecimalFormat tpsFormat, healthFormat;

	public void loadExpressions() {
		nodes.clear();

		if (symbols.isEmpty()) {
			symbols.addAll(java.util.Arrays.asList("\u2022", "\u27A4", "\u2122", "\u2191", "\u2192", "\u2193", "\u221E", "\u2591", "\u25B2", "\u25B6", "\u25C0", "\u25CF", "\u2605",
					"\u2606", "\u2610", "\u2611", "\u2620", "\u2622", "\u2623", "\u2639", "\u263A", "\u2713", "\u2714", "\u2718", "\u271A", "\u213B", "\u2720", "\u2721", "\u2726",
					"\u2727", "\u2729", "\u272A", "\u272E", "\u272F", "\u32E1", "\u275D", "\u275E", "\u30C4", "\u2669", "\u266A", "\u266B", "\u266C", "\u266D", "\u266E", "\u266F",
					"\u00B6", "\u00A9", "\u00AE", "\u23CE", "\u21E7", "\u21EA", "\u1D34\u1D30", "\u2612", "\u2660", "\u2663", "\u263B", "\u2593", "\u27BE", "\u2794", "\u27B3",
					"\u27A7", "\u300A", "\u300B", "\uFE3E", "\uFE3D", "\u2603", "\u00B9", "\u00B2", "\u00B3", "\u2248", "\u2120", "\u2665", "\u272C", "\u2194", "\u00AB", "\u00BB",
					"\u2600", "\u2666", "\u20BD", "\u260E", "\u2602", "\u2190", "\u2196", "\u2197", "\u2198", "\u2199", "\u27B2", "\u2710", "\u270E", "\u270F", "\u2706", "\u25C4",
					"\u263C", "\u25BA", "\u2195", "\u25BC", "\u2460", "\u2461", "\u2462", "\u2463", "\u2464", "\u2465", "\u2466", "\u2467", "\u2468", "\u2469", "\u246A", "\u246B",
					"\u246C", "\u246D", "\u246E", "\u246F", "\u2470", "\u2471", "\u2472", "\u2473", "\u2668", "\u2711", "\u2716", "\u2730", "\u2736", "\u2557", "\u2563", "\u25D9",
					"\u25CB", "\u2560", "\u2524", "\u2551", "\u255D", "\u2302", "\u2510", "\u2749", "\u2332", "\u00BD", "\u00BC", "\u00BE", "\u2153", "\u2154", "\u2116", "\u2020",
					"\u2021", "\u00B5", "\u00A2", "\u00A3", "\u2205", "\u2264", "\u2265", "\u2260", "\u2227", "\u2228", "\u2229", "\u222A", "\u2208", "\u2200", "\u2203", "\u2204",
					"\u2211", "\u220F", "\u21BA", "\u21BB", "\u03A9"));
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

	public String replaceIntegerVariables(Player player, String str) {
		if (str.indexOf("%player-ping%") != -1) {
			str = str.replace("%player-ping%", formatPing(player.getConnection().getLatency()));
		}

		if (str.indexOf("%player-level%") != -1) {
			str = str.replace("%player-level%", Integer.toString(player.get(Keys.EXPERIENCE_LEVEL).orElse(0)));
		}

		if (str.indexOf("%player-total-level%") != -1) {
			str = str.replace("%player-total-level%", Integer.toString(player.get(Keys.TOTAL_EXPERIENCE).orElse(0)));
		}

		if (str.indexOf("%online-players%") != -1)
			str = str.replace("%online-players%", Integer.toString(Sponge.getGame().getServer().getOnlinePlayers().size()));

		if (str.indexOf("%max-players%") != -1)
			str = str.replace("%max-players%", Integer.toString(Sponge.getGame().getServer().getMaxPlayers()));

		if (str.indexOf("%staff-online%") != -1) {
			int staffs = 0;

			for (Player all : Sponge.getGame().getServer().getOnlinePlayers()) {
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

	public Text replaceVariables(Player player, String str) {
		if (str.isEmpty()) {
			return TextSerializers.FORMATTING_CODE.deserialize(str);
		}

		str = str.replace("%player%", player.getName());
		str = str.replace("%servertype%", Sponge.getPlatform().getType().name());
		str = str.replace("%mc-version%", Sponge.getPlatform().getMinecraftVersion().getName());

		str = replaceIntegerVariables(player, str);

		if (str.indexOf("%motd%") != -1) {
			str = str.replace("%motd%", Sponge.getGame().getServer().getMotd().toPlain());
		}

		if (str.indexOf("%world%") != -1) {
			str = str.replace("%world%", player.getWorld().getName());
		}

		if (str.indexOf("%player-uuid%") != -1) {
			str = str.replace("%player-uuid%", player.getUniqueId().toString());
		}

		if (str.indexOf("%player-health%") != -1) {
			str = str.replace("%player-health%", healthFormat.format(player.getHealthData().health().get()));
		}

		if (str.indexOf("%player-max-health%") != -1) {
			str = str.replace("%player-max-health%", Double.toString(player.getHealthData().maxHealth().get()));
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
			java.net.InetAddress inetAddress = player.getConnection().getAddress().getAddress();

			if (inetAddress != null) {
				str = str.replace("%ip-address%", inetAddress.toString().replace("/", ""));
			}
		}

		if (str.indexOf("%tps%") != -1) {
			str = str.replace("%tps%", tpsFormat.format(Sponge.getGame().getServer().getTicksPerSecond()));
		}

		return TextSerializers.FORMATTING_CODE.deserialize(str);
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
