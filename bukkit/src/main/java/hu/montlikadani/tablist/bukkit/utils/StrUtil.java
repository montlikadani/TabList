package hu.montlikadani.tablist.bukkit.utils;

public final class StrUtil {

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
