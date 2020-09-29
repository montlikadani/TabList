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
		if (!ConfigValues.isFakePlayers()) {
			return;
		}

		fakePlayers.clear();

		List<String> fpls = plugin.getConf().getFakeplayers().getStringList("fakeplayers");
		for (String l : fpls) {
			IFakePlayers fp = new FakePlayers(colorMsg(l));
			fakePlayers.add(fp);

			plugin.getServer().getOnlinePlayers().forEach(fp::createFakeplayer);
		}
	}

	public boolean createPlayer(Player p, String name) {
		return createPlayer(p, name, "");
	}

	public boolean createPlayer(Player p, String name, String headUUID) {
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

		Configuration conf = plugin.getConf();
		List<String> fakepls = conf.getFakeplayers().getStringList("fakeplayers");

		fakepls.add(name);

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

		fp.createFakeplayer(p, headUUID);
		return true;
	}

	public void removeAllFakePlayer() {
		fakePlayers.forEach(IFakePlayers::removeFakePlayer);
		fakePlayers.clear();
	}

	public boolean removePlayer(String name) {
		if (name == null || name.trim().isEmpty()) {
			return false;
		}

		Configuration conf = plugin.getConf();
		List<String> fakepls = conf.getFakeplayers().getStringList("fakeplayers");

		fakepls.remove(name);

		conf.getFakeplayers().set("fakeplayers", fakepls);
		try {
			conf.getFakeplayers().save(conf.getFakeplayersFile());
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		getFakePlayerByName(name).ifPresent(fp -> {
			fp.removeFakePlayer();
			fakePlayers.remove(fp);
		});

		return true;
	}
}
