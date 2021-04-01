package hu.montlikadani.tablist.bukkit.user;

import org.bukkit.entity.Player;

import hu.montlikadani.tablist.bukkit.tablist.TabHandler;
import hu.montlikadani.tablist.bukkit.tablist.groups.GroupPlayer;

public interface TabListUser {

	Player getPlayer();

	boolean isHidden();

	void setHidden(boolean hidden);

	boolean isRemovedFromPlayerList();

	void removeFromPlayerList();

	void addToPlayerList();

	java.util.UUID getUniqueId();

	GroupPlayer getGroupPlayer();

	TabHandler getTabHandler();

}
