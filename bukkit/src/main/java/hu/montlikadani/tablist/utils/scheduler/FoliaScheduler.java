package hu.montlikadani.tablist.utils.scheduler;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class FoliaScheduler implements TLScheduler {

    private final Plugin plugin;
    private ScheduledTask scheduledTask;

    public FoliaScheduler(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public TLScheduler submitAsync(Runnable task, long initialDelay, long period) {
        scheduledTask = plugin.getServer().getAsyncScheduler().runAtFixedRate(plugin, consumer -> task.run(), initialDelay, period, TimeUnit.MILLISECONDS);
        return this;
    }

    @Override
    public <V> V submitSync(Supplier<V> supplier) {
        return supplier.get();
    }

    @Override
    public void cancelTask() {
        if (scheduledTask != null) {
            scheduledTask.cancel();
        }
    }
}