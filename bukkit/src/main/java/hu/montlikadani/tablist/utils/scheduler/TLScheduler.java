package hu.montlikadani.tablist.utils.scheduler;

public interface TLScheduler {

    TLScheduler submitAsync(Runnable task, long initialDelay, long period);

    void runDelayed(Runnable task, org.bukkit.Location location, long delay);

    void submitSync(Runnable runnable);

    void runTaskAsynchronously(Runnable task);

    void cancelTask();

}
