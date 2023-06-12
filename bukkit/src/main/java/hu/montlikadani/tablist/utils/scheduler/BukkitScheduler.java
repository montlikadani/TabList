package hu.montlikadani.tablist.utils.scheduler;

import org.bukkit.plugin.Plugin;

public final class BukkitScheduler implements TLScheduler {

    private final Plugin plugin;
    private org.bukkit.scheduler.BukkitTask bukkitTask;

    public BukkitScheduler(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public BukkitScheduler submitAsync(Runnable task, long initialDelay, long period) {
        bukkitTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, task, initialDelay, period);
        return this;
    }

    @Override
    public <V> V submitSync(java.util.function.Supplier<V> supplier) {
        if (plugin.isEnabled() && !plugin.getServer().isPrimaryThread()) { // Check if current thread is async
            try {
                return plugin.getServer().getScheduler().callSyncMethod(plugin, supplier::get).get();
            } catch (InterruptedException | java.util.concurrent.ExecutionException e) {
                e.printStackTrace();
            } catch (java.util.concurrent.CancellationException e) {
                return null;
            }
        }

        return supplier.get();
    }

    @Override
    public void cancelTask() {
        if (bukkitTask != null) {
            bukkitTask.cancel();
        }
    }
}
