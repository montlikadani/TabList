package hu.montlikadani.tablist.bungee.tablist.groups;

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

	private final Set<PlayerGroup> playersGroup = new HashSet<>();

	public Groups(TabList plugin) {
		this.plugin = plugin;
	}

	public Optional<PlayerGroup> getPlayerGroup(ProxiedPlayer player) {
		return Optional
				.ofNullable(playersGroup.stream().filter(g -> g.getPlayer().equals(player)).findFirst().orElse(null));
	}

	public void addPlayer(ProxiedPlayer player) {
		if (getPlayerGroup(player).isPresent()) {
			return;
		}

		playersGroup.add(new PlayerGroup(player));
	}

	public void removePlayer(ProxiedPlayer player) {
		getPlayerGroup(player).ifPresent(playersGroup::remove);
	}

	@Override
	public void start() {
		cancel();

		if (!plugin.getConf().getBoolean("tablist-groups.enabled", false) || plugin.getProxy().getPlayers().isEmpty()) {
			return;
		}

		task = plugin.getProxy().getScheduler().schedule(plugin, () -> {
			if (plugin.getProxy().getPlayers().isEmpty() || !plugin.getConf().contains("groups")) {
				cancel();
				return;
			}

			playersGroup.forEach(PlayerGroup::update);
		}, 0L, plugin.getConf().getInt("tablist-groups.refresh-time"), TimeUnit.MILLISECONDS);
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

		playersGroup.forEach(g -> g.sendPacket(g.getPlayer(), g.getPlayer().getName()));
		playersGroup.clear();
	}
}
