package hu.montlikadani.tablist.bukkit.user;

import hu.montlikadani.tablist.bukkit.tablist.TabHandler;
import hu.montlikadani.tablist.bukkit.tablist.groups.GroupPlayer;

public interface TabListUser {

	org.bukkit.entity.Player getPlayer();

	boolean isHidden();

	void setHidden(boolean hidden);

	boolean isRemovedFromPlayerList();

	void removeFromPlayerList();

	void addToPlayerList();

	java.util.UUID getUniqueId();

	GroupPlayer getGroupPlayer();

	TabHandler getTabHandler();

}
