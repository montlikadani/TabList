package hu.montlikadani.tablist.utils.task;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import org.bukkit.scheduler.BukkitTask;

import hu.montlikadani.tablist.TabList;

public final class Tasks {

	private Tasks() {
	}

	private static final org.bukkit.plugin.Plugin TL = org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(TabList.class);

	public static BukkitTask submitAsync(Runnable task, long delay, long period) {
		return TL.getServer().getScheduler().runTaskTimerAsynchronously(TL, task, delay, period);
	}

	public static <V> V submitSync(Supplier<V> sup) {
		if (TL.isEnabled() && !TL.getServer().isPrimaryThread()) { // Check if current thread is async
			try {
				return TL.getServer().getScheduler().callSyncMethod(TL, sup::get).get();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			} catch (CancellationException e) { // Does not really have other proper solution
				return null;
			}
		}

		return sup.get();
	}
}
