package hu.montlikadani.tablist;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.serialize.SerializationException;

import com.google.inject.Inject;

public class ConfigManager {

	private String path;
	private String name;
	private File file;

	@Inject
	@DefaultConfig(sharedRoot = true)
	private Path configPath;

	@Inject
	private CommentedConfigurationNode node;

	@Inject
	@DefaultConfig(sharedRoot = true)
	private ConfigurationLoader<CommentedConfigurationNode> loader;

	public ConfigManager(String path, String name) {
		this.path = path;
		this.name = name;

		File folder = new File(path);
		if (!folder.exists()) {
			folder.mkdirs();
		}

		this.file = new File(folder, name);
		this.loader = HoconConfigurationLoader.builder().file(file)
				.defaultOptions(ConfigurationOptions.defaults().shouldCopyDefaults(true)).build();
	}

	public String getPath() {
		return path;
	}

	public String getName() {
		return name;
	}

	public File getFile() {
		return file;
	}

	@Inject
	public Path getConfigPath() {
		return configPath;
	}

	@Inject
	public CommentedConfigurationNode getConfigNode() {
		return node;
	}

	@Inject
	public ConfigurationLoader<CommentedConfigurationNode> getConfigLoader() {
		return loader;
	}

	public void createFile() {
		if (file.exists()) {
			return;
		}

		try (InputStream in = TabList.get().getClass().getClassLoader().getResourceAsStream(name)) {
			if (in != null) {
				Files.copy(in, file.toPath());
				in.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public CommentedConfigurationNode get(Object... path) {
		return node.node(path);
	}

	public void set(Object value, Object... path) {
		if (!contains(path) && TabList.get().getC().isSetMissing()) {
			try {
				get(path).set(Object.class, value);
			} catch (SerializationException e) {
				e.printStackTrace();
			}
		}
	}

	public void setComment(String comment, Object... path) {
		if (comment != null && !comment.trim().isEmpty()) {
			get(path).comment(comment);
		}
	}

	public boolean getBoolean(Object... path) {
		return get(path).getBoolean();
	}

	public boolean getBoolean(Boolean defValue, Object... path) {
		set(defValue, path);
		return get(path).getBoolean(defValue);
	}

	public int getInt(Object... path) {
		return get(path).getInt();
	}

	public int getInt(Integer defValue, Object... path) {
		set(defValue, path);
		return get(path).getInt(defValue);
	}

	public String getString(Object... path) {
		return get(path).getString();
	}

	public String getString(String defValue, Object... path) {
		set(defValue, path);
		return get(path).getString(defValue);
	}

	public List<String> getStringList(Object... path) {
		return getStringList(null, path);
	}

	public List<String> getStringList(List<String> def, Object... path) {
		try {
			if (def == null) {
				return get(path).getList(String.class);
			}

			return get(path).getList(String.class, def);
		} catch (SerializationException e) {
			e.printStackTrace();
		}

		return new ArrayList<>();
	}

	public boolean contains(Object... path) {
		return !get(path).virtual();
	}

	public boolean isExistsAndNotEmpty(Object... path) {
		return contains(path) && !get(path).getString().isEmpty();
	}

	public boolean isString(Object... path) {
		Object val = get(path).raw();
		return val instanceof String;
	}

	public boolean isList(Object... path) {
		Object val = get(path).raw();
		return val instanceof List;
	}

	public void save() {
		try {
			loader.save(node);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void load() {
		try {
			node = loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
