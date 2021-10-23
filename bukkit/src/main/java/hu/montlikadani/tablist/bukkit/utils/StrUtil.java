package hu.montlikadani.tablist.bukkit.utils;

import java.util.regex.Pattern;

public final class StrUtil {

	private static final Pattern COMMA_SPACE_SEPARATED_PATTERN = Pattern.compile(", ");
	private static final Pattern NUMBER_ESCAPE_SEQUENCE = Pattern.compile("[^\\d]");

	public static Pattern getCommaSpaceSeparatedPattern() {
		return COMMA_SPACE_SEPARATED_PATTERN;
	}

	public static Pattern getNumberEscapeSequence() {
		return NUMBER_ESCAPE_SEQUENCE;
	}

	public static String replaceNextChar(String text, int start, String search, String replacement, int max) {
		int index = text.indexOf(search, start);

		if (index == -1) {
			return text;
		}

		StringBuilder builder = new StringBuilder(text);

		while (index != -1) {
			builder = builder.replace(index, index + 1, replacement);

			if (--max <= 0) {
				break;
			}

			index = builder.indexOf(search, start);
		}

		return builder.toString();
	}
}
