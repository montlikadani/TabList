package hu.montlikadani.tablist.bukkit.tablist;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public interface ITabHandler {

	Player getPlayer();

	TabBuilder getBuilder();

	BukkitTask getTask();
}
