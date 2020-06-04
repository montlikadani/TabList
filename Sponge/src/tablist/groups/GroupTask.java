package hu.montlikadani.tablist.sponge.tablist.groups;

import java.util.HashMap;
import java.util.function.Consumer;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;

import hu.montlikadani.tablist.sponge.TabList;

public class GroupTask implements Consumer<Task> {

	private final HashMap<String, TabPlayer> tabPlayers = new HashMap<>();

	private Task task;

	public Task getTask() {
		return task;
	}

	public HashMap<String, TabPlayer> getTabPlayers() {
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

		for (TabPlayer pl : tabPlayers.values()) {
			if (pl.update()) {
				pl.getGroup().ifPresent(g -> g.setTeam(pl.getPlayer()));
			}
		}
	}
}
