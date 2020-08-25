package hu.montlikadani.tablist.bukkit.tablist;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public interface ITabHandler {

	Player getPlayer();

	UUID getPlayerUUID();

	TabBuilder getBuilder();

	BukkitTask getTask();
}
