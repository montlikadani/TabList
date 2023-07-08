package hu.montlikadani.tablist.utils.scheduler;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;

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
    public void runDelayed(Runnable task, org.bukkit.Location location, long delay) {
        plugin.getServer().getRegionScheduler().runDelayed(plugin, location, consumer -> task.run(), delay);
    }

    @Override
    public void submitSync(Runnable runnable) {
        plugin.getServer().getGlobalRegionScheduler().execute(plugin, runnable);
    }

    @Override
    public void runTaskAsynchronously(Runnable task) {
        plugin.getServer().getAsyncScheduler().runNow(plugin, schedule -> task.run());
    }

    @Override
    public void cancelTask() {
        if (scheduledTask != null) {
            scheduledTask.cancel();
        }
    }
}
