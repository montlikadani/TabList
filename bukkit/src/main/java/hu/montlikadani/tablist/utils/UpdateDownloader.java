package hu.montlikadani.tablist.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.logging.Level;
import java.util.regex.Pattern;

import hu.montlikadani.tablist.TabList;

public abstract class UpdateDownloader {

	public static void checkFromGithub(final TabList tabList) {
		deleteDirectory(tabList);

		if (!tabList.getConfig().getBoolean("check-update", true)) {
			return;
		}

		java.util.concurrent.CompletableFuture.supplyAsync(() -> {
			try {
				return fetch(tabList);
			} catch (java.io.FileNotFoundException | java.net.UnknownHostException ignore) {
			} catch (IOException ex) { // catch Exception in case if it fails
				Util.printTrace(Level.SEVERE, tabList, ex.getMessage(), ex);
			}

			return -1;
		}).thenAccept(code -> {
			if (code == HttpURLConnection.HTTP_OK) {
				Util.consolePrint(Level.INFO, tabList, "The new TabList has been downloaded, restart the server to apply.");
			} else if (code != -2) {
				Util.consolePrint(Level.INFO, tabList, "Downloading update failed, error code: {0}", code);
			}
		});
	}

	private static int fetch(TabList tabList) throws IOException {
		String lineWithVersion = "";

		try (BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(URI.create(
						"https://raw.githubusercontent.com/montlikadani/TabList/master/bukkit/src/main/resources/plugin.yml")
				.toURL().openStream()))) {
			String s;

			while ((s = reader.readLine()) != null) {
				if (s.indexOf("version") != -1) {
					lineWithVersion = s;
					break;
				}
			}
		}

		if (lineWithVersion.isEmpty()) {
			return -2;
		}

		String pluginVersion = "";
		try {
			pluginVersion = tabList.getPluginMeta().getVersion();
		} catch (NoSuchMethodError ignore) {
		}

		if (pluginVersion.isEmpty()) {
			pluginVersion = tabList.getDescription().getVersion();
		}

		Pattern pattern = Pattern.compile("[^0-9]");
		int currentVersion;

		try {
			currentVersion = Integer.parseInt(pattern.matcher(pluginVersion).replaceAll(""));
		} catch (NumberFormatException ex) {
			return -2;
		}

		String versionString = Pattern.compile(": ").split(lineWithVersion, 2)[1];
		int newVersion = Integer.parseInt(pattern.matcher(versionString).replaceAll(""));

		if (newVersion <= currentVersion) {
			return -2;
		}

		Util.consolePrint(Level.INFO, tabList, "-------------");
		Util.consolePrint(Level.INFO, tabList, "New update is available for TabList");
		Util.consolePrint(Level.INFO, tabList, "Your version: {0}, New version {1}", pluginVersion, versionString);

		boolean downloadUpdates = tabList.getConfig().getBoolean("download-updates", false);

		if (!downloadUpdates) {
			Util.consolePrint(Level.INFO, tabList, "Download: https://www.spigotmc.org/resources/46229/");
			Util.consolePrint(Level.INFO, tabList, "");
			Util.consolePrint(Level.INFO, tabList,
					"Always consider upgrading to the latest version, which may include fixes.");
		}

		Util.consolePrint(Level.INFO, tabList, "");
		Util.consolePrint(Level.INFO, tabList, "To disable update checking, go to the config file");
		Util.consolePrint(Level.INFO, tabList, "-------------");

		if (!downloadUpdates) {
			return -2;
		}

		File updateFolder = tabList.getServer().getUpdateFolderFile();

		if (!updateFolder.exists() && !updateFolder.mkdir()) {
			return -2;
		}

		String name = "TabList-bukkit-v" + versionString + ".jar";
		File jar = new File(updateFolder, name);

		if (jar.exists()) {
			return -2;
		}

		Util.consolePrint(Level.INFO, tabList, "Downloading new version of TabList...");

		HttpURLConnection urlConnection = (HttpURLConnection) URI
				.create("https://github.com/montlikadani/TabList/releases/latest/download/" + name).toURL().openConnection();
		urlConnection.setRequestMethod("HEAD");

		int responseCode = urlConnection.getResponseCode();

		// Make sure the response code is always HTTP_OK = 200
		if (responseCode == HttpURLConnection.HTTP_OK) {
			try (java.io.InputStream in = urlConnection.getInputStream()) {
				java.nio.file.Files.copy(in, jar.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			}
		}

		return responseCode;
	}

	// Old releases directory
	private static void deleteDirectory(TabList tabList) {
		File releasesFolder = new File(tabList.getDataFolder(), "releases");

		if (!releasesFolder.exists()) {
			return;
		}

		File[] files = releasesFolder.listFiles();

		if (files != null) {
			for (File file : files) {
				try {
					file.delete();
				} catch (SecurityException ignore) {
				}
			}
		}

		try {
			releasesFolder.delete();
		} catch (SecurityException ignore) {
		}
	}
}
