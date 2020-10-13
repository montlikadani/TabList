package hu.montlikadani.tablist.bukkit.tablist.fakeplayers;

import static hu.montlikadani.tablist.bukkit.utils.Util.colorMsg;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.bukkit.entity.Player;

import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.config.ConfigValues;
import hu.montlikadani.tablist.bukkit.config.Configuration;
import hu.montlikadani.tablist.bukkit.utils.Util;

public class FakePlayerHandler {

	private TabList plugin;

	private final Set<IFakePlayers> fakePlayers = new HashSet<>();

	public FakePlayerHandler(TabList plugin) {
		this.plugin = plugin;
	}

	public Set<IFakePlayers> getFakePlayers() {
		return fakePlayers;
	}

	public Optional<IFakePlayers> getFakePlayerByName(String name) {
		for (IFakePlayers fp : fakePlayers) {
			if (fp.getName().equalsIgnoreCase(name)) {
				return Optional.ofNullable(fp);
			}
		}

		return Optional.empty();
	}

	public void load() {
		fakePlayers.clear();

		if (!ConfigValues.isFakePlayers()) {
			return;
		}

		for (String l : getFakePlayersFromConfig()) {
			String name = l;
			String headUUID = "";
			int ping = -1;
			if (l.contains(";")) {
				String[] split = l.split(";");

				name = split[0];
				headUUID = split.length > 0 ? split[1] : "";
				ping = split.length > 1 ? Integer.parseInt(split[2]) : -1;
			}

			final IFakePlayers fp = new FakePlayers(colorMsg(name));
			fakePlayers.add(fp);

			final int finalPing = ping;
			final String finalHeadUUID = headUUID;
			plugin.getServer().getOnlinePlayers().forEach(all -> fp.createFakePlayer(all, finalHeadUUID, finalPing));
		}
	}

	public boolean createPlayer(Player p, String name) {
		return createPlayer(p, name, "", -1);
	}

	public boolean createPlayer(Player p, String name, int ping) {
		return createPlayer(p, name, "", ping);
	}

	public boolean createPlayer(Player p, String name, String headUUID, int ping) {
		if (name == null || name.trim().isEmpty()) {
			return false;
		}

		if (name.length() > 16) {
			name = name.substring(0, 16);
		}

		if (getFakePlayerByName(name).isPresent()) {
			return false;
		}

		if (!Util.isRealUUID(headUUID)) {
			p.sendMessage("This uuid not matches to a real player uuid.");
			return false;
		}

		List<String> fakepls = getFakePlayersFromConfig();
		String result = name + ";" + (headUUID.trim().isEmpty() ? "uuid" : headUUID) + (ping > -1 ? ";" + ping : "");
		fakepls.add(result);

		Configuration conf = plugin.getConf();
		conf.getFakeplayers().set("fakeplayers", fakepls);
		try {
			conf.getFakeplayers().save(conf.getFakeplayersFile());
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		name = colorMsg(name);

		IFakePlayers fp = new FakePlayers(name);
		fakePlayers.add(fp);

		fp.createFakePlayer(p, headUUID, ping);
		return true;
	}

	public void removeAllFakePlayer(boolean removeFromConfig) {
		fakePlayers.forEach(IFakePlayers::removeFakePlayer);
		fakePlayers.clear();

		if (!removeFromConfig) {
			return;
		}

		List<String> fakepls = getFakePlayersFromConfig();
		fakepls.clear();

		Configuration conf = plugin.getConf();
		conf.getFakeplayers().set("fakeplayers", fakepls);
		try {
			conf.getFakeplayers().save(conf.getFakeplayersFile());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean removePlayer(String name) {
		if (name == null || name.trim().isEmpty()) {
			return false;
		}

		List<String> fakepls = getFakePlayersFromConfig();

		String path = "";
		for (String names : fakepls) {
			if (!names.contains(";")) {
				path = names;
				continue;
			}

			String n = names.split(";")[0];
			if (n.equalsIgnoreCase(name)) {
				path = names;
				break;
			}
		}

		if (!path.isEmpty()) {
			fakepls.remove(path);

			Configuration conf = plugin.getConf();
			conf.getFakeplayers().set("fakeplayers", fakepls);
			try {
				conf.getFakeplayers().save(conf.getFakeplayersFile());
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}

		getFakePlayerByName(name).ifPresent(fp -> {
			fp.removeFakePlayer();
			fakePlayers.remove(fp);
		});

		return true;
	}

	public List<String> getFakePlayersFromConfig() {
		return plugin.getConf().getFakeplayers().getStringList("fakeplayers");
	}
}
