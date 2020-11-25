package hu.montlikadani.tablist.bungee.tablist;

import net.md_5.bungee.api.scheduler.ScheduledTask;

public interface ITask {

	void start();

	ScheduledTask getTask();

	void cancel();
}
