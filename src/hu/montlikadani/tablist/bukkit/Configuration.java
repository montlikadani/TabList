package hu.montlikadani.tablist.bukkit;

import static hu.montlikadani.tablist.bukkit.Util.logConsole;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class Configuration {

	private TabList plugin;

	private FileConfiguration config, messages, names, groups, fakeplayers, animCreator, tablist;
	private File config_file, messages_file, animation_file, tablist_file, groups_file, names_file, fakeplayers_file;

	private int cver = 15;
	private int gver = 5;

	public Configuration(TabList plugin) {
		this.plugin = plugin;
	}

	void loadFiles() {
		try {
			File folder = plugin.getFolder();

			if (config_file == null) {
				config_file = new File(folder, "config.yml");
			}

			if (messages_file == null) {
				messages_file = new File(folder, "messages.yml");
			}

			if (animation_file == null) {
				animation_file = new File(folder, "animcreator.yml");
			}

			if (config_file.exists()) {
				config = YamlConfiguration.loadConfiguration(config_file);
				config.load(config_file);

				if (!config.isSet("config-version") || !config.get("config-version").equals(cver)) {
					logConsole(Level.WARNING, "Found outdated configuration (config.yml)! (Your version: "
							+ config.getInt("config-version") + " | Newest version: " + cver + ")");
				}
			} else {
				config = createFile(config_file, "config.yml", false);
			}

			if (!config.contains("tablist")) {
				if (tablist_file == null) {
					tablist_file = new File(folder, "tablist.yml");
				}

				if (tablist_file.exists()) {
					tablist = YamlConfiguration.loadConfiguration(tablist_file);
					tablist.load(tablist_file);
				} else {
					tablist = createFile(tablist_file, "tablist.yml", false);
				}
			}

			if (messages_file.exists()) {
				messages = YamlConfiguration.loadConfiguration(messages_file);
				messages.load(messages_file);
				messages.save(messages_file);
			} else {
				messages = createFile(messages_file, "messages.yml", false);
			}

			if (animation_file.exists()) {
				animCreator = YamlConfiguration.loadConfiguration(animation_file);
				animCreator.load(animation_file);
			} else {
				animCreator = createFile(animation_file, "animcreator.yml", false);
			}

			if (config.getBoolean("tabname.enable")) {
				if (names_file == null) {
					names_file = new File(folder, "names.yml");
				}

				if (names_file.exists()) {
					names = YamlConfiguration.loadConfiguration(names_file);
					names.load(names_file);
					names.save(names_file);
				} else {
					names = createFile(names_file, "names.yml", true);
				}
			}

			if (config.getBoolean("change-prefix-suffix-in-tablist.enable")) {
				if (groups_file == null) {
					groups_file = new File(folder, "groups.yml");
				}

				if (groups_file.exists()) {
					groups = YamlConfiguration.loadConfiguration(groups_file);
					groups.load(groups_file);

					if (!groups.isSet("config-version") || !groups.get("config-version").equals(gver)) {
						logConsole(Level.WARNING, "Found outdated configuration (groups.yml)! (Your version: "
								+ groups.getInt("config-version") + " | Newest version: " + gver + ")");
					}
				} else {
					groups = createFile(groups_file, "groups.yml", false);
				}
			}

			if (config.getBoolean("enable-fake-players")) {
				if (fakeplayers_file == null) {
					fakeplayers_file = new File(folder, "fakeplayers.yml");
				}

				if (fakeplayers_file.exists()) {
					fakeplayers = YamlConfiguration.loadConfiguration(fakeplayers_file);
					fakeplayers.load(fakeplayers_file);
				} else {
					fakeplayers = createFile(fakeplayers_file, "fakeplayers.yml", true);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			logConsole(Level.WARNING,
					"There was an error. Please report it here:\nhttps://github.com/montlikadani/TabList/issues", false);
		}
	}

	void createAnimFile() {
		if (animation_file != null && animation_file.exists()) {
			return;
		}

		if (animation_file == null) {
			animation_file = new File(plugin.getFolder(), "animcreator.yml");
		}

		if (!animation_file.exists()) {
			animCreator = createFile(animation_file, "animcreator.yml", false);
		}
	}

	void createNamesFile() {
		if (names_file != null && names_file.exists()) {
			return;
		}

		if (names_file == null) {
			names_file = new File(plugin.getFolder(), "names.yml");
		}

		if (!names_file.exists()) {
			names = createFile(names_file, "names.yml", true);
		}
	}

	void createGroupsFile() {
		if (groups_file != null && groups_file.exists()) {
			return;
		}

		if (groups_file == null) {
			groups_file = new File(plugin.getFolder(), "groups.yml");
		}

		if (groups_file.exists()) {
			groups = createFile(groups_file, "groups.yml", false);
		}
	}

	void createFakePlayersFile() {
		if (fakeplayers_file != null && fakeplayers_file.exists()) {
			return;
		}

		if (fakeplayers_file == null) {
			fakeplayers_file = new File(plugin.getFolder(), "fakeplayers.yml");
		}

		if (!fakeplayers_file.exists()) {
			fakeplayers = createFile(fakeplayers_file, "fakeplayers.yml", true);
		}
	}

	FileConfiguration createFile(File file, String name, boolean newFile) {
		if (newFile) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			plugin.saveResource(name, false);
		}

		logConsole(name + " file created!", false);
		return YamlConfiguration.loadConfiguration(file);
	}

	public FileConfiguration getConfig() {
		return config;
	}

	public FileConfiguration getMessages() {
		return messages;
	}

	public FileConfiguration getNames() {
		return names;
	}

	public FileConfiguration getGroups() {
		return groups;
	}

	public FileConfiguration getFakeplayers() {
		return fakeplayers;
	}

	public FileConfiguration getAnimCreator() {
		return animCreator;
	}

	public FileConfiguration getTablist() {
		return tablist;
	}

	public File getConfigFile() {
		return config_file;
	}

	public File getMessagesFile() {
		return messages_file;
	}

	public File getAnimationFile() {
		return animation_file;
	}

	public File getTablistFile() {
		return tablist_file;
	}

	public File getGroupsFile() {
		return groups_file;
	}

	public File getNamesFile() {
		return names_file;
	}

	public File getFakeplayersFile() {
		return fakeplayers_file;
	}
}
