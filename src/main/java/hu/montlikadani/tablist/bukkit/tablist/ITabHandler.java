package hu.montlikadani.tablist.bukkit.tablist;

import java.util.UUID;

import org.bukkit.entity.Player;

public interface ITabHandler {

	Player getPlayer();

	UUID getPlayerUUID();

	TabBuilder getBuilder();
}
