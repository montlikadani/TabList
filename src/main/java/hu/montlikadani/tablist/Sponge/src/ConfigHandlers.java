package hu.montlikadani.tablist.Sponge.src;

import java.nio.file.Path;

import org.spongepowered.api.Sponge;

public class ConfigHandlers {

	private TabList plugin;
	private String name;

	private Path path;
	private ConfigManager config;

	public ConfigHandlers(TabList plugin, String name) {
		this.plugin = plugin;
		this.name = name;
	}

	public void createFile() {
		path = Sponge.getGame().getConfigManager().getPluginConfig(plugin).getDirectory();
		config = new ConfigManager(path.toString(), name);
		config.createFile();
	}

	public void reload() {
		if (!isExists()) {
			createFile();
		} else {
			config.load();
			config.save();
		}
	}

	public boolean isExists() {
		return config != null && config.getFile().exists();
	}

	public Path getPath() {
		return path;
	}

	public ConfigManager getConfig() {
		return config;
	}
}
