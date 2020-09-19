package hu.montlikadani.tablist.bukkit.config;

import static hu.montlikadani.tablist.bukkit.utils.Util.logConsole;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import hu.montlikadani.tablist.bukkit.TabList;

public class Configuration {

	private TabList plugin;

	private FileConfiguration messages, names, groups, fakeplayers, animCreator;
	private CommentedConfig config, tablist;
	private File config_file, messages_file, animation_file, tablist_file, groups_file, names_file, fakeplayers_file;

	public Configuration(TabList plugin) {
		this.plugin = plugin;
	}

	public void loadFiles() {
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

			if (!config_file.exists()) {
				plugin.saveResource("config.yml", false);
			}

			config = new CommentedConfig(config_file);
			ConfigValues.loadValues();

			if (tablist_file == null) {
				tablist_file = new File(folder, "tablist.yml");
			}

			if (!tablist_file.exists()) {
				plugin.saveResource("tablist.yml", false);
			}

			tablist = new CommentedConfig(tablist_file);

			messages = createFile(messages_file, "messages.yml", false);
			messages.save(messages_file);

			animCreator = createFile(animation_file, "animcreator.yml", false);
			animCreator.load(animation_file);

			if (ConfigValues.isTabNameEnabled()) {
				if (names_file == null) {
					names_file = new File(folder, "names.yml");
				}

				names = createFile(names_file, "names.yml", true);
				names.save(names_file);
			}

			if (ConfigValues.isPrefixSuffixEnabled()) {
				if (groups_file == null) {
					groups_file = new File(folder, "groups.yml");
				}

				groups = createFile(groups_file, "groups.yml", false);
				groups.load(groups_file);
			}

			if (ConfigValues.isFakePlayers()) {
				if (fakeplayers_file == null) {
					fakeplayers_file = new File(folder, "fakeplayers.yml");
				}

				fakeplayers = createFile(fakeplayers_file, "fakeplayers.yml", true);
			}
		} catch (Exception e) {
			e.printStackTrace();
			logConsole(Level.WARNING,
					"There was an error. Please report it here:\nhttps://github.com/montlikadani/TabList/issues",
					false);
		}
	}

	public void createNamesFile() {
		if (names_file != null && names_file.exists()) {
			return;
		}

		if (names_file == null) {
			names_file = new File(plugin.getFolder(), "names.yml");
		}

		names = createFile(names_file, "names.yml", true);
	}

	public void createGroupsFile() {
		if (groups_file != null && groups_file.exists()) {
			return;
		}

		if (groups_file == null) {
			groups_file = new File(plugin.getFolder(), "groups.yml");
		}

		groups = createFile(groups_file, "groups.yml", false);
	}

	public void createFakePlayersFile() {
		if (fakeplayers_file != null && fakeplayers_file.exists()) {
			return;
		}

		if (fakeplayers_file == null) {
			fakeplayers_file = new File(plugin.getFolder(), "fakeplayers.yml");
		}

		fakeplayers = createFile(fakeplayers_file, "fakeplayers.yml", true);
	}

	FileConfiguration createFile(File file, String name, boolean newFile) {
		if (!file.exists()) {
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
		}

		return YamlConfiguration.loadConfiguration(file);
	}

	public CommentedConfig getConfig() {
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

	public CommentedConfig getTablist() {
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
