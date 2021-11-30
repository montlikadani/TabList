package hu.montlikadani.tablist.tablist.groups;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.config.ConfigConstants;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.scheduler.ScheduledTask;

public class Groups {

	private final TabList plugin;

	private final Set<PlayerGroup> playersGroup = Collections.newSetFromMap(new ConcurrentHashMap<>());

	private ScheduledTask task;

	public Groups(TabList plugin) {
		this.plugin = plugin;
	}

	public Optional<PlayerGroup> getPlayerGroup(UUID playerId) {
		return playersGroup.stream().filter(g -> g.getPlayerId().equals(playerId)).findFirst();
	}

	public void addPlayer(ProxiedPlayer player) {
		if (!ConfigConstants.isGroupsEnabled()) {
			return;
		}

		UUID playerId = player.getUniqueId();

		if (!getPlayerGroup(playerId).isPresent()) {
			playersGroup.add(new PlayerGroup(playerId));
		}
	}

	public void removePlayer(ProxiedPlayer player) {
		getPlayerGroup(player.getUniqueId()).ifPresent(playersGroup::remove);
	}

	public void start() {
		if (!ConfigConstants.isGroupsEnabled() || plugin.getProxy().getOnlineCount() == 0) {
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

			for (PlayerGroup group : playersGroup) {
				group.update();
			}
		}, 10L, ConfigConstants.getGroupsRefreshInterval(), TimeUnit.MILLISECONDS);
	}

	public void cancel() {
		if (task != null) {
			task.cancel();
			task = null;
		}

		playersGroup.forEach(g -> {
			ProxiedPlayer player = plugin.getProxy().getPlayer(g.getPlayerId());

			if (player != null) {
				g.sendPacket(player, player.getName());
			}
		});

		playersGroup.clear();
	}
}
