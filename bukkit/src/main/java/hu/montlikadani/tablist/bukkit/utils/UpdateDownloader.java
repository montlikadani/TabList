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
import java.util.regex.Pattern;

import org.bukkit.entity.Player;

import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.api.TabListAPI;
import hu.montlikadani.tablist.bukkit.config.constantsLoader.ConfigValues;

public abstract class UpdateDownloader {

	private static final TabList TL = TabListAPI.getPlugin();

	private static File releasesFolder;
	private static final Pattern versionPattern = Pattern.compile(": "),
			integerVersionPattern = Pattern.compile("[^0-9]");

	public static void checkFromGithub(org.bukkit.command.CommandSender sender) {
		if (!ConfigValues.isCheckUpdate()) {
			deleteDirectory();
			return;
		}

		releasesFolder = new File(TL.getFolder(), "releases");

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

				String versionString = versionPattern.split(lineWithVersion, 2)[1];

				int newVersion = Integer.parseInt(integerVersionPattern.matcher(versionString).replaceAll(""));
				int currentVersion = Integer
						.parseInt(integerVersionPattern.matcher(TL.getDescription().getVersion()).replaceAll(""));

				if (newVersion <= currentVersion || currentVersion >= newVersion) {
					deleteDirectory();
					return false;
				}

				if (sender instanceof Player) {
					Util.sendMsg(sender,
							Util.colorMsg("&aA new update for TabList is available!&4 Version:&7 " + versionString
									+ (ConfigValues.isDownloadUpdates() ? ""
											: "\n&6Download:&c &nhttps://www.spigotmc.org/resources/46229/")));
				} else {
					TL.getLogger().log(java.util.logging.Level.INFO, "New version (" + versionString
							+ ") is available at https://www.spigotmc.org/resources/46229/");
				}

				if (!ConfigValues.isDownloadUpdates()) {
					deleteDirectory();
					return false;
				}

				releasesFolder.mkdirs();

				final String name = "TabList-v" + versionString;
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
			} catch (FileNotFoundException f) {
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
