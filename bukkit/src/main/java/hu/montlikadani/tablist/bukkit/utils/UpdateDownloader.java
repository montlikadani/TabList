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

import org.bukkit.entity.Player;

import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.API.TabListAPI;

public abstract class UpdateDownloader {

	private static final TabList PLUGIN = TabListAPI.getPlugin();

	private static File releasesFolder;

	public static void checkFromGithub(org.bukkit.command.CommandSender sender) {
		releasesFolder = new File(PLUGIN.getFolder(), "releases");

		if (!PLUGIN.getConfig().get("check-update", false)) {
			deleteDirectory();
			return;
		}

		CompletableFuture.supplyAsync(() -> {
			try {
				URL githubUrl = new URL(
						"https://raw.githubusercontent.com/montlikadani/TabList/master/bukkit/src/main/resources/plugin.yml");
				BufferedReader br = new BufferedReader(new InputStreamReader(githubUrl.openStream()));
				String s;
				String lineWithVersion = "";
				while ((s = br.readLine()) != null) {
					if (s.toLowerCase().contains("version")) {
						lineWithVersion = s;
						break;
					}
				}

				String versionString = lineWithVersion.split(": ", 2)[1],
						nVersion = versionString.replaceAll("[^0-9]", ""),
						cVersion = PLUGIN.getDescription().getVersion().replaceAll("[^0-9]", "");

				int newVersion = Integer.parseInt(nVersion);
				int currentVersion = Integer.parseInt(cVersion);

				if (newVersion <= currentVersion || currentVersion >= newVersion) {
					deleteDirectory();
					return false;
				}

				String msg = "";
				if (sender instanceof Player) {
					msg = Util.colorMsg("&aA new update for TabList is available!&4 ServerVersion:&7 " + versionString
							+ (PLUGIN.getConfig().get("download-updates", false) ? ""
									: "\n&6Download:&c &nhttps://www.spigotmc.org/resources/46229/"));
				} else {
					msg = "New version (" + versionString
							+ ") is available at https://www.spigotmc.org/resources/46229/";
				}

				Util.sendMsg(sender, msg);

				if (!PLUGIN.getConfig().get("download-updates", false)) {
					deleteDirectory();
					return false;
				}

				final String name = "TabList-v" + versionString;

				if (!releasesFolder.exists()) {
					releasesFolder.mkdir();
				}

				// Do not attempt to download the file again, when it is already downloaded
				final File jar = new File(releasesFolder, name + ".jar");
				if (jar.exists()) {
					return false;
				}

				Util.logConsole("Downloading new version of TabList...");

				final URL download = new URL(
						"https://github.com/montlikadani/TabList/releases/latest/download/" + name + ".jar");

				try (InputStream in = download.openStream()) {
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
		if (!releasesFolder.exists()) {
			return;
		}

		for (File file : releasesFolder.listFiles()) {
			try {
				file.delete();
			} catch (SecurityException e) {
			}
		}

		try {
			releasesFolder.delete();
		} catch (SecurityException e) {
		}
	}
}
