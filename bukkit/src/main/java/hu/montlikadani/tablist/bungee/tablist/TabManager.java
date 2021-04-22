package hu.montlikadani.tablist.bungee.tablist;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import hu.montlikadani.tablist.bungee.TabList;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.scheduler.ScheduledTask;

public class TabManager implements ITask {

	private TabList plugin;

	private ScheduledTask task;

	private final Set<UUID> tabenable = new HashSet<>();
	private final Set<PlayerTab> playerTabs = Collections.synchronizedSet(new HashSet<PlayerTab>());

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

	public void addPlayer(ProxiedPlayer player) {
		if (!plugin.getConf().getBoolean("tablist.enable", false)) {
			return;
		}

		synchronized (playerTabs) {
			PlayerTab tab = getPlayerTab(player).orElse(new PlayerTab(plugin, player));
			if (!playerTabs.contains(tab)) {
				playerTabs.add(tab);
			}

			tab.loadTabList();
		}
	}

	public void removePlayer(ProxiedPlayer player) {
		synchronized (playerTabs) {
			getPlayerTab(player).ifPresent(playerTabs::remove);
		}
	}

	public Optional<PlayerTab> getPlayerTab(ProxiedPlayer player) {
		return playerTabs.stream().filter(g -> g.getPlayer().equals(player)).findFirst();
	}

	@Override
	public void start() {
		if (!plugin.getConf().getBoolean("tablist.enable", false) || plugin.getProxy().getPlayers().isEmpty()) {
			cancel();
			return;
		}

		if (task != null) {
			return;
		}

		task = plugin.getProxy().getScheduler().schedule(plugin, () -> {
			if (plugin.getProxy().getPlayers().isEmpty()) {
				cancel();
				return;
			}

			synchronized (playerTabs) {
				playerTabs.stream().filter(t -> {
					if (!tabenable.contains(t.getPlayer().getUniqueId())) {
						return true;
					}

					t.getPlayer().resetTabHeader();
					return false;
				}).forEach(PlayerTab::update);
			}
		}, 10L, plugin.getConf().getInt("tablist.refresh-interval"), TimeUnit.MILLISECONDS);
	}

	@Override
	public void cancel() {
		if (task != null) {
			task.cancel();
			task = null;
		}

		synchronized (playerTabs) {
			plugin.getProxy().getPlayers().forEach(all -> getPlayerTab(all).ifPresent(PlayerTab::clearAll));
			playerTabs.clear();
		}
	}
}
