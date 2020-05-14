package hu.montlikadani.tablist.bukkit.tablist.fakeplayers;

import static hu.montlikadani.tablist.bukkit.utils.Util.colorMsg;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bukkit.entity.Player;

import hu.montlikadani.tablist.bukkit.ConfigValues;
import hu.montlikadani.tablist.bukkit.Configuration;
import hu.montlikadani.tablist.bukkit.TabList;

public class FakePlayerHandler {

	private TabList plugin;

	private final Set<IFakePlayers> fakePlayers = new HashSet<>();

	public FakePlayerHandler(TabList plugin) {
		this.plugin = plugin;
	}

	public Set<IFakePlayers> getFakePlayers() {
		return fakePlayers;
	}

	public IFakePlayers getFakePlayerByName(String name) {
		for (IFakePlayers fp : fakePlayers) {
			if (fp.getName().equalsIgnoreCase(name)) {
				return fp;
			}
		}

		return null;
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
		if (name == null || name.trim().isEmpty()) {
			return false;
		}

		if (name.length() > 16) {
			name = name.substring(0, 16);
		}

		if (getFakePlayerByName(name) != null) {
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

		fp.createFakeplayer(p);
		return true;
	}

	public void removeAllFakePlayer() {
		for (IFakePlayers fp : fakePlayers) {
			if (fp != null) {
				fp.removeFakePlayer();
			}
		}

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

		for (Iterator<IFakePlayers> it = fakePlayers.iterator(); it.hasNext();) {
			IFakePlayers fp = it.next();
			if (fp != null && fp.getName().equalsIgnoreCase(name)) {
				fp.removeFakePlayer();
				it.remove();
				break;
			}
		}

		return true;
	}
}
