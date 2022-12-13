package hu.montlikadani.tablist.user;

/**
 * The interface which holds the users recognised by TabList resource.
 */
public interface TabListUser {

	/**
	 * @return {@link org.bukkit.entity.Player}
	 */
	org.bukkit.entity.Player getPlayer();

	/**
	 * @return true if the tablist for this player is visible
	 */
	boolean isTabVisible();

	/**
	 * Sets the tablist visibility for this player.
	 * 
	 * @param visibility true if visible, false otherwise
	 */
	void setTabVisibility(boolean visibility);

	/**
	 * @return true if this player is hidden from other players
	 */
	boolean isHidden();

	/**
	 * Makes this player hidden from other players.
	 * 
	 * @param hidden true to hide, false to make it visible
	 */
	void setHidden(boolean hidden);

	/**
	 * @return true if this player removed from player list
	 */
	boolean isRemovedFromPlayerList();

	/**
	 * Removes this player from the player list. This method including {@link #addToPlayerList()} should be avoided from
	 * calling as it might produce client side issues.
	 */
	void removeFromPlayerList();

	/**
	 * Adds this player back to the player list. This method including {@link #removeFromPlayerList()} should be avoided
	 * from calling as it might produce client side issues.
	 */
	void addToPlayerList();

	/**
	 * @return the {@link java.util.UUID} of this player
	 */
	java.util.UUID getUniqueId();

	/**
	 * @return {@link hu.montlikadani.tablist.tablist.groups.GroupPlayer}
	 */
	hu.montlikadani.tablist.tablist.groups.GroupPlayer getGroupPlayer();

	/**
	 * @return {@link hu.montlikadani.tablist.tablist.TabHandler}
	 */
	hu.montlikadani.tablist.tablist.TabHandler getTabHandler();

	/**
	 * @return {@link PlayerScore}
	 */
	PlayerScore getPlayerScore();


}
