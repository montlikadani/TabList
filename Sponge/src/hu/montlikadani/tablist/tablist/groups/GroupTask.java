package hu.montlikadani.tablist.tablist.groups;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;

import hu.montlikadani.tablist.TabList;

public class GroupTask implements Consumer<Task> {

	private final HashMap<String, TabPlayer> tabPlayers = new HashMap<>();
	private final List<TabPlayer> sortedTabPlayers = Collections.synchronizedList(new LinkedList<TabPlayer>());

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

		TabPlayer tabPlayer = new TabPlayer(player.getUniqueId());
		tabPlayer.update();
		tabPlayers.put(uuid, tabPlayer);
		addToTabListPlayerList(tabPlayer);

		synchronized (sortedTabPlayers) {
			int priority = 0;
			for (TabPlayer tabPl : sortedTabPlayers) {
				if (!tabPl.getGroup().isPresent())
					continue;

				tabPl.getGroup().get().setTeam(tabPl.getPlayerUUID(), priority);
				priority++;
			}
		}

		return tabPlayer;
	}

	public void removePlayer(Player player) {
		TabPlayer tabPlayer = tabPlayers.remove(player.getUniqueId().toString());
		if (tabPlayer != null) {
			tabPlayer.getGroup().ifPresent(g -> g.removeTeam(player));
			sortedTabPlayers.remove(tabPlayer);
		}
	}

	private void addToTabListPlayerList(TabPlayer tlp) {
		int pos = 0;

		synchronized (sortedTabPlayers) {
			for (TabPlayer p : sortedTabPlayers) {
				if (tlp.compareTo(p) < 0)
					break;

				pos++;
			}
		}

		sortedTabPlayers.add(pos, tlp);
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
			// cancel(); // Do not cancel task, due to player respawn
			return;
		}

		for (Player pl : Sponge.getServer().getOnlinePlayers()) {
			TabPlayer tp = tabPlayers.get(pl.getUniqueId().toString());
			if (tp == null) {
				tp = new TabPlayer(pl.getUniqueId());

				tabPlayers.put(pl.getUniqueId().toString(), tp);

				tp.update();
				addToTabListPlayerList(tp);
			} else if (tp.update()) {
				sortedTabPlayers.remove(tp);
				addToTabListPlayerList(tp);
			}
		}

		synchronized (sortedTabPlayers) {
			int priority = 0;
			for (TabPlayer tabPl : sortedTabPlayers) {
				if (!tabPl.getGroup().isPresent())
					continue;

				tabPl.getGroup().get().setTeam(tabPl.getPlayerUUID(), priority);
				priority++;
			}
		}
	}
}
