package hu.montlikadani.tablist.config.constantsLoader;

import hu.montlikadani.tablist.TabList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hu.montlikadani.tablist.utils.Pair;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import hu.montlikadani.tablist.tablist.TabText;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

public final class TabConfigValues {

	private static boolean enabled, rememberToggledTablistToFile, hideTabWhenPlayerVanished, random;

	private static int updateInterval;

	private static List<String> disabledWorlds, blackListedPlayers;

	private static TabText[] defaultHeader, defaultFooter;

	public static final Map<String, OptionSeparator> SEPARATOR_MAP = new HashMap<>();
	public static final Map<Permission, Pair<TabText[], TabText[]>> PERMISSION_MAP = new HashMap<>();

	public static void loadValues(TabList tl, FileConfiguration c) {
		SEPARATOR_MAP.clear();
		PERMISSION_MAP.clear();

		enabled = c.getBoolean("enabled", true);
		rememberToggledTablistToFile = c.getBoolean("remember-toggled-tablist-to-file", true);
		hideTabWhenPlayerVanished = c.getBoolean("hide-tab-when-player-vanished", false);
		random = c.getBoolean("random", false);
		disabledWorlds = c.getStringList("disabled-worlds");
		blackListedPlayers = c.getStringList("blacklisted-players");

		if ((updateInterval = c.getInt("interval", 4)) > 10000) {
			updateInterval = 10000;
		}

		defaultHeader = objectToArrayConversion(tl, c.get("header", null));
		defaultFooter = objectToArrayConversion(tl, c.get("footer", null));

		ConfigurationSection section = c.getConfigurationSection("per-world");
		if (section != null) {
			for (String one : section.getKeys(false)) {
				String[] split = one.split(", ");

				if (split.length == 0) {
					SEPARATOR_MAP.put(one, new OptionSeparator(tl, one, one, section, true));
				} else {
					for (String worldName : split) {
						SEPARATOR_MAP.put(worldName, new OptionSeparator(tl, one, worldName, section, true));
					}
				}
			}
		}

		if ((section = c.getConfigurationSection("permissions")) != null) {
			for (String key : section.getKeys(false)) {
				Object header = section.get(key + ".header", null);
				Object footer = section.get(key + ".footer", null);

				if (header == null && footer == null) {
					continue;
				}

				String permission = section.getString(key + ".name", "tablist.permissionName");
				Permission perm = Bukkit.getServer().getPluginManager().getPermission(permission);

				// Permission should be added before we check for the player,
				// because operator players have all permissions by default
				if (perm != null) {

					// Set and recalculate existing permission
					perm.setDefault(PermissionDefault.FALSE);
				} else {
					perm = new Permission(permission, PermissionDefault.NOT_OP);
					Bukkit.getServer().getPluginManager().addPermission(perm);
				}

				PERMISSION_MAP.put(perm, new Pair<>(objectToArrayConversion(tl, header), objectToArrayConversion(tl, footer)));
			}
		}

		if ((section = c.getConfigurationSection("per-player")) != null) {
			for (String key : section.getKeys(false)) {
				String[] split = key.split(", ");

				if (split.length == 0) {
					SEPARATOR_MAP.put(key, new OptionSeparator(tl, key, key, section, false));
				} else {
					for (String playerName : split) {
						SEPARATOR_MAP.put(playerName, new OptionSeparator(tl, key, playerName, section, false));
					}
				}
			}
		}

		if ((section = c.getConfigurationSection("per-group")) != null) {
			for (String key : section.getKeys(false)) {
				String[] split = key.split(", ");

				if (split.length == 0) {
					SEPARATOR_MAP.put(key, new OptionSeparator(tl, key, key, section, false));
				} else {
					for (String groupName : split) {
						SEPARATOR_MAP.put(groupName, new OptionSeparator(tl, key, groupName, section, false));
					}
				}
			}
		}
	}

	private static TabText[] objectToArrayConversion(TabList tl, Object obj) {
		if (obj instanceof List) {
			String[] array = ((List<?>) obj).toArray(new String[0]);
			TabText[] tt = new TabText[array.length];

			for (int i = 0; i < array.length; i++) {
				TabText text = new TabText();
				text.setPlainText(tl.getPlaceholders().replaceMiscVariables(array[i]));
				tt[i] = text;
			}

			return tt;
		}

		if (obj instanceof String) {
			TabText text = new TabText();
			text.setPlainText(tl.getPlaceholders().replaceMiscVariables((String) obj));

			return new TabText[] { text };
		}

		return null;
	}

	public static final class OptionSeparator {

		private Map<String, Pair<TabText[], TabText[]>> configKeyMap;
		public Pair<TabText[], TabText[]> pair;

		OptionSeparator(TabList tl, String configPath, String name, ConfigurationSection section, boolean extraContent) {
			TabText[] key = objectToArrayConversion(tl, section.get(configPath + ".header", null));
			TabText[] value = objectToArrayConversion(tl, section.get(configPath + ".footer", null));

			if (key != null || value != null) {
				pair = new Pair<>(key, value);
			}

			if (!extraContent) {
				return;
			}

			(configKeyMap = new HashMap<>(1)).put(name, pair);

			readFromConfig(tl, section.getConfigurationSection(configPath + ".per-player"));
			readFromConfig(tl, section.getConfigurationSection(configPath + ".per-group"));
		}

		public Map<String, Pair<TabText[], TabText[]>> getConfigKeyMap() {
			return configKeyMap;
		}

		private void readFromConfig(TabList tl, ConfigurationSection configSection) {
			if (configSection == null) {
				return;
			}

			for (String one : configSection.getKeys(false)) {
				String[] split = one.split(", ");

				if (split.length == 0) {
					configKeyMap.put(one, new Pair<>(objectToArrayConversion(tl, configSection.get(one + ".header", null)),
							objectToArrayConversion(tl, configSection.get(one + ".footer", null))));
				} else {
					for (String name : split) {
						configKeyMap.put(name, new Pair<>(objectToArrayConversion(tl, configSection.get(one + ".header", null)),
								objectToArrayConversion(tl, configSection.get(one + ".footer", null))));
					}
				}
			}
		}
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static boolean isRememberToggledTablistToFile() {
		return rememberToggledTablistToFile;
	}

	public static boolean isHideTabWhenPlayerVanished() {
		return hideTabWhenPlayerVanished;
	}

	public static boolean isRandom() {
		return random;
	}

	public static int getUpdateInterval() {
		return updateInterval;
	}

	public static List<String> getDisabledWorlds() {
		return disabledWorlds;
	}

	public static List<String> getBlackListedPlayers() {
		return blackListedPlayers;
	}

	public static TabText[] getDefaultHeader() {
		return defaultHeader;
	}

	public static TabText[] getDefaultFooter() {
		return defaultFooter;
	}
}
