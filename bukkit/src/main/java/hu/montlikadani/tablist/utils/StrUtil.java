package hu.montlikadani.tablist.utils;

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
}
