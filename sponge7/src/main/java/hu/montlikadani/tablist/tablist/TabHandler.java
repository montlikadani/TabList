package hu.montlikadani.tablist.tablist;

import org.spongepowered.api.scheduler.Task;

import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.config.ConfigValues;
import hu.montlikadani.tablist.user.TabListUser;

public class TabHandler {

	public static final java.util.Map<java.util.UUID, Boolean> TABENABLED = new java.util.HashMap<>();

	private TabList plugin;
	private Task task;

	public TabHandler(TabList plugin) {
		this.plugin = plugin;
	}

	public void cancelTask() {
		if (task != null) {
			task.cancel();
			task = null;
		}
	}

	public void addPlayer(TabListUser user) {
		if (!ConfigValues.isTablistEnabled()) {
			return;
		}

		user.getTabListManager().loadTab();

		final int refreshTime = ConfigValues.getTablistUpdateTime();
		if (refreshTime < 1) {
			user.getTabListManager().sendTab();
			return;
		}

		if (task == null) {
			task = Task.builder().async().intervalTicks(refreshTime).execute(task -> {
				if (plugin.getTabUsers().isEmpty()) {
					cancelTask();
					return;
				}

				for (TabListUser us : plugin.getTabUsers()) {
					us.getTabListManager().sendTab();
				}
			}).submit(plugin);
		}
	}

	public void removeAll() {
		cancelTask();

		for (TabListUser user : plugin.getTabUsers()) {
			user.getTabListManager().clearTab();
		}
	}
}
