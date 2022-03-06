package hu.montlikadani.tablist.config;

import java.nio.file.Path;
import java.util.function.Supplier;

import org.spongepowered.api.Sponge;

import hu.montlikadani.tablist.TabList;

public class ConfigHandlers implements Supplier<ConfigManager> {

	private final TabList tl;
	private final String name;
	private final boolean setMissing;

	private Path path;
	private ConfigManager config;

	public ConfigHandlers(TabList tl, String name, boolean setMissing) {
		this.tl = tl;
		this.name = name;
		this.setMissing = setMissing;
	}

	@Override
	public ConfigManager get() {
		return config;
	}

	public boolean isExists() {
		return config != null && config.getFile().exists();
	}

	public Path getPath() {
		return path;
	}

	public String getName() {
		return name;
	}

	public boolean isSetMissing() {
		return setMissing;
	}

	private void createFile() {
		path = Sponge.configManager().pluginConfig(tl.getPluginContainer()).directory();
		config = new ConfigManager(path.toString(), name, setMissing);
		config.createFile();
	}

	public void reload() {
		if (!isExists()) {
			createFile();
		}

		config.load();
	}
}
