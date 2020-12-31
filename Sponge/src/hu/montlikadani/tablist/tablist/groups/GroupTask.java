package hu.montlikadani.tablist.tablist.groups;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.spongepowered.api.scheduler.ScheduledTask;

import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.player.ITabPlayer;
import hu.montlikadani.tablist.utils.SchedulerUtil;

public final class GroupTask implements Consumer<ScheduledTask> {

	private final HashMap<String, TabUser> tabUsers = new HashMap<>();
	private final List<TabUser> sortedTabUsers = Collections.synchronizedList(new LinkedList<TabUser>());

	private ScheduledTask task;

	public Optional<ScheduledTask> getTask() {
		return Optional.ofNullable(task);
	}

	public HashMap<String, TabUser> getTabUsers() {
		return tabUsers;
	}

	public Optional<TabUser> getTabUser(ITabPlayer player) {
		return player != null ? Optional.ofNullable(tabUsers.get(player.getPlayerUUID().toString())) : Optional.empty();
	}

	public TabUser addPlayer(ITabPlayer player) {
		String uuid = player.getPlayerUUID().toString();

		if (tabUsers.containsKey(uuid)) {
			return tabUsers.get(uuid);
		}

		TabUser tabUser = new TabUser(player.getPlayerUUID());
		tabUser.updateGroup();
		tabUsers.put(uuid, tabUser);
		addToTabListPlayerList(tabUser);

		synchronized (sortedTabUsers) {
			int priority = 0;
			for (TabUser tabUs : sortedTabUsers) {
				if (!tabUs.getGroup().isPresent())
					continue;

				tabUs.getGroup().get().setTeam(tabUs.getPlayerUUID(), priority);
				priority++;
			}
		}

		return tabUser;
	}

	public void removePlayer(ITabPlayer player) {
		TabUser tabU = tabUsers.remove(player.getPlayerUUID().toString());
		if (tabU != null) {
			tabU.getGroup().ifPresent(g -> g.removeTeam(tabU));
			sortedTabUsers.remove(tabU);
		}
	}

	private void addToTabListPlayerList(TabUser tlp) {
		int pos = 0;

		synchronized (sortedTabUsers) {
			for (TabUser p : sortedTabUsers) {
				if (tlp.compareTo(p) < 0)
					break;

				pos++;
			}
		}

		sortedTabUsers.add(pos, tlp);
	}

	public void runTask() {
		if (!isRunning()) {
			task = SchedulerUtil.submitScheduleAsyncTask(4, TimeUnit.MILLISECONDS, this::accept);
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
	public void accept(ScheduledTask t) {
		if (TabList.get().getTabPlayers().isEmpty()) {
			return;
		}

		for (ITabPlayer tabPlayer : TabList.get().getTabPlayers()) {
			TabUser tp = tabUsers.get(tabPlayer.getPlayerUUID().toString());
			if (tp == null) {
				tp = new TabUser(tabPlayer.getPlayerUUID());

				tabUsers.put(tabPlayer.getPlayerUUID().toString(), tp);

				tp.updateGroup();
				addToTabListPlayerList(tp);
			} else if (tp.updateGroup()) {
				sortedTabUsers.remove(tp);
				addToTabListPlayerList(tp);
			}
		}

		synchronized (sortedTabUsers) {
			int priority = 0;
			for (TabUser tabPl : sortedTabUsers) {
				if (!tabPl.getGroup().isPresent())
					continue;

				tabPl.getGroup().get().setTeam(tabPl.getPlayerUUID(), priority);
				priority++;
			}
		}
	}
}
