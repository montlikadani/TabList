package hu.montlikadani.tablist.bukkit.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;

import org.bukkit.entity.Player;

import hu.montlikadani.tablist.bukkit.TabList;

/**
 * @author montlikadani
 *
 */
public class UpdateDownloader {

	public static void checkFromGithub(org.bukkit.command.CommandSender sender) {
		if (!TabList.getInstance().getConf().getConfig().getBoolean("check-update")) {
			return;
		}

		CompletableFuture.supplyAsync(() -> {
			try {
				URL githubUrl = new URL(
						"https://raw.githubusercontent.com/montlikadani/TabList/master/src/main/resources/plugin.yml");
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
					return null;
				}

				String msg = "";
				if (sender instanceof Player) {
					msg = Util.colorMsg("&aA new update for TabList is available!&4 Version:&7 " + versionString
							+ (TabList.getInstance().getConf().getConfig().getBoolean("download-updates", false) ? ""
									: "\n&6Download:&c &nhttps://www.spigotmc.org/resources/46229/"));
				} else {
					msg = "New version (" + versionString
							+ ") is available at https://www.spigotmc.org/resources/46229/";
				}

				Util.sendMsg(sender, msg);

				if (!TabList.getInstance().getConf().getConfig().getBoolean("download-updates", false)) {
					return null;
				}

				final String name = "TabList-v" + versionString;

				String updatesFolder = TabList.getInstance().getFolder() + File.separator + "releases";
				File temp = new File(updatesFolder);
				if (!temp.exists()) {
					temp.mkdir();
				}

				// Do not attempt to download the file again, when it is already downloaded
				final File jar = new File(updatesFolder + File.separator + name + ".jar");
				if (jar.exists()) {
					return null;
				}

				Util.logConsole("Downloading new version of TabList...");

				final URL download = new URL(
						"https://github.com/montlikadani/TabList/releases/latest/download/" + name + ".jar");

				InputStream in = download.openStream();
				Files.copy(in, jar.toPath(), StandardCopyOption.REPLACE_EXISTING);

				in.close();

				Util.logConsole("The new TabList has been downloaded to releases folder.");
			} catch (Exception e) {
				e.printStackTrace();
			}

			return null;
		});
	}
}
