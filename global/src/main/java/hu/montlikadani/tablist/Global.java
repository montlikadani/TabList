package hu.montlikadani.tablist;

import java.util.function.Supplier;

public final class Global {

	private Global() {
	}

	public static String setSymbols(String s) {
		if (s.indexOf('<') == -1) {
			return s;
		}

		int i = -1;

		for (String symbol : new String[] { "\u2022", "\u27A4", "\u2122", "\u2191", "\u2192", "\u2193", "\u221E", "\u2591", "\u25B2", "\u25B6", "\u25C0", "\u25CF", "\u2605",
				"\u2606", "\u2610", "\u2611", "\u2620", "\u2622", "\u2623", "\u2639", "\u263A", "\u2713", "\u2714", "\u2718", "\u271A", "\u213B", "\u2720", "\u2721", "\u2726",
				"\u2727", "\u2729", "\u272A", "\u272E", "\u272F", "\u32E1", "\u275D", "\u275E", "\u30C4", "\u2669", "\u266A", "\u266B", "\u266C", "\u266D", "\u266E", "\u266F",
				"\u00B6", "\u00A9", "\u00AE", "\u23CE", "\u21E7", "\u21EA", "\u1D34\u1D30", "\u2612", "\u2660", "\u2663", "\u263B", "\u2593", "\u27BE", "\u2794", "\u27B3",
				"\u27A7", "\u300A", "\u300B", "\uFE3E", "\uFE3D", "\u2603", "\u00B9", "\u00B2", "\u00B3", "\u2248", "\u2120", "\u2665", "\u272C", "\u2194", "\u00AB", "\u00BB",
				"\u2600", "\u2666", "\u20BD", "\u260E", "\u2602", "\u2190", "\u2196", "\u2197", "\u2198", "\u2199", "\u27B2", "\u2710", "\u270E", "\u270F", "\u2706", "\u25C4",
				"\u263C", "\u25BA", "\u2195", "\u25BC", "\u2460", "\u2461", "\u2462", "\u2463", "\u2464", "\u2465", "\u2466", "\u2467", "\u2468", "\u2469", "\u246A", "\u246B",
				"\u246C", "\u246D", "\u246E", "\u246F", "\u2470", "\u2471", "\u2472", "\u2473", "\u2668", "\u2711", "\u2716", "\u2730", "\u2736", "\u2557", "\u2563", "\u25D9",
				"\u25CB", "\u2560", "\u2524", "\u2551", "\u255D", "\u2302", "\u2510", "\u2749", "\u2332", "\u00BD", "\u00BC", "\u00BE", "\u2153", "\u2154", "\u2116", "\u2020",
				"\u2021", "\u00B5", "\u00A2", "\u00A3", "\u2205", "\u2264", "\u2265", "\u2260", "\u2227", "\u2228", "\u2229", "\u222A", "\u2208", "\u2200", "\u2203", "\u2204",
				"\u2211", "\u220F", "\u21BA", "\u21BB", "\u03A9" }) {
			s = s.replace('<' + Integer.toString(++i) + '>', symbol);
		}

		return s;
	}

	public static boolean isValidColourCharacter(char ch) {
		return ((ch >= 'a' && ch <= 'f') || (ch == 'k' || ch == 'l' || ch == 'm' || ch == 'n' || ch == 'o' || ch == 'r')) || Character.isDigit(ch);
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

		while ((index = builder.replace(index, index + searchLength, replacement.get()).indexOf(search, index += replacementLength)) != -1) {
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
				return builder.toString();
			}

			index = builder.indexOf(search, start);
		}

		return builder.toString();
	}
}
