package hu.montlikadani.tablist.tablist;

import org.bukkit.scheduler.BukkitTask;

import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.config.constantsLoader.TabConfigValues;
import hu.montlikadani.tablist.user.TabListUser;
import hu.montlikadani.tablist.utils.task.Tasks;

public final class TabManager {

	private transient final TabList plugin;
	public transient final TabToggleBase toggleBase;

	private BukkitTask task;

	public TabManager(TabList plugin) {
		this.plugin = plugin;
		this.toggleBase = new TabToggleBase(plugin);
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
				if (plugin.performanceIsUnderValue() || plugin.getUsers().isEmpty()) {
					cancelTask();
					return;
				}

				for (TabListUser u : plugin.getUsers()) {
					u.getTabHandler().sendTab();
				}
			}, 0, refreshTime);
		}
	}
}
