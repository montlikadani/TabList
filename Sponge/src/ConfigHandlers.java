package hu.montlikadani.tablist.sponge;

import java.nio.file.Path;

import org.spongepowered.api.Sponge;

public class ConfigHandlers {

	private TabList plugin;
	private String name;
	private boolean setMissing;

	private Path path;
	private ConfigManager config;

	public ConfigHandlers(TabList plugin, String name, boolean setMissing) {
		this.plugin = plugin;
		this.name = name;
		this.setMissing = setMissing;
	}

	public void createFile() {
		path = Sponge.getGame().getConfigManager().getPluginConfig(plugin).getDirectory();
		config = new ConfigManager(path.toString(), name);
		config.createFile();
	}

	public void reload() {
		if (!isExists()) {
			createFile();
		}

		config.load();
		config.save();
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

	public String getName() {
		return name;
	}

	public boolean isSetMissing() {
		return setMissing;
	}
}
