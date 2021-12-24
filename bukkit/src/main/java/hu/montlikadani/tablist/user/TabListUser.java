package hu.montlikadani.tablist.user;

public interface TabListUser {

	org.bukkit.entity.Player getPlayer();

	boolean isHidden();

	void setHidden(boolean hidden);

	boolean isRemovedFromPlayerList();

	void removeFromPlayerList();

	void addToPlayerList();

	java.util.UUID getUniqueId();

	hu.montlikadani.tablist.tablist.groups.GroupPlayer getGroupPlayer();

	hu.montlikadani.tablist.tablist.TabHandler getTabHandler();

	PlayerScore getPlayerScore();

}
