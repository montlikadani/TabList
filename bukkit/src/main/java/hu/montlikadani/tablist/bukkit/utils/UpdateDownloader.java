package hu.montlikadani.tablist.bukkit.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.regex.Pattern;

import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.config.constantsLoader.ConfigValues;

public abstract class UpdateDownloader {

	private static File releasesFolder;

	public static void checkFromGithub(TabList tabList) {
		if (!ConfigValues.isCheckUpdate()) {
			deleteDirectory();
			return;
		}

		CompletableFuture.supplyAsync(() -> {
			try {
				URL githubUrl = new URL(
						"https://raw.githubusercontent.com/montlikadani/TabList/master/bukkit/src/main/resources/plugin.yml");
				String lineWithVersion = "";

				try (BufferedReader br = new BufferedReader(new InputStreamReader(githubUrl.openStream()))) {
					String s;

					while ((s = br.readLine()) != null) {
						if (s.contains("version")) {
							lineWithVersion = s;
							break;
						}
					}
				}

				String versionString = Pattern.compile(": ").split(lineWithVersion, 2)[1];
				Pattern integerVersionPattern = Pattern.compile("[^0-9]");

				int newVersion = Integer.parseInt(integerVersionPattern.matcher(versionString).replaceAll(""));
				int currentVersion = Integer
						.parseInt(integerVersionPattern.matcher(tabList.getDescription().getVersion()).replaceAll(""));

				if (newVersion <= currentVersion || currentVersion >= newVersion) {
					deleteDirectory();
					return false;
				}

				tabList.getLogger().log(Level.INFO, "-------------");
				tabList.getLogger().log(Level.INFO, "New update is available for TabList");
				tabList.getLogger().log(Level.INFO,
						"Your version: " + tabList.getDescription().getVersion() + ", New version " + versionString);

				if (!ConfigValues.isDownloadUpdates()) {
					tabList.getLogger().log(Level.INFO, "Download: https://www.spigotmc.org/resources/46229/");
					tabList.getLogger().log(Level.INFO, "");
					tabList.getLogger().log(Level.INFO,
							"Always consider upgrading to the latest version, which may include fixes.");
				}

				tabList.getLogger().log(Level.INFO, "");
				tabList.getLogger().log(Level.INFO,
						"To disable update checking, go to the config file (not recommended)");
				tabList.getLogger().log(Level.INFO, "-------------");

				if (!ConfigValues.isDownloadUpdates()) {
					deleteDirectory();
					return false;
				}

				(releasesFolder = new File(tabList.getFolder(), "releases")).mkdirs();

				final String name = "TabList-" + versionString;
				final File jar = new File(releasesFolder, name + ".jar");

				if (jar.exists()) {
					return false; // Do not attempt to download the file again, when it is already downloaded
				}

				Util.logConsole("Downloading new version of TabList...");

				try (InputStream in = new URL(
						"https://github.com/montlikadani/TabList/releases/latest/download/" + name + ".jar")
								.openStream()) {
					Files.copy(in, jar.toPath(), StandardCopyOption.REPLACE_EXISTING);
				}

				return true;
			} catch (FileNotFoundException | java.net.UnknownHostException f) {
			} catch (Exception e) {
				e.printStackTrace();
			}

			return false;
		}).thenAccept(success -> {
			if (success) {
				Util.logConsole("The new TabList has been downloaded to releases folder.");
			}
		});
	}

	private static void deleteDirectory() {
		if (releasesFolder == null || !releasesFolder.exists()) {
			return;
		}

		File[] files = releasesFolder.listFiles();

		if (files != null) {
			for (File file : files) {
				try {
					file.delete();
				} catch (SecurityException e) {
				}
			}
		}

		try {
			releasesFolder.delete();
		} catch (SecurityException e) {
		}
	}
}
