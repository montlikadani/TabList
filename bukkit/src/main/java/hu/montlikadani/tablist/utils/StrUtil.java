package hu.montlikadani.tablist.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StrUtil {

	private static final Pattern COMMA_SPACE_SEPARATED_PATTERN = Pattern.compile(", ");
	private static final Matcher NUMBER_ESCAPE_SEQUENCE = Pattern.compile("[^\\d]").matcher("");

	public static Pattern getCommaSpaceSeparatedPattern() {
		return COMMA_SPACE_SEPARATED_PATTERN;
	}

	public static Matcher getNumberEscapeSequence() {
		return NUMBER_ESCAPE_SEQUENCE;
	}
}
