package hu.montlikadani.tablist.bungee.tablist;

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

	public void addPlayer(ProxiedPlayer player) {
		if (!plugin.getConf().getBoolean("tablist.enable", false) || getPlayerTab(player).isPresent()) {
			return;
		}

		PlayerTab tab = new PlayerTab(player);
		playerTabs.add(tab);
		tab.loadTabList();
	}

	public void removePlayer(ProxiedPlayer player) {
		getPlayerTab(player).ifPresent(playerTabs::remove);
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

			playerTabs.stream().filter(t -> {
				if (!tabenable.contains(t.getPlayer().getUniqueId())) {
					return true;
				}

				t.getPlayer().resetTabHeader();
				return false;
			}).forEach(PlayerTab::update);
		}, 10L, plugin.getConf().getInt("tablist.refresh-interval"), TimeUnit.MILLISECONDS);
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
