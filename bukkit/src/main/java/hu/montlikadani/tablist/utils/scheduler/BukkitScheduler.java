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
    public void runDelayed(Runnable task, org.bukkit.Location location, long delay) {
        plugin.getServer().getScheduler().runTaskLater(plugin, task, delay);
    }

    @Override
    public void submitSync(Runnable runnable) {
        if (plugin.isEnabled() && !plugin.getServer().isPrimaryThread()) {
            plugin.getServer().getScheduler().runTask(plugin, runnable);
        }
    }

    @Override
    public void runTaskAsynchronously(Runnable task) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, task);
    }

    @Override
    public void cancelTask() {
        if (bukkitTask != null) {
            bukkitTask.cancel();
        }
    }
}
