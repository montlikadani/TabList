package hu.montlikadani.tablist;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

import hu.montlikadani.tablist.utils.operators.ExpressionNode;
import hu.montlikadani.tablist.utils.operators.OperatorNodes;

public class Variables {

	private final List<ExpressionNode> nodes = new ArrayList<>();

	public void loadExpressions() {
		nodes.clear();

		if (ConfigValues.isPingFormatEnabled()) {
			for (String f : ConfigValues.getPingColorFormats()) {
				nodes.add(new OperatorNodes(f));
			}
		}

		// Sort ping in descending order
		for (int i = 0; i < nodes.size(); i++) {
			for (int j = nodes.size() - 1; j > i; j--) {
				ExpressionNode node = nodes.get(i), node2 = nodes.get(j);
				if (node.getCondition().getSecondCondition() < node2.getCondition().getSecondCondition()) {
					nodes.set(i, node2);
					nodes.set(j, node);
				}
			}
		}
	}

	public Text replaceVariables(Player p, String str) {
		int staffs = 0;
		if (str.contains("%staff-online%")) {
			for (Player all : Sponge.getServer().getOnlinePlayers()) {
				if (all.hasPermission("tablist.onlinestaff")) {
					staffs++;
				}
			}
		}

		String address = "";
		if (str.contains("%ip-address%")) {
			address = p.getConnection().getAddress().getAddress().toString();
			address = address.replace("/", "");
		}

		if (str.contains("%tps%")) {
			DecimalFormat df = (DecimalFormat) NumberFormat.getNumberInstance(Locale.US);
			df.applyPattern("#0.00");
			str = str.replace("%tps%", df.format(Sponge.getServer().getTicksPerSecond()));
		}

		String t = "", dt = "";
		if (str.contains("%server-time%") || str.contains("%date%")) {
			DateTimeFormatter form = DateTimeFormatter.ofPattern(ConfigValues.getTimeFormat()),
					form2 = DateTimeFormatter.ofPattern(ConfigValues.getDateFormat());
			TimeZone zone = ConfigValues.isUseSystemZone() ? TimeZone.getTimeZone(java.time.ZoneId.systemDefault())
					: TimeZone.getTimeZone(ConfigValues.getTimeZone());
			LocalDateTime now = zone == null ? LocalDateTime.now() : LocalDateTime.now(zone.toZoneId());

			Calendar cal = Calendar.getInstance();

			t = form != null ? now.format(form) : cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE);
			dt = form2 != null ? now.format(form2) : cal.get(Calendar.YEAR) + "/" + cal.get(Calendar.DATE);
		}

		Runtime r = Runtime.getRuntime();
		Long fram = Long.valueOf(r.freeMemory() / 1048576L),
				mram = Long.valueOf(r.maxMemory() / 1048576L),
				uram = Long.valueOf((r.totalMemory() - r.freeMemory()) / 1048576L);

		str = setSymbols(str);

		if (str.contains("%player%")) {
			str = str.replace("%player%", p.getName());
		}

		if (str.contains("%player-ping%")) {
			str = str.replace("%player-ping%", formatPing(p.getConnection().getLatency()));
		}

		if (str.contains("%player-uuid%")) {
			str = str.replace("%player-uuid%", p.getUniqueId().toString());
		}

		if (str.contains("%player-level%")) {
			str = str.replace("%player-level%", Integer.toString(p.get(Keys.EXPERIENCE_LEVEL).orElse(0)));
		}

		if (str.contains("%player-total-level%")) {
			str = str.replace("%player-total-level%", Integer.toString(p.get(Keys.TOTAL_EXPERIENCE).orElse(0)));
		}

		if (str.contains("%world%")) {
			str = str.replace("%world%", p.getWorld().getName());
		}

		if (str.contains("%player-health%")) {
			DecimalFormat df = (DecimalFormat) NumberFormat.getNumberInstance(Locale.US);
			df.applyPattern("#0.0");
			str = str.replace("%player-health%", df.format(p.getHealthData().health().get()));
		}

		if (str.contains("%player-max-health%")) {
			str = str.replace("%player-max-health%", Double.toString(p.getHealthData().maxHealth().get()));
		}

		if (!t.isEmpty())
			str = str.replace("%server-time%", t);

		if (!dt.isEmpty())
			str = str.replace("%date%", dt);

		if (str.contains("%server-ram-free%"))
			str = str.replace("%server-ram-free%", Long.toString(fram.longValue()));

		if (str.contains("%server-ram-max%"))
			str = str.replace("%server-ram-max%", Long.toString(mram.longValue()));

		if (str.contains("%server-ram-used%"))
			str = str.replace("%server-ram-used%", Long.toString(uram.longValue()));

		if (str.contains("%online-players%"))
			str = str.replace("%online-players%", Integer.toString(Sponge.getServer().getOnlinePlayers().size()));

		if (str.contains("%max-players%"))
			str = str.replace("%max-players%", Integer.toString(Sponge.getServer().getMaxPlayers()));

		if (str.contains("%servertype%"))
			str = str.replace("%servertype%", Sponge.getGame().getPlatform().getType().name());

		if (str.contains("%staff-online%")) {
			str = str.replace("%staff-online%", Integer.toString(staffs));
		}

		if (!address.isEmpty())
			str = str.replace("%ip-address%", address);

		if (str.contains("%mc-version%"))
			str = str.replace("%mc-version%", Sponge.getGame().getPlatform().getMinecraftVersion().getName());

		if (str.contains("%motd%"))
			str = str.replace("%motd%", Sponge.getServer().getMotd().toPlain());

		str = str.replace("\n", "\n");
		return TextSerializers.FORMATTING_CODE.deserialize(str);
	}

	private String formatPing(int ping) {
		if (!ConfigValues.isPingFormatEnabled() || ConfigValues.getPingColorFormats().isEmpty()) {
			return "" + ping;
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

	public String setSymbols(String s) {
		s = s.replace("<0>", "•");
		s = s.replace("<1>", "➤");
		s = s.replace("<2>", "™");
		s = s.replace("<3>", "↑");
		s = s.replace("<4>", "→");
		s = s.replace("<5>", "↓");
		s = s.replace("<6>", "∞");
		s = s.replace("<7>", "░");
		s = s.replace("<8>", "▲");
		s = s.replace("<9>", "▶");
		s = s.replace("<10>", "◀");
		s = s.replace("<11>", "●");
		s = s.replace("<12>", "★");
		s = s.replace("<13>", "☆");
		s = s.replace("<14>", "☐");
		s = s.replace("<15>", "☑");
		s = s.replace("<16>", "☠");
		s = s.replace("<17>", "☢");
		s = s.replace("<18>", "☣");
		s = s.replace("<19>", "☹");
		s = s.replace("<20>", "☺");
		s = s.replace("<21>", "✓");
		s = s.replace("<22>", "✔");
		s = s.replace("<23>", "✘");
		s = s.replace("<24>", "✚");
		s = s.replace("<25>", "℻");
		s = s.replace("<26>", "✠");
		s = s.replace("<27>", "✡");
		s = s.replace("<28>", "✦");
		s = s.replace("<29>", "✧");
		s = s.replace("<30>", "✩");
		s = s.replace("<31>", "✪");
		s = s.replace("<32>", "✮");
		s = s.replace("<33>", "✯");
		s = s.replace("<34>", "㋡");
		s = s.replace("<35>", "❝");
		s = s.replace("<36>", "❞");
		s = s.replace("<37>", "ツ");
		s = s.replace("<38>", "♩");
		s = s.replace("<39>", "♪");
		s = s.replace("<40>", "♫");
		s = s.replace("<41>", "♬");
		s = s.replace("<42>", "♭");
		s = s.replace("<43>", "♮");
		s = s.replace("<44>", "♯");
		s = s.replace("<45>", "¶");
		s = s.replace("<46>", "\u00A9");
		s = s.replace("<47>", "\u00AE");
		s = s.replace("<48>", "⏎");
		s = s.replace("<49>", "⇧");
		s = s.replace("<50>", "⇪");
		s = s.replace("<51>", "ᴴᴰ");
		s = s.replace("<52>", "☒");
		s = s.replace("<53>", "♠");
		s = s.replace("<54>", "♣");
		s = s.replace("<55>", "☻");
		s = s.replace("<56>", "▓");
		s = s.replace("<57>", "➾");
		s = s.replace("<58>", "➔");
		s = s.replace("<59>", "➳");
		s = s.replace("<60>", "➧");
		s = s.replace("<61>", "《");
		s = s.replace("<62>", "》");
		s = s.replace("<63>", "︾");
		s = s.replace("<64>", "︽");
		s = s.replace("<65>", "☃");
		s = s.replace("<66>", "¹");
		s = s.replace("<67>", "²");
		s = s.replace("<68>", "³");
		s = s.replace("<69>", "≈");
		s = s.replace("<70>", "℠");
		s = s.replace("<71>", "\u2665");
		s = s.replace("<72>", "✬");
		s = s.replace("<73>", "↔");
		s = s.replace("<74>", "«");
		s = s.replace("<75>", "»");
		s = s.replace("<76>", "☀");
		s = s.replace("<77>", "♦");
		s = s.replace("<78>", "₽");
		s = s.replace("<79>", "☎");
		s = s.replace("<80>", "☂");
		s = s.replace("<81>", "←");
		s = s.replace("<82>", "↖");
		s = s.replace("<83>", "↗");
		s = s.replace("<84>", "↘");
		s = s.replace("<85>", "↙");
		s = s.replace("<86>", "➲");
		s = s.replace("<87>", "✐");
		s = s.replace("<88>", "✎");
		s = s.replace("<89>", "✏");
		s = s.replace("<90>", "✆");
		s = s.replace("<91>", "◄");
		s = s.replace("<92>", "☼");
		s = s.replace("<93>", "►");
		s = s.replace("<94>", "↕");
		s = s.replace("<95>", "▼");
		s = s.replace("<96>", "①");
		s = s.replace("<97>", "②");
		s = s.replace("<98>", "③");
		s = s.replace("<99>", "④");
		s = s.replace("<100>", "⑤");
		s = s.replace("<101>", "⑥");
		s = s.replace("<102>", "⑦");
		s = s.replace("<103>", "⑧");
		s = s.replace("<104>", "⑨");
		s = s.replace("<105>", "⑩");
		s = s.replace("<106>", "⑪");
		s = s.replace("<107>", "⑫");
		s = s.replace("<108>", "⑬");
		s = s.replace("<109>", "⑭");
		s = s.replace("<110>", "⑮");
		s = s.replace("<111>", "⑯");
		s = s.replace("<112>", "⑰");
		s = s.replace("<113>", "⑱");
		s = s.replace("<114>", "⑲");
		s = s.replace("<115>", "⑳");
		s = s.replace("<116>", "♨");
		s = s.replace("<117>", "✑");
		s = s.replace("<118>", "✖");
		s = s.replace("<119>", "✰");
		s = s.replace("<120>", "✶");
		s = s.replace("<121>", "╗");
		s = s.replace("<122>", "╣");
		s = s.replace("<123>", "◙");
		s = s.replace("<124>", "○");
		s = s.replace("<125>", "╠");
		s = s.replace("<126>", "┤");
		s = s.replace("<127>", "║");
		s = s.replace("<128>", "╝");
		s = s.replace("<129>", "⌂");
		s = s.replace("<130>", "┐");
		s = s.replace("<131>", "❉");
		s = s.replace("<132>", "⌲");
		s = s.replace("<133>", "½");
		s = s.replace("<134>", "¼");
		s = s.replace("<135>", "¾");
		s = s.replace("<136>", "⅓");
		s = s.replace("<137>", "⅔");
		s = s.replace("<138>", "№");
		s = s.replace("<139>", "†");
		s = s.replace("<140>", "‡");
		s = s.replace("<141>", "µ");
		s = s.replace("<142>", "¢");
		s = s.replace("<143>", "£");
		s = s.replace("<144>", "∅");
		s = s.replace("<145>", "≤");
		s = s.replace("<146>", "≥");
		s = s.replace("<147>", "≠");
		s = s.replace("<148>", "∧");
		s = s.replace("<149>", "∨");
		s = s.replace("<150>", "∩");
		s = s.replace("<151>", "∪");
		s = s.replace("<152>", "∈");
		s = s.replace("<153>", "∀");
		s = s.replace("<154>", "∃");
		s = s.replace("<155>", "∄");
		s = s.replace("<156>", "∑");
		s = s.replace("<157>", "∏");
		s = s.replace("<158>", "↺");
		s = s.replace("<159>", "↻");
		s = s.replace("<160>", "Ω");

		return s;
	}
}
