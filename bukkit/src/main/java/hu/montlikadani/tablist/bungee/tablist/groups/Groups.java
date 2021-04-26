package hu.montlikadani.tablist.bungee.tablist.groups;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import hu.montlikadani.tablist.bungee.TabList;
import hu.montlikadani.tablist.bungee.config.ConfigConstants;
import hu.montlikadani.tablist.bungee.tablist.ITask;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.scheduler.ScheduledTask;

public class Groups implements ITask {

	private final TabList plugin;

	private final Set<PlayerGroup> playersGroup = Collections.newSetFromMap(new ConcurrentHashMap<>());

	private ScheduledTask task;

	public Groups(TabList plugin) {
		this.plugin = plugin;
	}

	public Optional<PlayerGroup> getPlayerGroup(ProxiedPlayer player) {
		return playersGroup.stream().filter(g -> g.getPlayerUUID().equals(player.getUniqueId())).findFirst();
	}

	public void addPlayer(ProxiedPlayer player) {
		if (!ConfigConstants.isGroupsEnabled() || !getPlayerGroup(player).isPresent()) {
			playersGroup.add(new PlayerGroup(plugin, player.getUniqueId()));
		}
	}

	public void removePlayer(ProxiedPlayer player) {
		getPlayerGroup(player).ifPresent(playersGroup::remove);
	}

	@Override
	public void start() {
		if (!ConfigConstants.isGroupsEnabled() || plugin.getProxy().getPlayers().isEmpty()) {
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

			for (PlayerGroup group : playersGroup) {
				group.update();
			}
		}, 10L, ConfigConstants.getGroupsRefreshInterval(), TimeUnit.MILLISECONDS);
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

		playersGroup.forEach(g -> {
			ProxiedPlayer player = plugin.getProxy().getPlayer(g.getPlayerUUID());

			if (player != null) {
				g.sendPacket(player, player.getName());
			}
		});

		playersGroup.clear();
	}
}
