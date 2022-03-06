package hu.montlikadani.tablist.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.spongepowered.api.config.DefaultConfig;

import com.google.common.reflect.TypeToken;

import hu.montlikadani.tablist.TabList;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;

public class ConfigManager {

	private final String path;
	private final String name;
	private final File file;
	private final boolean setMissing;

	private CommentedConfigurationNode node;

	@com.google.inject.Inject
	@DefaultConfig(sharedRoot = true)
	private ConfigurationLoader<CommentedConfigurationNode> loader;

	public ConfigManager(String path, String name, boolean setMissing) {
		this.path = path;
		this.name = name;
		this.setMissing = setMissing;

		File folder = new File(path);
		folder.mkdirs();

		this.file = new File(folder, name);
		this.loader = HoconConfigurationLoader.builder().setFile(file)
				.setDefaultOptions(ConfigurationOptions.defaults()).build();
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

	public CommentedConfigurationNode getCommentedConfigNode() {
		return node;
	}

	public ConfigurationLoader<CommentedConfigurationNode> getConfigLoader() {
		return loader;
	}

	public void createFile() {
		if (file.exists()) {
			return;
		}

		try (InputStream in = TabList.class.getClassLoader().getResourceAsStream(name)) {
			Files.copy(in, file.toPath());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public CommentedConfigurationNode getNode(Object... path) {
		return node.getNode(path);
	}

	public void set(ConfigurationNode node, Object value) {
		if (setMissing && !contains(node)) {
			node.setValue(value);
		}
	}

	public void setComment(CommentedConfigurationNode node, String comment) {
		if (comment != null && !comment.isEmpty()) {
			node.setComment(comment);
		}
	}

	public boolean getBoolean(ConfigurationNode node, boolean def) {
		set(node, def);
		return node.getBoolean(def);
	}

	public int getInt(ConfigurationNode node, int def) {
		set(node, def);
		return node.getInt(def);
	}

	public String getString(ConfigurationNode node, String def) {
		set(node, def);
		return node.getString(def);
	}

	public List<String> getAsList(ConfigurationNode node, List<String> def) {
		try {
			return node.getList(TypeToken.of(String.class), def);
		} catch (ObjectMappingException e) {
			e.printStackTrace();
		}

		return def == null ? new ArrayList<>() : def;
	}

	public List<String> getAsList(ConfigurationNode node) {
		try {
			return node.getList(TypeToken.of(String.class));
		} catch (ObjectMappingException e) {
			e.printStackTrace();
		}

		return new ArrayList<>();
	}

	public boolean contains(ConfigurationNode node) {
		return !node.isVirtual();
	}

	public boolean isString(ConfigurationNode node) {
		return node.getValue() instanceof String;
	}

	public boolean isList(ConfigurationNode node) {
		return node.getValue() instanceof List;
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
