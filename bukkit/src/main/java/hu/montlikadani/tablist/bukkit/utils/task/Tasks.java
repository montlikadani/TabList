package hu.montlikadani.tablist.bukkit.utils.task;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import hu.montlikadani.tablist.bukkit.API.TabListAPI;

public final class Tasks {

	private Tasks() {
	}

	public static BukkitTask submitAsync(Runnable task, long delay, long period) {
		return Bukkit.getScheduler().runTaskTimerAsynchronously(TabListAPI.getPlugin(), task, delay, period);
	}
}
