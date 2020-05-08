package hu.montlikadani.tablist.bungee;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.config.Configuration;

public class TabManager implements ITask {

	private TabList plugin;

	private ScheduledTask task;

	private int i = 0, i2 = 0;

	private final Set<UUID> tabenable = new HashSet<>();

	private final List<String> hList = new ArrayList<>();
	private final List<String> fList = new ArrayList<>();

	public TabManager(TabList plugin) {
		this.plugin = plugin;
	}

	public List<String> getHeader() {
		return hList;
	}

	public List<String> getFooter() {
		return fList;
	}

	@Override
	public ScheduledTask getTask() {
		return task;
	}

	public Set<UUID> getTabToggle() {
		return tabenable;
	}

	@Override
	public void start() {
		hList.clear();
		fList.clear();

		if (!plugin.getConf().getBoolean("tablist.enable", false)) {
			return;
		}

		if (task != null) {
			cancel();
		}

		task = plugin.getProxy().getScheduler().schedule(plugin, () -> {
			if (plugin.getProxy().getPlayers().isEmpty()) {
				cancel();
				return;
			}

			plugin.getProxy().getPlayers().forEach(this::update);
		}, 0L, plugin.getConf().getInt("tablist.refresh-interval"), TimeUnit.MILLISECONDS);
	}

	@Override
	public void update(final ProxiedPlayer pl) {
		// To make sure the task is cancelled
		if (!plugin.getConf().getBoolean("tablist.enable", false)) {
			cancel();
			return;
		}

		if (tabenable.contains(pl.getUniqueId())) {
			pl.resetTabHeader();
			return;
		}

		if (pl.getServer() != null && plugin.getConf().getStringList("tablist.disabled-servers")
				.contains(pl.getServer().getInfo().getName())) {
			pl.resetTabHeader();
			return;
		}

		if (plugin.getConf().getStringList("tablist.blacklisted-players").contains(pl.getName())) {
			pl.resetTabHeader();
			return;
		}

		String[] t = getTablist(pl);
		if (t != null) {
			pl.setTabHeader(plugin.getComponentBuilder(Misc.replaceVariables(t[0], pl)),
					plugin.getComponentBuilder(Misc.replaceVariables(t[1], pl)));
		}
	}

	private String[] getTablist(ProxiedPlayer p) {
		String name = p.getName();
		String path = "tablist.";
		String server = p.getServer() != null ? p.getServer().getInfo().getName() : "";

		Configuration con = plugin.getConf();

		hList.clear();

		hList.addAll(con.getStringList(path + "per-server." + server + ".per-player." + name + ".header"));
		if (hList.isEmpty())
			hList.addAll(con.getStringList(path + "per-server." + server + ".header"));

		if (hList.isEmpty())
			hList.addAll(con.getStringList(path + "per-player." + name + ".header"));

		if (hList.isEmpty())
			hList.addAll(con.getStringList(path + "header"));

		fList.clear();

		fList.addAll(con.getStringList(path + "per-server." + server + ".per-player." + name + ".footer"));
		if (fList.isEmpty())
			fList.addAll(con.getStringList(path + "per-server." + server + ".footer"));

		if (fList.isEmpty())
			fList.addAll(con.getStringList(path + "per-player." + name + ".footer"));

		if (fList.isEmpty())
			fList.addAll(con.getStringList(path + "footer"));

		if (hList.isEmpty() && fList.isEmpty()) {
			return null;
		}

		int hSize = hList.size() - 1;
		int fSize = fList.size() - 1;

		if (i < hSize) {
			i++;
		} else {
			i = 0;
		}

		if (i2 < fSize) {
			i2++;
		} else {
			i2 = 0;
		}

		return new String[] { hList.get(i), fList.get(i2) };
	}

	@Override
	public void cancel() {
		if (task != null) {
			task.cancel();
			task = null;
		}

		plugin.getProxy().getPlayers().forEach(p -> p.resetTabHeader());
	}
}
