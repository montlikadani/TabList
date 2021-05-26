package hu.montlikadani.tablist.bukkit.utils;

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
	v1_18_R2;

	private int value;

	private static String[] arrayVersion;
	private static ServerVersion current;

	ServerVersion() {
		value = Integer.valueOf(name().replaceAll("[^\\d.]", ""));
	}

	public int getValue() {
		return value;
	}

	public static ServerVersion getCurrent() {
		if (current != null)
			return current;

		String[] v = getArrayVersion();
		String vv = v[v.length - 1];
		for (ServerVersion one : values()) {
			if (one.name().equalsIgnoreCase(vv)) {
				current = one;
				break;
			}
		}

		if (current == null) { // It is possible when immo not added a new version enum
			current = ServerVersion.v1_16_R3;
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
		return getCurrent().getValue() >= v.getValue();
	}

	public static boolean isCurrentHigher(ServerVersion v) {
		return getCurrent().getValue() > v.getValue();
	}

	public static boolean isCurrentLower(ServerVersion v) {
		return getCurrent().getValue() < v.getValue();
	}

	public static boolean isCurrentEqualOrLower(ServerVersion v) {
		return getCurrent().getValue() <= v.getValue();
	}

	public static boolean isCurrentEqual(ServerVersion v) {
		return getCurrent().getValue() == v.getValue();
	}
}
