package hu.montlikadani.tablist.bungee.tablist.groups;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import hu.montlikadani.tablist.bungee.TabList;
import hu.montlikadani.tablist.bungee.tablist.ITask;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.scheduler.ScheduledTask;

public class Groups implements ITask {

	private TabList plugin;
	private ScheduledTask task;

	private final Set<PlayerGroup> playersGroup = Collections.synchronizedSet(new HashSet<PlayerGroup>());

	public Groups(TabList plugin) {
		this.plugin = plugin;
	}

	public Optional<PlayerGroup> getPlayerGroup(ProxiedPlayer player) {
		return playersGroup.stream().filter(g -> g.getPlayerUUID().equals(player.getUniqueId())).findFirst();
	}

	public void addPlayer(ProxiedPlayer player) {
		synchronized (playersGroup) {
			if (!plugin.getConf().getBoolean("tablist-groups.enabled", false) || !getPlayerGroup(player).isPresent()) {
				playersGroup.add(new PlayerGroup(plugin, player.getUniqueId()));
			}
		}
	}

	public void removePlayer(ProxiedPlayer player) {
		synchronized (playersGroup) {
			getPlayerGroup(player).ifPresent(playersGroup::remove);
		}
	}

	@Override
	public void start() {
		if (!plugin.getConf().getBoolean("tablist-groups.enabled", false) || plugin.getProxy().getPlayers().isEmpty()) {
			cancel();
			return;
		}

		if (task != null) {
			return;
		}

		task = plugin.getProxy().getScheduler().schedule(plugin, () -> {
			if (plugin.getProxy().getPlayers().isEmpty() || !plugin.getConf().contains("groups")) {
				cancel();
				return;
			}

			synchronized (playersGroup) {
				playersGroup.forEach(PlayerGroup::update);
			}
		}, 10L, plugin.getConf().getInt("tablist-groups.refresh-time"), TimeUnit.MILLISECONDS);
	}

	@Override
	public ScheduledTask getTask() {
		return task;
	}

	@Override
	public void cancel() {
		if (task != null) {
			task.cancel();
			task = null;
		}

		synchronized (playersGroup) {
			playersGroup.forEach(g -> {
				ProxiedPlayer player = plugin.getProxy().getPlayer(g.getPlayerUUID());
				if (player != null) {
					g.sendPacket(player, player.getName());
				}
			});

			playersGroup.clear();
		}
	}
}
