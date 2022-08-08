package hu.montlikadani.tablist.tablist;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.config.ConfigConstants;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.scheduler.ScheduledTask;

public class TabManager {

	private final TabList plugin;

	private ScheduledTask task;

	public final Set<UUID> tabEnableStatus = new HashSet<>();
	private final Set<PlayerTab> playerTabs = Collections.newSetFromMap(new ConcurrentHashMap<>());

	public TabManager(TabList plugin) {
		this.plugin = plugin;
	}

	public void addPlayer(ProxiedPlayer player) {
		if (!ConfigConstants.isTabEnabled()) {
			return;
		}

		UUID playerId = player.getUniqueId();

		getPlayerTab(playerId).orElseGet(() -> {
			PlayerTab pTab = new PlayerTab(plugin, playerId);
			playerTabs.add(pTab);
			return pTab;
		}).loadTabList();
	}

	public void removePlayer(ProxiedPlayer player) {
		getPlayerTab(player.getUniqueId()).ifPresent(playerTabs::remove);
	}

	public Optional<PlayerTab> getPlayerTab(UUID playerId) {
		return playerTabs.stream().filter(g -> g.playerId.equals(playerId)).findFirst();
	}

	public void start() {
		if (!ConfigConstants.isTabEnabled() || plugin.getProxy().getOnlineCount() == 0) {
			cancel();
			return;
		}

		if (task != null) {
			return;
		}

		task = plugin.getProxy().getScheduler().schedule(plugin, () -> {
			if (plugin.getProxy().getOnlineCount() == 0) {
				cancel();
				return;
			}

			for (PlayerTab tab : playerTabs) {
				if (tabEnableStatus.contains(tab.playerId)) {
					ProxiedPlayer player = tab.getPlayer();

					if (player != null) {
						player.resetTabHeader();
					}

					continue;
				}

				tab.update();
			}
		}, 10L, ConfigConstants.getTabRefreshInterval(), TimeUnit.MILLISECONDS);
	}

	public void cancel() {
		if (task != null) {
			task.cancel();
			task = null;
		}

		for (ProxiedPlayer player : plugin.getProxy().getPlayers()) {
			getPlayerTab(player.getUniqueId()).ifPresent(PlayerTab::clearTab);
		}

		playerTabs.clear();
	}
}
