package hu.montlikadani.tablist.bungee.tablist;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import hu.montlikadani.tablist.bungee.TabList;
import hu.montlikadani.tablist.bungee.config.ConfigConstants;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.scheduler.ScheduledTask;

public class TabManager implements ITask {

	private TabList plugin;

	private ScheduledTask task;

	private final Set<UUID> tabEnableStatus = new HashSet<>();
	private final Set<PlayerTab> playerTabs = Collections.newSetFromMap(new ConcurrentHashMap<>());

	public TabManager(TabList plugin) {
		this.plugin = plugin;
	}

	@Override
	public ScheduledTask getTask() {
		return task;
	}

	public Set<UUID> getTabToggle() {
		return tabEnableStatus;
	}

	public Set<PlayerTab> getPlayerTabs() {
		return playerTabs;
	}

	public void addPlayer(ProxiedPlayer player) {
		if (!ConfigConstants.isTabEnabled()) {
			return;
		}

		PlayerTab tab = getPlayerTab(player).orElse(new PlayerTab(plugin, player.getUniqueId()));
		tab.loadTabList();
		playerTabs.add(tab);
	}

	public void removePlayer(ProxiedPlayer player) {
		getPlayerTab(player).ifPresent(playerTabs::remove);
	}

	public Optional<PlayerTab> getPlayerTab(ProxiedPlayer player) {
		return playerTabs.stream().filter(g -> g.getUniqueId().equals(player.getUniqueId())).findFirst();
	}

	@Override
	public void start() {
		if (!ConfigConstants.isTabEnabled() || plugin.getProxy().getPlayers().isEmpty()) {
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

			for (PlayerTab tab : playerTabs) {
				if (tabEnableStatus.contains(tab.getUniqueId())) {
					tab.getPlayer().resetTabHeader();
					continue;
				}

				tab.update();
			}
		}, 10L, ConfigConstants.getTabRefreshInterval(), TimeUnit.MILLISECONDS);
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
