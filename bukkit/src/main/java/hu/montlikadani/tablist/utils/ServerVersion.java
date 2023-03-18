package hu.montlikadani.tablist.utils;

public enum ServerVersion {
	v1_8_R1,
	v1_8_R2,
	v1_8_R3,
	v1_9_R1,
	v1_9_R2,
	v1_10_R1,
	v1_11_R1,
	v1_12_R1,
	v1_13_R1,
	v1_13_R2,
	v1_14_R1,
	v1_14_R2,
	v1_15_R1,
	v1_15_R2,
	v1_16_R1,
	v1_16_R2,
	v1_16_R3,
	v1_17_R1,
	v1_17_R2,
	v1_18_R1,
	v1_18_R2,
	v1_19_R1,
	v1_19_R2,
	v1_19_R3;

	private final int value;

	private static String[] arrayVersion;
	private static ServerVersion current;

	ServerVersion() {
		value = Integer.parseInt(StrUtil.getNumberEscapeSequence().reset(name()).replaceAll(""));
	}

	public static ServerVersion getCurrent() {
		if (current != null)
			return current;

		String[] version = getArrayVersion();
		String last = version[version.length - 1];

		for (ServerVersion one : values()) {
			if (one.name().equalsIgnoreCase(last)) {
				current = one;
				break;
			}
		}

		return current;
	}

	public static String[] getArrayVersion() {
		if (arrayVersion == null) {
			arrayVersion = org.bukkit.Bukkit.getServer().getClass().getPackage().getName().split("\\.");
		}

		return arrayVersion;
	}

	public static boolean isCurrentEqualOrHigher(ServerVersion v) {
		return getCurrent().value >= v.value;
	}

	public static boolean isCurrentLower(ServerVersion v) {
		return getCurrent().value < v.value;
	}

	public static boolean isCurrentEqualOrLower(ServerVersion v) {
		return getCurrent().value <= v.value;
	}
}
