package hu.montlikadani.tablist.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.regex.Pattern;

import hu.montlikadani.tablist.TabList;

public abstract class UpdateDownloader {

	public static void checkFromGithub(TabList tabList) {
		deleteDirectory(tabList);

		if (!tabList.getConfig().getBoolean("check-update", true)) {
			return;
		}

		CompletableFuture.supplyAsync(() -> {
			try {
				String lineWithVersion = "";

				try (BufferedReader br = new BufferedReader(new InputStreamReader(new URL(
						"https://raw.githubusercontent.com/montlikadani/TabList/master/bukkit/src/main/resources/plugin.yml")
								.openStream()))) {
					String s;

					while ((s = br.readLine()) != null) {
						if (s.indexOf("version") != -1) {
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
					return false;
				}

				tabList.getLogger().log(Level.INFO, "-------------");
				tabList.getLogger().log(Level.INFO, "New update is available for TabList");
				tabList.getLogger().log(Level.INFO,
						"Your version: " + tabList.getDescription().getVersion() + ", New version " + versionString);

				boolean downloadUpdates = tabList.getConfig().getBoolean("download-updates", false);

				if (!downloadUpdates) {
					tabList.getLogger().log(Level.INFO, "Download: https://www.spigotmc.org/resources/46229/");
					tabList.getLogger().log(Level.INFO, "");
					tabList.getLogger().log(Level.INFO,
							"Always consider upgrading to the latest version, which may include fixes.");
				}

				tabList.getLogger().log(Level.INFO, "");
				tabList.getLogger().log(Level.INFO, "To disable update checking, go to the config file");
				tabList.getLogger().log(Level.INFO, "-------------");

				if (!downloadUpdates) {
					return false;
				}

				File updateFolder = tabList.getServer().getUpdateFolderFile();
				if (!updateFolder.exists() && !updateFolder.mkdir()) {
					return false;
				}

				final String name = "TabList-bukkit-" + versionString + ".jar";
				final File jar = new File(updateFolder, name);

				if (jar.exists()) {
					return false; // Do not attempt to download the file again, when it is already downloaded
				}

				Util.logConsole("Downloading new version of TabList...");

				try (java.io.InputStream in = new URL("https://github.com/montlikadani/TabList/releases/latest/download/" + name)
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
				Util.logConsole("The new TabList has been downloaded to " + tabList.getServer().getUpdateFolder() + " folder.");
			}
		});
	}

	// Old releases directory
	private static void deleteDirectory(TabList tabList) {
		File releasesFolder = new File(tabList.getFolder(), "releases");

		if (!releasesFolder.exists()) {
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
