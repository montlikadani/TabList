package hu.montlikadani.tablist.tablist;

import org.bukkit.scheduler.BukkitTask;

import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.config.constantsLoader.TabConfigValues;
import hu.montlikadani.tablist.user.TabListUser;
import hu.montlikadani.tablist.utils.task.Tasks;

public final class TabManager {

	private final TabList plugin;
	private final TabToggleBase toggleBase;

	private BukkitTask task;

	public TabManager(TabList plugin) {
		this.plugin = plugin;
		this.toggleBase = new TabToggleBase(plugin);
	}

	public TabToggleBase getToggleBase() {
		return toggleBase;
	}

	public BukkitTask getTask() {
		return task;
	}

	public void cancelTask() {
		if (task != null) {
			task.cancel();
			task = null;
		}
	}

	public void addPlayer(TabListUser user) {
		if (!TabConfigValues.isEnabled()) {
			return;
		}

		user.getTabHandler().loadTabComponents();

		final int refreshTime = TabConfigValues.getUpdateInterval();
		if (refreshTime < 1) {
			user.getTabHandler().sendTab();
			return;
		}

		if (task == null) {
			task = Tasks.submitAsync(() -> {
				if (plugin.getUsers().isEmpty()) {
					cancelTask();
					return;
				}

				for (TabListUser u : plugin.getUsers()) {
					u.getTabHandler().sendTab();
				}
			}, refreshTime, refreshTime);
		}
	}

	public void removeAll() {
		cancelTask();

		for (TabListUser user : plugin.getUsers()) {
			user.getTabHandler().sendEmptyTab(user.getPlayer());
		}
	}
}
