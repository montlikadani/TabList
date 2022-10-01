package hu.montlikadani.tablist;

import java.util.function.Supplier;
import java.util.regex.Pattern;

import net.kyori.adventure.text.format.TextColor;

public final class Global {

	private Global() {
	}

	public static final Pattern HEX_PATTERN = Pattern.compile("#[a-fA-F0-9]{6}");

	public static String matchHexColour(String s) {
		java.util.regex.Matcher matcher = HEX_PATTERN.matcher(s);

		while (matcher.find()) {
			String group = matcher.group(0);

			try {
				TextColor hex = TextColor.color(Integer.parseInt(group.substring(1), 16));

				if (hex != null) {
					s = s.replace(group, hex.asHexString());
				}
			} catch (NumberFormatException e) {
			} catch (Error er) {
				try {
					s = s.replace(group, net.md_5.bungee.api.ChatColor.of(group).toString());
				} catch (IllegalArgumentException e) {
				}
			}
		}

		return s;
	}

	public static String setSymbols(String s) {
		if (s.indexOf('<') == -1) {
			return s;
		}

		int i = -1;

		for (String symbol : Symbols.SYMBOLS) {
			s = s.replace('<' + Integer.toString(++i) + '>', symbol);
		}

		return s;
	}

	public static boolean isValidColourCharacter(char ch) {
		return ((ch >= 'a' && ch <= 'f') || (ch == 'k' || ch == 'l' || ch == 'm' || ch == 'n' || ch == 'o' || ch == 'r'))
				|| Character.isDigit(ch);
	}

	public static void replace(StringBuilder builder, String search, Supplier<String> replacement) {
		int index = builder.indexOf(search);

		if (index == -1) {
			return;
		}

		int searchLength = search.length();

		while ((index = builder.replace(index, index + searchLength, replacement.get()).indexOf(search, 0)) != -1) {
		}
	}

	public static String replace(String text, String search, Supplier<String> replacement) {
		int index = text.indexOf(search);

		if (index == -1) {
			return text;
		}

		StringBuilder builder = new StringBuilder(text);
		int searchLength = search.length();
		int replacementLength = replacement.get().length();

		while ((index = builder.replace(index, index + searchLength, replacement.get()).indexOf(search,
				index += replacementLength)) != -1) {
		}

		return builder.toString();
	}

	public static String replaceFrom(String text, int start, String search, String replacement, int max) {
		int index = start == 0 ? text.indexOf(search) : text.indexOf(search, start);

		if (index == -1) {
			return text;
		}

		StringBuilder builder = new StringBuilder(text);

		while (index != -1) {
			builder.replace(index, index + 1, replacement);

			if (--max <= 0) {
				break;
			}

			index = builder.indexOf(search, start);
		}

		return builder.toString();
	}
}
