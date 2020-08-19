package hu.montlikadani.tablist.sponge.tablist.groups;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.function.Consumer;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;

import hu.montlikadani.tablist.sponge.TabList;

public class GroupTask implements Consumer<Task> {

	private final HashMap<String, TabPlayer> tabPlayers = new HashMap<>();
	private final LinkedList<TabPlayer> sortedTabPlayers = new LinkedList<>();

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
		tabPlayers.put(uuid, tabPlayer);
		addToTabListPlayerList(tabPlayer);

		int priority = 0;
		for (TabPlayer tabPl : sortedTabPlayers) {
			if (!tabPl.getGroup().isPresent())
				continue;

			tabPl.getGroup().get().setTeam(tabPl.getPlayerUUID(), priority);
			priority++;
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

		for (TabPlayer p : sortedTabPlayers) {
			if (tlp.compareTo(p) < 0)
				break;

			pos++;
		}

		//if (pos >= 0 && pos <= sortedTabPlayers.size()) {
			sortedTabPlayers.add(pos, tlp);
		//}
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

		int priority = 0;
		for (TabPlayer tabPl : sortedTabPlayers) {
			if (!tabPl.getGroup().isPresent())
				continue;

			tabPl.getGroup().get().setTeam(tabPl.getPlayerUUID(), priority);
			priority++;
		}
	}
}
