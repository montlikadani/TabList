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

public abstract class UpdateDownloader {

	private static final File RELEASESFOLDER = new File(TabList.getInstance().getFolder(), "releases");

	public static void checkFromGithub(org.bukkit.command.CommandSender sender) {
		if (!TabList.getInstance().getConfig().getBoolean("check-update")) {
			deleteDirectory();
			return;
		}

		CompletableFuture<?> comp = CompletableFuture.supplyAsync(() -> {
			try {
				URL githubUrl = new URL(
						"https://raw.githubusercontent.com/montlikadani/TabList/master/bukkit/src/main/resources/plugin.yml");
				BufferedReader br = new BufferedReader(new InputStreamReader(githubUrl.openStream()));
				String s;
				String lineWithVersion = "";
				while ((s = br.readLine()) != null) {
					String line = s;
					if (line.toLowerCase().contains("version")) {
						lineWithVersion = line;
						break;
					}
				}

				String versionString = lineWithVersion.split(": ")[1],
						nVersion = versionString.replaceAll("[^0-9]", ""),
						cVersion = TabList.getInstance().getDescription().getVersion().replaceAll("[^0-9]", "");

				int newVersion = Integer.parseInt(nVersion);
				int currentVersion = Integer.parseInt(cVersion);

				if (newVersion <= currentVersion || currentVersion >= newVersion) {
					return false;
				}

				String msg = "";
				if (sender instanceof Player) {
					msg = Util.colorMsg("&aA new update for TabList is available!&4 Version:&7 " + versionString
							+ (TabList.getInstance().getConfig().getBoolean("download-updates", false) ? ""
									: "\n&6Download:&c &nhttps://www.spigotmc.org/resources/46229/"));
				} else {
					msg = "New version (" + versionString
							+ ") is available at https://www.spigotmc.org/resources/46229/";
				}

				Util.sendMsg(sender, msg);

				if (!TabList.getInstance().getConfig().getBoolean("download-updates", false)) {
					deleteDirectory();
					return false;
				}

				final String name = "TabList-v" + versionString;

				if (!RELEASESFOLDER.exists()) {
					RELEASESFOLDER.mkdir();
				}

				// Do not attempt to download the file again, when it is already downloaded
				final File jar = new File(RELEASESFOLDER, name + ".jar");
				if (jar.exists()) {
					return false;
				}

				Util.logConsole("Downloading new version of TabList...");

				final URL download = new URL(
						"https://github.com/montlikadani/TabList/releases/latest/download/" + name + ".jar");

				InputStream in = download.openStream();
				try {
					Files.copy(in, jar.toPath(), StandardCopyOption.REPLACE_EXISTING);
				} finally {
					in.close();
				}

				return true;
			} catch (FileNotFoundException f) {
			} catch (Exception e) {
				e.printStackTrace();
			}

			return false;
		}).thenAccept(y -> {
			if (y) {
				Util.logConsole("The new TabList has been downloaded to releases folder.");
			}
		});

		// no
		new Thread(() -> {
			int tries = 0;

			while (!comp.isDone()) {
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				if (tries++ > 15) {
					comp.cancel(true);
					break;
				}
			}
		}).start();
	}

	private static void deleteDirectory() {
		if (!RELEASESFOLDER.exists()) {
			return;
		}

		for (File file : RELEASESFOLDER.listFiles()) {
			try {
				file.delete();
			} catch (SecurityException e) {
			}
		}

		try {
			RELEASESFOLDER.delete();
		} catch (SecurityException e) {
		}
	}
}
