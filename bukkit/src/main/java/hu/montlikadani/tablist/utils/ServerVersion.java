package hu.montlikadani.tablist.utils;

public enum ServerVersion {

	v1_20_2, v1_20_1, v1_20,
	v1_19_4, v1_19_3, v1_19_2, v1_19_1, v1_19,
	v1_18_2, v1_18_1, v1_18,
	v1_17_1, v1_17,
	v1_16_5, v1_16_4, v1_16_3, v1_16_2, v1_16_1, v1_16,
	v1_15_2, v1_15_1, v1_15,
	v1_14_4, v1_14_3, v1_14_2, v1_14_1, v1_14,
	v1_13_2, v1_13_1, v1_13,
	v1_12_2, v1_12_1, v1_12,
	v1_11_2, v1_11_1, v1_11,
	v1_10_2, v1_10_1, v1_10,
	v1_9_4, v1_9_3, v1_9_2, v1_9_1, v1_9,
	v1_8_9, v1_8_8, v1_8_7, v1_8_6, v1_8_5, v1_8_4, v1_8_3, v1_8_2, v1_8_1, v1_8;

	private static ServerVersion current;

	public static ServerVersion getCurrent() {
		if (current != null)
			return current;

		String name = 'v' + org.bukkit.Bukkit.getServer().getBukkitVersion().split("-", 2)[0].replace('.', '_');

		for (ServerVersion one : values()) {
			if (one.name().equals(name)) {
				return current = one;
			}
		}

		return null;
	}
}
