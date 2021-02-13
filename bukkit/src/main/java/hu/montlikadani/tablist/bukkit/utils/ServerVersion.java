package hu.montlikadani.tablist.bukkit.utils;

public class ServerVersion {

	public Version getVersion() {
		return Version.getCurrent();
	}

	public enum Version {
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

		private Integer value;
		private String shortVersion;

		private static String[] arrayVersion;
		private static Version current;

		Version() {
			value = Integer.valueOf(name().replaceAll("[^\\d.]", ""));
			shortVersion = name().substring(0, name().length() - 3);
		}

		public Integer getValue() {
			return value;
		}

		public String getShortVersion() {
			return shortVersion;
		}

		public static Version getCurrent() {
			if (current != null)
				return current;

			String[] v = getArrayVersion();
			String vv = v[v.length - 1];
			for (Version one : values()) {
				if (one.name().equalsIgnoreCase(vv)) {
					current = one;
					break;
				}
			}

			if (current == null) { // It is possible when immo not added a new version enum
				current = Version.v1_16_R3;
			}

			return current;
		}

		public static String[] getArrayVersion() {
			if (arrayVersion == null) {
				arrayVersion = org.bukkit.Bukkit.getServer().getClass().getPackage().getName().split("\\.");
			}

			return arrayVersion;
		}

		public boolean isLower(Version version) {
			return getValue() < version.getValue();
		}

		public boolean isHigher(Version version) {
			return getValue() > version.getValue();
		}

		public boolean isEqual(Version version) {
			return getValue().equals(version.getValue());
		}

		public boolean isEqualOrLower(Version version) {
			return getValue() <= version.getValue();
		}

		public boolean isEqualOrHigher(Version version) {
			return getValue() >= version.getValue();
		}

		public static boolean isCurrentEqualOrHigher(Version v) {
			return getCurrent().getValue() >= v.getValue();
		}

		public static boolean isCurrentHigher(Version v) {
			return getCurrent().getValue() > v.getValue();
		}

		public static boolean isCurrentLower(Version v) {
			return getCurrent().getValue() < v.getValue();
		}

		public static boolean isCurrentEqualOrLower(Version v) {
			return getCurrent().getValue() <= v.getValue();
		}

		public static boolean isCurrentEqual(Version v) {
			return getCurrent().getValue().equals(v.getValue());
		}
	}
}
