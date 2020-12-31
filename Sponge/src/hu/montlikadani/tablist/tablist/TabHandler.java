package hu.montlikadani.tablist.tablist;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.scheduler.ScheduledTask;

import hu.montlikadani.tablist.ConfigValues;
import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.player.ITabPlayer;
import hu.montlikadani.tablist.utils.SchedulerUtil;

public class TabHandler {

	public static final Map<UUID, Boolean> TABENABLED = new HashMap<>();

	private TabList plugin;
	private ScheduledTask task;

	private final Set<TabListManager> tabPlayers = new HashSet<>();

	public TabHandler(TabList plugin) {
		this.plugin = plugin;
	}

	public Set<TabListManager> getTabPlayers() {
		return tabPlayers;
	}

	public Optional<ScheduledTask> getTask() {
		return Optional.ofNullable(task);
	}

	public void cancelTask() {
		if (task != null) {
			task.cancel();
			task = null;
		}
	}

	public void addPlayer(ITabPlayer p) {
		if (p == null || isPlayerInTab(p)) {
			return;
		}

		final TabListManager tabManager = new TabListManager(plugin, p);
		tabManager.loadTab();
		tabPlayers.add(tabManager);

		final int refreshTime = ConfigValues.getTablistUpdateTime();
		if (refreshTime < 1) {
			tabManager.sendTab();
			return;
		}

		if (task == null) {
			task = SchedulerUtil.submitScheduleAsyncTask(refreshTime, TimeUnit.MILLISECONDS, task -> {
				if (Sponge.getServer().getOnlinePlayers().isEmpty()) {
					cancelTask();
					return;
				}

				tabPlayers.forEach(TabListManager::sendTab);
			});
		}
	}

	public void removePlayer(ITabPlayer player) {
		getPlayerTab(player).ifPresent(tabManager -> {
			tabManager.sendTabList(player, "", "");
			tabPlayers.remove(tabManager);
		});
	}

	public void removeAll() {
		cancelTask();

		tabPlayers.forEach(TabListManager::clearTab);
		tabPlayers.clear();
	}

	public boolean isPlayerInTab(ITabPlayer player) {
		return getPlayerTab(player).isPresent();
	}

	public Optional<TabListManager> getPlayerTab(final ITabPlayer player) {
		return tabPlayers.stream().filter(tab -> tab.getPlayer().getPlayerUUID().equals(player.getPlayerUUID()))
				.findFirst();
	}
}
