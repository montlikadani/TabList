package hu.montlikadani.tablist.config;

import java.io.File;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.config.constantsLoader.ConfigValues;
import hu.montlikadani.tablist.config.constantsLoader.TabConfigValues;

public class Configuration {

	private transient TabList plugin;

	private transient FileConfiguration tablist, groups, fakePlayers, animCreator;
	private transient CommentedConfig config;
	private transient ConfigMessages messages;
	private File configFile, animationFile, tablistFile, groupsFile, fakePlayersFile;

	public Configuration(TabList plugin) {
		this.plugin = plugin;

		File folder = plugin.getFolder();
		messages = new ConfigMessages(folder);

		configFile = new File(folder, "config.yml");
		animationFile = new File(folder, "animcreator.yml");
		tablistFile = new File(folder, "tablist.yml");
		groupsFile = new File(folder, "groups.yml");
		fakePlayersFile = new File(folder, "fakeplayers.yml");
	}

	public void loadFiles() {
		// Monument
		File names = new File(plugin.getFolder(), "names.yml");
		if (names.exists()) {
			names.delete();
		}

		if (!configFile.exists()) {
			plugin.saveResource(configFile.getName(), false);
		}

		config = new CommentedConfig(configFile);
		tablist = createFile(tablistFile, tablistFile.getName(), false);

		ConfigValues.loadValues(config);
		TabConfigValues.loadValues(tablist);
		messages.createAndLoad();

		animCreator = createFile(animationFile, animationFile.getName(), false);

		if (ConfigValues.isPrefixSuffixEnabled()) {
			groups = createFile(groupsFile, groupsFile.getName(), false);
		}

		if (ConfigValues.isFakePlayers()) {
			fakePlayers = createFile(fakePlayersFile, fakePlayersFile.getName(), true);
		}
	}

	FileConfiguration createFile(File file, String name, boolean newFile) {
		if (!file.exists()) {
			if (newFile) {
				try {
					file.createNewFile();
				} catch (java.io.IOException e) {
					e.printStackTrace();
				}
			} else {
				plugin.saveResource(name, false);
			}
		}

		return YamlConfiguration.loadConfiguration(file);
	}

	public void deleteEmptyFiles() {
		if (fakePlayersFile.length() == 0L) {
			fakePlayersFile.delete();
		}
	}

	public CommentedConfig getConfig() {
		return config;
	}

	public ConfigMessages getMessages() {
		return messages;
	}

	public FileConfiguration getGroups() {
		if (groups == null || !groupsFile.exists()) {
			groups = createFile(groupsFile, groupsFile.getName(), false);
		}

		return groups;
	}

	public FileConfiguration getFakeplayers() {
		if (fakePlayers == null || !fakePlayersFile.exists()) {
			fakePlayers = createFile(fakePlayersFile, fakePlayersFile.getName(), true);
		}

		return fakePlayers;
	}

	public FileConfiguration getAnimCreator() {
		return animCreator;
	}

	public FileConfiguration getTablist() {
		return tablist;
	}

	public File getAnimationFile() {
		return animationFile;
	}

	public File getTablistFile() {
		return tablistFile;
	}

	public File getGroupsFile() {
		return groupsFile;
	}

	public File getFakeplayersFile() {
		return fakePlayersFile;
	}
}
