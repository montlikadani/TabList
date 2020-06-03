package hu.montlikadani.tablist.bungee.tablist;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import hu.montlikadani.tablist.bungee.Misc;
import hu.montlikadani.tablist.bungee.TabList;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.scheduler.ScheduledTask;

public class TabManager implements ITask {

	private TabList plugin;

	private ScheduledTask task;

	private final Set<UUID> tabenable = new HashSet<>();
	private final Set<PlayerTab> playerTabs = new HashSet<>();

	public TabManager(TabList plugin) {
		this.plugin = plugin;
	}

	@Override
	public ScheduledTask getTask() {
		return task;
	}

	public Set<UUID> getTabToggle() {
		return tabenable;
	}

	public Set<PlayerTab> getPlayerTabs() {
		return playerTabs;
	}

	public Optional<PlayerTab> getPlayerTab(ProxiedPlayer player) {
		PlayerTab tab = null;

		for (PlayerTab tabs : playerTabs) {
			if (tabs.getPlayer() == player) {
				tab = tabs;
				break;
			}
		}

		return Optional.ofNullable(tab);
	}

	@Override
	public void start() {
		cancel();

		if (!plugin.getConf().getBoolean("tablist.enable", false)) {
			return;
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

		if (!getPlayerTab(pl).isPresent()) {
			PlayerTab tab = new PlayerTab(pl);
			playerTabs.add(tab);
			tab.loadTabList();
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

		List<String> restrictedPlayers = plugin.getConf().getStringList("tablist.blacklisted-players");
		if (restrictedPlayers.isEmpty()) {
			restrictedPlayers = plugin.getConf().getStringList("tablist.restricted-players");
		}

		if (restrictedPlayers.contains(pl.getName())) {
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
		Optional<PlayerTab> tab = getPlayerTab(p);
		if (tab.isPresent()) {
			return new String[] { tab.get().getNextHeader(), tab.get().getNextFooter() };
		}

		return new String[0];
	}

	@Override
	public void cancel() {
		if (task != null) {
			task.cancel();
			task = null;
		}

		plugin.getProxy().getPlayers().forEach(all -> getPlayerTab(all).ifPresent(PlayerTab::clearAll));
		playerTabs.clear();
	}
}
