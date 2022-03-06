package hu.montlikadani.tablist.utils;

import java.util.function.Consumer;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.util.Ticks;

public abstract class SchedulerUtil {

	private static final org.spongepowered.plugin.PluginContainer TL = Sponge.pluginManager().plugin("tablist").get();

	public static ScheduledTask submitScheduleSyncTask(long delay, java.time.temporal.TemporalUnit unit,
			Consumer<ScheduledTask> execute) {
		return Sponge.server().scheduler().submit(Task.builder().plugin(TL).delay(delay, unit).execute(execute::accept).build());
	}

	public static ScheduledTask submitScheduleAsyncTask(long interval, Consumer<ScheduledTask> execute) {
		return Sponge.server().game().asyncScheduler()
				.submit(Task.builder().plugin(TL).interval(Ticks.of(interval)).execute(execute::accept).build());
	}
}
