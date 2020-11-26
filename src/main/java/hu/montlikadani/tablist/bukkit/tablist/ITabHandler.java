package hu.montlikadani.tablist.bukkit.tablist;

import java.util.UUID;

import org.bukkit.entity.Player;

import hu.montlikadani.tablist.bukkit.tablist.tabentries.TabEntry;

public interface ITabHandler {

	Player getPlayer();

	UUID getPlayerUUID();

	TabBuilder getBuilder();

	TabEntry getTabEntry();
}
