package hu.montlikadani.tablist.utils.scheduler;

public interface TLScheduler {

    TLScheduler submitAsync(Runnable task, long initialDelay, long period);

    <V> V submitSync(java.util.function.Supplier<V> sup);

    void cancelTask();

}
