package hu.montlikadani.tablist.bukkit.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.Validate;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import com.google.common.io.Files;

public class CommentedConfig extends YamlConfiguration {

	private final Map<String, String> comments = new HashMap<>();

	private YamlConfiguration config;
	private File file;

	public CommentedConfig(File file) {
		super();

		this.file = file;

		if (file != null) {
			this.config = getYml(file);
		}
	}

	public YamlConfiguration getConfig() {
		return config;
	}

	public File getFile() {
		return file;
	}

	public void load() {
		try {
			load(file);
		} catch (IOException | InvalidConfigurationException e) {
			e.printStackTrace();
		}
	}

	public void save() {
		try {
			save(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void save(String file) throws IOException {
		Validate.notEmpty(file, "File cannot be null/empty");

		save(new File(file));
	}

	@Override
	public void save(File file) throws IOException {
		Validate.notNull(file, "File cannot be null");

		Files.createParentDirs(file);
		String data = insertComments(saveToString());
		// 2. arg should be string for j8 users
		PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8.name());

		try {
			writer.write(data);
		} finally {
			writer.close();
		}
	}

	private String insertComments(String yaml) {
		if (comments.isEmpty()) {
			return yaml;
		}

		String[] yamlContents = yaml.split("[" + System.getProperty("line.separator") + "]");
		StringBuilder newContents = new StringBuilder(), currentPath = new StringBuilder();
		boolean commentedPath = false;
		boolean node = false;
		int depth = 0;

		for (final String line : yamlContents) {
			if (line.startsWith("#")) {
				continue; // Ignore comments
			}

			boolean keyOk = true;
			if (line.contains(": ")) {
				int index = line.indexOf(": ");
				if (index < 0) {
					index = line.length() - 1;
				}

				int whiteSpace = 0;
				for (int n = 0; n < line.length(); n++) {
					if (line.charAt(n) == ' ') {
						whiteSpace++;
					} else {
						break;
					}
				}

				String key = line.substring(whiteSpace, index);
				if (key.contains(" ") || key.contains("&") || key.contains(".") || key.contains("'")
						|| key.contains("\"")) {
					keyOk = false;
				}
			}

			if (line.contains(": ") && keyOk || (line.length() > 1 && line.charAt(line.length() - 1) == ':')) {
				commentedPath = false;
				node = true;

				int index = line.indexOf(": ");
				if (index < 0) {
					index = line.length() - 1;
				}

				if (currentPath.toString().isEmpty()) {
					currentPath = new StringBuilder(line.substring(0, index));
				} else {
					int whiteSpace = 0;
					for (int n = 0; n < line.length(); n++) {
						if (line.charAt(n) == ' ') {
							whiteSpace++;
						} else {
							break;
						}
					}

					if (whiteSpace / 2 > depth) {
						currentPath.append('.').append(line.substring(whiteSpace, index));
						depth++;
					} else if (whiteSpace / 2 < depth) {
						int newDepth = whiteSpace / 2;
						for (int i = 0; i < depth - newDepth; i++) {
							currentPath.replace(currentPath.lastIndexOf("."), currentPath.length(), "");
						}

						int lastIndex = currentPath.lastIndexOf(".");
						if (lastIndex < 0) {
							currentPath = new StringBuilder();
						} else {
							currentPath.replace(currentPath.lastIndexOf("."), currentPath.length(), "").append('.');
						}

						currentPath.append(line.substring(whiteSpace, index));
						depth = newDepth;
					} else {
						int lastIndex = currentPath.lastIndexOf(".");
						if (lastIndex < 0) {
							currentPath = new StringBuilder();
						} else {
							currentPath.replace(currentPath.lastIndexOf("."), currentPath.length(), "").append('.');
						}

						currentPath.append(line.substring(whiteSpace, index));
					}
				}
			} else {
				node = false;
			}

			StringBuilder newLine = new StringBuilder(line);
			if (node) {
				String comment = "";
				if (!commentedPath) {
					comment = comments.getOrDefault(currentPath.toString(), "");
				}

				if (!comment.isEmpty()) {
					newLine.insert(0, System.getProperty("line.separator")).insert(0, comment);
					comment = "";
					commentedPath = true;
				}
			}

			newLine.append(System.getProperty("line.separator"));
			newContents.append(newLine.toString());
		}

		return newContents.toString();
	}

	public void addComment(String path, String... commentLines) {
		String leadingSpaces = "";
		for (int n = 0; n < path.length(); n++) {
			if (path.charAt(n) == '.') {
				leadingSpaces += "  ";
			}
		}

		boolean newLine = false;
		StringBuilder comment = new StringBuilder();
		for (String line : commentLines) {
			if (!line.isEmpty()) {
				line = leadingSpaces + "# " + line;
			}

			if (comment.length() > 0) {
				comment.append(System.getProperty("line.separator"));
			}

			if (!newLine) {
				line = "\n" + line;
				newLine = true;
			}

			comment.append(line);
		}

		comments.put(path, comment.toString());
	}

	public YamlConfiguration getYml(File file) {
		YamlConfiguration config = new YamlConfiguration();
		FileInputStream inputStream = null;

		try {
			inputStream = new FileInputStream(file);
			InputStreamReader read = new InputStreamReader(inputStream, StandardCharsets.UTF_8);

			config.load(read);
			read.close();
		} catch (FileNotFoundException e) {
		} catch (InvalidConfigurationException | IOException e) {
			System.out.println(e.getLocalizedMessage());
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return config;
	}

	public void copyDefaults(boolean value) {
		config.options().copyDefaults(value);
	}

	private String process(String path, Object value) {
		config.addDefault(path, value);
		copySetting(path);
		return path;
	}

	public Boolean get(String path, boolean def) {
		path = process(path, def);
		return config.getBoolean(path);
	}

	public int get(String path, int def) {
		path = process(path, def);
		return config.getInt(path);
	}

	public List<Integer> getIntList(String path, List<Integer> def) {
		path = process(path, def);
		return config.getIntegerList(path);
	}

	public List<String> get(String path, List<String> def) {
		path = process(path, def);

		List<String> ls = config.getStringList(path);
		for (int p = 0; p < ls.size(); p++) {
			ls.set(p, ls.get(p));
		}

		return ls;
	}

	public String get(String path, String def) {
		path = process(path, def);
		return config.getString(path);
	}

	public Double get(String path, Double def) {
		path = process(path, def);
		return config.getDouble(path);
	}

	private synchronized void copySetting(String path) {
		set(path, config.get(path));
	}
}
