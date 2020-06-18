package hu.montlikadani.tablist.sponge.tablist.groups;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;

import hu.montlikadani.tablist.sponge.TabList;

public class GroupTask implements Consumer<Task> {

	private final ConcurrentHashMap<String, TabPlayer> tabPlayers = new ConcurrentHashMap<>();

	private Task task;

	public Task getTask() {
		return task;
	}

	public ConcurrentHashMap<String, TabPlayer> getTabPlayers() {
		return tabPlayers;
	}

	public TabPlayer addPlayer(Player player) {
		String uuid = player.getUniqueId().toString();

		if (tabPlayers.containsKey(uuid)) {
			return tabPlayers.get(uuid);
		}

		TabPlayer tabPlayer = new TabPlayer(player);
		tabPlayers.put(uuid, tabPlayer);
		return tabPlayer;
	}

	public void removePlayer(Player player) {
		String uuid = player.getUniqueId().toString();
		if (tabPlayers.containsKey(uuid)) {
			tabPlayers.get(uuid).getGroup().ifPresent(g -> g.removeTeam(player));
			tabPlayers.remove(uuid);
		}
	}

	public void runTask() {
		if (!isRunning()) {
			task = Task.builder().async().intervalTicks(4).execute(this::accept).submit(TabList.get());
		}
	}

	public void cancel() {
		if (!isRunning()) {
			return;
		}

		task.cancel();
		task = null;
	}

	public boolean isRunning() {
		return task != null;
	}

	@Override
	public void accept(Task t) {
		if (Sponge.getServer().getOnlinePlayers().isEmpty()) {
			cancel();
			return;
		}

		for (Map.Entry<String, TabPlayer> map : tabPlayers.entrySet()) {
			TabPlayer v = map.getValue();
			if (v.update()) {
				v.getGroup().ifPresent(g -> g.setTeam(v.getPlayer()));
			}
		}
	}
}
