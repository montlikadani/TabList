package hu.montlikadani.tablist.user;

import hu.montlikadani.tablist.tablist.TabHandler;
import hu.montlikadani.tablist.tablist.groups.GroupPlayer;

public interface TabListUser {

	String getScoreName();

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
