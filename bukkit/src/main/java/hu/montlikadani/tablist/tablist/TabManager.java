package hu.montlikadani.tablist.tablist;

import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.config.constantsLoader.TabConfigValues;
import hu.montlikadani.tablist.user.TabListUser;
import hu.montlikadani.tablist.utils.scheduler.TLScheduler;

public final class TabManager {

	private transient final TabList plugin;
	public transient final TabToggleBase toggleBase;

	private TLScheduler scheduler;

	public TabManager(TabList plugin) {
		this.plugin = plugin;
		this.toggleBase = new TabToggleBase();
	}

	public void cancelTask() {
		if (scheduler != null) {
			scheduler.cancelTask();
			scheduler = null;
		}
	}

	public void load(TabListUser user) {
		if (!TabConfigValues.isEnabled()) {
			return;
		}

		user.getTabHandler().loadTabComponents();

		int refreshTime = TabConfigValues.getUpdateInterval();

		if (refreshTime < 1) {
			user.getTabHandler().sendTab();
			return;
		}

		if (scheduler == null) {
			scheduler = plugin.newTLScheduler().submitAsync(() -> {
				if (plugin.tpsIsUnderValue() || plugin.getUsers().isEmpty()) {
					cancelTask();
					return;
				}

				for (TabListUser one : plugin.getUsers()) {
					one.getTabHandler().sendTab();
				}
			}, 0, refreshTime);
		}
	}
}
