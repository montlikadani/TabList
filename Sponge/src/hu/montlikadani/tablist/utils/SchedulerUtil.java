package hu.montlikadani.tablist.utils;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.scheduler.Task;

public abstract class SchedulerUtil {

	public static ScheduledTask submitScheduleSyncTask(long delay, TimeUnit timeUnit, Consumer<ScheduledTask> execute) {
		return Sponge.getServer().getScheduler()
				.submit(Task.builder().delay(delay, timeUnit).execute(execute::accept).build());
	}

	public static ScheduledTask submitScheduleAsyncTask(long interval, TimeUnit timeUnit,
			Consumer<ScheduledTask> execute) {
		return Sponge.getServer().getGame().getAsyncScheduler()
				.submit(Task.builder().interval(interval, timeUnit).execute(execute::accept).build());
	}
}
