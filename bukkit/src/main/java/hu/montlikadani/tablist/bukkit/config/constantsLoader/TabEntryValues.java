package hu.montlikadani.tablist.bukkit.config.constantsLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.configuration.ConfigurationSection;

import hu.montlikadani.tablist.bukkit.config.CommentedConfig;
import hu.montlikadani.tablist.bukkit.utils.Util;

public final class TabEntryValues {

	private static boolean enabled = false;

	private static int columns;
	private static long refreshRate;

	public static final Map<ColumnValues.ConfigType, List<ColumnValues>> COLUMN_SECTION = new HashMap<>();
	public static final Map<PlaceholderSetting.SettingType, PlaceholderSetting> VARIABLE_SETTINGS = new HashMap<>();

	public static void loadValues(CommentedConfig c) {
		if (!(enabled = c.getBoolean("enabled", false))) {
			return;
		}

		columns = c.getInt("columns-size", 4);
		if (columns > 4 || columns < 1) {
			columns = 4;
		}

		refreshRate = c.getLong("refresh-rate", 6L);
		if (refreshRate < 1) {
			refreshRate = 6L;
		}

		for (PlaceholderSetting.SettingType type : PlaceholderSetting.SettingType.values()) {
			int max = c.getInt("placeholder-setting." + type.path + ".max", 8);
			if (max >= 20) {
				max = 19;
			}

			boolean showAfkPlayers = c.getBoolean("placeholder-setting." + type.path + ".show-afk-players", true);

			VARIABLE_SETTINGS.put(type, new PlaceholderSetting(max, showAfkPlayers));
		}

		for (ColumnValues.ConfigType type : ColumnValues.ConfigType.values()) {
			List<ColumnValues> columnValues = new ArrayList<>();

			if (type == ColumnValues.ConfigType.DEFAULT) {
				String text = c.getString(type.path + "text", " ");
				UUID head = Util.tryParseId(c.getString(type.path + "head"))
						.orElse(UUID.fromString("7f2fa3f7-b400-4c08-b816-65d2de5d9010"));
				int ping = Util.tryParse(c.getString(type.path + "ping", "1000")).orElse(1000);

				columnValues.add(new ColumnValues(text, head, ping, 0));
			} else if (type == ColumnValues.ConfigType.NORMAL) {
				ConfigurationSection section = c.getConfigurationSection(type.path);
				if (section != null) {
					for (String col : section.getKeys(false)) {
						int column = Util.tryParse(col).orElse(0);
						if (column <= 0 || column > 4) {
							continue;
						}

						for (String rowContext : section.getStringList(col)) {
							String text = "";
							UUID headId = null;
							int ping = 1000;

							String[] split = rowContext.split(";");
							if (split.length == 0) {
								if (rowContext.contains("ping:")) {
									ping = Util.tryParse(rowContext.replace("ping:", "")).orElse(1000);
								} else if (rowContext.contains("head:")) {
									headId = Util.tryParseId(rowContext.replace("head:", "")).orElse(null);
								} else {
									text = rowContext;
								}
							} else {
								for (String one : split) {
									if (one.contains("ping:")) {
										ping = Util.tryParse(one.replace("ping:", "")).orElse(1000);
									} else if (one.contains("head:")) {
										headId = Util.tryParseId(one.replace("head:", "")).orElse(null);
									} else {
										text = one;
									}
								}
							}

							columnValues.add(new ColumnValues(text, headId, ping, column));
						}
					}
				}
			}

			COLUMN_SECTION.put(type, columnValues);
		}
	}

	public static final class ColumnValues {

		private UUID headId;
		private String text;
		private int ping, column;

		private ColumnValues(String text, UUID headId, int ping, int column) {
			this.text = text;
			this.headId = headId;
			this.ping = ping;
			this.column = column;
		}

		public String getText() {
			return text;
		}

		public UUID getHeadId() {
			return headId;
		}

		public int getPing() {
			return ping;
		}

		public int getColumn() {
			return column;
		}

		public enum ConfigType {
			DEFAULT("default."), NORMAL("columns");

			public final String path;

			ConfigType(String path) {
				this.path = path;
			}
		}
	}

	public static final class PlaceholderSetting {

		private int max;
		private boolean showAfkPlayers;

		private PlaceholderSetting(int max, boolean showAfkPlayers) {
			this.max = max;
			this.showAfkPlayers = showAfkPlayers;
		}

		public int getMax() {
			return max;
		}

		public boolean isShowAfkPlayers() {
			return showAfkPlayers;
		}

		public enum SettingType {
			WORLD_PLAYERS("world_players"), ONLINE_PLAYERS("players"), PLAYERS_IN_GROUP("players_in_group");

			public final String path;

			SettingType(String path) {
				this.path = path;
			}
		}
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static int getColumns() {
		return columns;
	}

	public static long getRefreshRate() {
		return refreshRate;
	}
}
