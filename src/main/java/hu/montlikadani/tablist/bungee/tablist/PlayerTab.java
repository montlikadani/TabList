package hu.montlikadani.tablist.bungee.tablist;

import java.util.ArrayList;
import java.util.List;

import hu.montlikadani.tablist.bungee.TabList;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;

public class PlayerTab {

	private ProxiedPlayer player;

	private int i = 0, i2 = 0;

	private final List<String> header = new ArrayList<>();
	private final List<String> footer = new ArrayList<>();

	public PlayerTab(ProxiedPlayer player) {
		this.player = player;
	}

	public ProxiedPlayer getPlayer() {
		return player;
	}

	public List<String> getHeader() {
		return header;
	}

	public List<String> getFooter() {
		return footer;
	}

	public String getNextHeader() {
		int hSize = header.size() - 1;

		if (i < hSize) {
			i++;
		} else {
			i = 0;
		}

		return header.get(i);
	}

	public String getNextFooter() {
		int fSize = footer.size() - 1;

		if (i2 < fSize) {
			i2++;
		} else {
			i2 = 0;
		}

		return footer.get(i2);
	}

	public void clearAll() {
		header.clear();
		footer.clear();

		player.resetTabHeader();
	}

	public void loadTabList() {
		clearAll();

		final Configuration conf = TabList.getInstance().getConf();
		final String pName = player.getName();
		final String server = player.getServer() != null ? player.getServer().getInfo().getName() : "";

		String path = "tablist.";

		per: for (String servers : conf.getSection(path + "per-server").getKeys()) {
			for (String split : servers.split(", ")) {
				if (server.equalsIgnoreCase(split)) {
					header.addAll(
							conf.getStringList(path + "per-server." + servers + ".per-player." + pName + ".header"));

					if (header.isEmpty())
						header.addAll(conf.getStringList(path + "per-server." + servers + ".header"));

					footer.addAll(
							conf.getStringList(path + "per-server." + servers + ".per-player." + pName + ".footer"));

					if (footer.isEmpty())
						footer.addAll(conf.getStringList(path + "per-server." + servers + ".footer"));

					break per;
				}
			}
		}

		pl: for (String players : conf.getSection(path + "per-player").getKeys()) {
			for (String split : players.split(", ")) {
				if (pName.equalsIgnoreCase(split)) {
					header.addAll(conf.getStringList(path + "per-player." + players + ".header"));
					footer.addAll(conf.getStringList(path + "per-player." + players + ".footer"));
					break pl;
				}
			}
		}

		if (!header.isEmpty() && !footer.isEmpty()) {
			return;
		}

		if (header.isEmpty())
			header.addAll(conf.getStringList(path + "per-server." + server + ".per-player." + pName + ".header"));

		if (header.isEmpty())
			header.addAll(conf.getStringList(path + "per-server." + server + ".header"));

		if (header.isEmpty())
			header.addAll(conf.getStringList(path + "per-player." + pName + ".header"));

		if (header.isEmpty())
			header.addAll(conf.getStringList(path + "header"));

		if (footer.isEmpty())
			footer.addAll(conf.getStringList(path + "per-server." + server + ".per-player." + pName + ".footer"));

		if (footer.isEmpty())
			footer.addAll(conf.getStringList(path + "per-server." + server + ".footer"));

		if (footer.isEmpty())
			footer.addAll(conf.getStringList(path + "per-player." + pName + ".footer"));

		if (footer.isEmpty())
			footer.addAll(conf.getStringList(path + "footer"));
	}
}
