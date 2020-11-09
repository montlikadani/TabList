package hu.montlikadani.tablist.sponge.tablist;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;

import hu.montlikadani.tablist.sponge.ConfigValues;
import hu.montlikadani.tablist.sponge.TabList;

public class TabHandler {

	public static final Map<UUID, Boolean> TABENABLED = new HashMap<>();

	private TabList plugin;
	private Task task;

	private final Set<TabListManager> tabPlayers = new HashSet<>();

	public TabHandler(TabList plugin) {
		this.plugin = plugin;
	}

	public Set<TabListManager> getTabPlayers() {
		return tabPlayers;
	}

	public Optional<Task> getTask() {
		return Optional.ofNullable(task);
	}

	public void cancelTask() {
		if (task != null) {
			task.cancel();
			task = null;
		}
	}

	public void addPlayer(Player p) {
		if (p == null || isPlayerInTab(p)) {
			return;
		}

		final TabListManager tabManager = new TabListManager(plugin, p.getUniqueId());
		tabManager.loadTab();
		tabPlayers.add(tabManager);

		final int refreshTime = ConfigValues.getTablistUpdateTime();
		if (refreshTime < 1) {
			tabManager.sendTab();
			return;
		}

		if (task == null) {
			task = Task.builder().async().intervalTicks(refreshTime).execute(task -> {
				if (Sponge.getServer().getOnlinePlayers().isEmpty()) {
					cancelTask();
					return;
				}

				tabPlayers.forEach(TabListManager::sendTab);
			}).submit(plugin);
		}
	}

	public void removePlayer(Player player) {
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

	public boolean isPlayerInTab(Player player) {
		return getPlayerTab(player).isPresent();
	}

	public Optional<TabListManager> getPlayerTab(final Player player) {
		return tabPlayers.stream().filter(tab -> tab.getPlayerUuid().equals(player.getUniqueId())).findFirst();
	}
}
