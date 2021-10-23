package hu.montlikadani.tablist.bukkit.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

public class CommentedConfig extends YamlConfiguration {

	// This map shouldn't be set as "weak" as this needed for #save method
	private final java.util.Map<String, String> comments = new java.util.HashMap<>();

	private YamlConfiguration config;
	private File file;

	public CommentedConfig(File file) {
		this.file = file;
		config = getYml();
	}

	public YamlConfiguration getConfig() {
		return config;
	}

	public File getFile() {
		return file;
	}

	private YamlConfiguration getYml() {
		YamlConfiguration config = new YamlConfiguration();

		try (InputStreamReader reader = new InputStreamReader(new java.io.FileInputStream(file))) {
			config.load(reader);
		} catch (FileNotFoundException e) {
		} catch (InvalidConfigurationException | IOException e) {
			org.bukkit.Bukkit.getLogger().log(java.util.logging.Level.WARNING, e.getLocalizedMessage());
		}

		return config;
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
		save(new File(file));
	}

	@Override
	public void save(File file) throws IOException {
		com.google.common.io.Files.createParentDirs(file);

		String saveToString = saveToString();
		if (saveToString.isEmpty()) {
			return;
		}

		try (PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8.name())) {
			writer.write(insertComments(saveToString));
			writer.flush();
		}
	}

	private String insertComments(String yaml) {
		if (comments.isEmpty()) {
			return yaml;
		}

		StringBuilder newContents = new StringBuilder(), currentPath = new StringBuilder();
		boolean commentedPath = false, node = false;
		int depth = 0;

		String[] split = yaml.split("[" + System.lineSeparator() + "]");

		for (int s = 0; s < split.length; s++) {
			final String line = split[s];

			if (line.charAt(0) == '#') {
				continue; // Ignore comments
			}

			int length = line.length();
			boolean keyOk = true;
			int index = line.indexOf(": ");

			if (index >= 0) {
				int whiteSpace = 0;

				for (int n = 0; n < length; n++) {
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

			if ((keyOk && index >= 0) || (length > 1 && line.charAt(length - 1) == ':')) {
				commentedPath = false;
				node = true;

				if (index < 0 && (index = line.indexOf(": ")) < 0) {
					index = length - 1;
				}

				if (currentPath.length() == 0) {
					currentPath = new StringBuilder(line.substring(0, index));
				} else {
					int whiteSpace = 0;

					for (int n = 0; n < length; n++) {
						if (line.charAt(n) == ' ') {
							whiteSpace++;
						} else {
							break;
						}
					}

					int newDepth = whiteSpace / 2;

					if (newDepth > depth) {
						currentPath.append('.').append(line.substring(whiteSpace, index));
						depth++;
					} else if (newDepth < depth) {
						int d = depth - newDepth;

						for (int i = 0; i < d; i++) {
							currentPath.replace(currentPath.lastIndexOf("."), currentPath.length(), "");
						}

						int lastIndex = currentPath.lastIndexOf(".");
						if (lastIndex < 0) {
							currentPath = new StringBuilder();
						} else {
							currentPath.replace(lastIndex, currentPath.length(), "").append('.');
						}

						currentPath.append(line.substring(whiteSpace, index));
						depth = newDepth;
					} else {
						int lastIndex = currentPath.lastIndexOf(".");

						if (lastIndex < 0) {
							currentPath = new StringBuilder();
						} else {
							currentPath.replace(lastIndex, currentPath.length(), "").append('.');
						}

						currentPath.append(line.substring(whiteSpace, index));
					}
				}
			} else {
				node = false;
			}

			StringBuilder newLine = new StringBuilder(line);

			if (node) {
				String comment = !commentedPath ? comments.getOrDefault(currentPath.toString(), "") : "";

				if (!comment.isEmpty()) {
					newLine.insert(0, System.lineSeparator()).insert(0, comment);
					commentedPath = true;
				}
			}

			newLine.append(System.lineSeparator());
			newContents.append(newLine.toString());
		}

		return newContents.toString();
	}

	public void addComment(String path, String... commentLines) {
		if (commentLines.length == 0) {
			return;
		}

		String leadingSpaces = "";
		int length = path.length();

		for (int n = 0; n < length; n++) {
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
				comment.append(System.lineSeparator());
			}

			if (!newLine) {
				line = System.lineSeparator() + line;
				newLine = true;
			}

			comment.append(line);
		}

		comments.put(path, comment.toString());
	}

	public void cleanUp() {
		// Get rid of removed options by cleaning the file content
		try (PrintWriter writer = new PrintWriter(file)) {
			writer.write("");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void copyDefaults() {
		config.options().copyDefaults(true);
	}

	public boolean get(String path, boolean def) {
		boolean value = config.getBoolean(path, def);

		set(path, value);
		return value;
	}

	public int get(String path, int def) {
		int value = config.getInt(path, def);

		set(path, value);
		return value;
	}

	public List<String> get(String path, List<String> def) {
		config.addDefault(path, def);

		List<String> value = config.getStringList(path);

		set(path, value);
		return value;
	}

	public String get(String path, String def) {
		String value = config.getString(path, def);

		set(path, value);
		return value;
	}
}
