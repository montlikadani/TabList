package hu.montlikadani.tablist;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Global {

	private Global() {
	}

	private static final Pattern PATTERN = Pattern.compile("#[a-fA-F0-9]{6}");

	public static String matchColorRegex(String s) {
		Matcher matcher = PATTERN.matcher(s);

		while (matcher.find()) {
			String group = matcher.group(0);

			try {
				s = s.replace(group, net.md_5.bungee.api.ChatColor.of(group).toString());
			} catch (IllegalArgumentException e) {
			}
		}

		return s;
	}

	public static String setSymbols(String s) {
		if (s.indexOf('<') < 0) {
			return s;
		}

		int i = -1;

		for (String symbol : Symbols.SYMBOLS) {
			String sym = "<" + ++i + ">";

			if (s.indexOf(sym) >= 0) {
				s = s.replace(sym, symbol);
			}
		}

		return s;
	}
}
