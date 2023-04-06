package hu.montlikadani.tablist.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StrUtil {

	private static final Matcher NUMBER_ESCAPE_SEQUENCE = Pattern.compile("[^\\d]").matcher("");

	public static Matcher getNumberEscapeSequence() {
		return NUMBER_ESCAPE_SEQUENCE;
	}
}
