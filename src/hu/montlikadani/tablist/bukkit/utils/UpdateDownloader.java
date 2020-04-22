package hu.montlikadani.tablist.bukkit.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.bukkit.scheduler.BukkitRunnable;

import hu.montlikadani.tablist.bukkit.TabList;

/**
 * @author montlikadani
 *
 */
public class UpdateDownloader {

	public static String checkFromGithub(String sender) {
		if (!TabList.getInstance().getC().getBoolean("check-update")) {
			return "";
		}

		String msg = "";
		String versionString = "";
		String lineWithVersion = "";

		int newVersion = 0;
		int currentVersion = 0;

		try {
			URL githubUrl = new URL("https://raw.githubusercontent.com/montlikadani/TabList/master/plugin.yml");
			BufferedReader br = new BufferedReader(new InputStreamReader(githubUrl.openStream()));
			String s;
			while ((s = br.readLine()) != null) {
				String line = s;
				if (line.toLowerCase().contains("version")) {
					lineWithVersion = line;
					break;
				}
			}

			versionString = lineWithVersion.split(": ")[1];
			String nVersion = versionString.replaceAll("[^0-9]", "");
			newVersion = Integer.parseInt(nVersion);

			String cVersion = TabList.getInstance().getDescription().getVersion().replaceAll("[^0-9]", "");
			currentVersion = Integer.parseInt(cVersion);

			if (newVersion > currentVersion) {
				if ("player".equals(sender)) {
					msg = "&8&m&l--------------------------------------------------\n"
							+ "&aA new update for TabList is available!&4 Version:&7 " + versionString
							+ (TabList.getInstance().getC().getBoolean("download-updates") ? ""
									: "\n&6Download:&c &nhttps://www.spigotmc.org/resources/46229/")
							+ "\n&8&m&l--------------------------------------------------";
				} else if ("console".equals(sender)) {
					msg = "New version (" + versionString
							+ ") is available at https://www.spigotmc.org/resources/46229/";
				}

				msg = Util.colorMsg(msg);
			} else if ("console".equals(sender)) {
				return "You're running the latest version.";
			}

			if (newVersion <= currentVersion) {
				return msg;
			}

			if (!TabList.getInstance().getC().getBoolean("download-updates")) {
				return msg;
			}

			final String name = "TabList-" + versionString;

			String updatesFolder = TabList.getInstance().getFolder() + File.separator + "releases";
			File temp = new File(updatesFolder);
			if (!temp.exists()) {
				temp.mkdir();
			}

			final File jar = new File(updatesFolder + File.separator + name + ".jar");
			if (jar.exists()) {
				temp.delete();
				return msg;
			}

			Util.logConsole("Downloading new version of TabList...");

			final URL download = new URL(
					"https://github.com/montlikadani/TabList/releases/latest/download/TabList.jar");

			new BukkitRunnable() {
				@Override
				public void run() {
					try {
						InputStream in = download.openStream();
						Files.copy(in, jar.toPath(), StandardCopyOption.REPLACE_EXISTING);

						in.close();

						Util.logConsole("The new TabList has been downloaded to releases folder.");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}.runTaskLaterAsynchronously(TabList.getInstance(), 0);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return msg;
	}
}
