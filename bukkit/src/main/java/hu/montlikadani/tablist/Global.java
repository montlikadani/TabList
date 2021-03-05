package hu.montlikadani.tablist;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

public class Global {

	private static final Pattern PATTERN = Pattern.compile("#[a-fA-F0-9]{6}");

	public static String matchColorRegex(String s) {
		Matcher matcher = PATTERN.matcher(s);
		while (matcher.find()) {
			String group = matcher.group(0);

			try {
				s = StringUtils.replace(s, group, net.md_5.bungee.api.ChatColor.of(group) + "");
			} catch (Exception e) {
				System.out.println("[TabList] Invalid hex color " + e.getLocalizedMessage());
			}
		}

		return s;
	}

	// TODO subject to remove or optimise?
	public static String setSymbols(String s) {
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
