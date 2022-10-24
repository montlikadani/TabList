package hu.montlikadani.tablist.user;

import hu.montlikadani.tablist.tablist.fakeplayers.IFakePlayer;

/**
 * The interface which holds the users recognised by TabList resource.
 */
public interface TabListUser {

	/**
	 * @return {@link org.bukkit.entity.Player}
	 */
	org.bukkit.entity.Player getPlayer();

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

	/**
	 * Checks if the specified {@link IFakePlayer} is visible to this player.
	 * 
	 * @param fakePlayer {@link IFakePlayer} to check
	 * @return true if the specified fake player is visible to this player
	 */
	boolean isFakePlayerVisible(IFakePlayer fakePlayer);

	/**
	 * Makes the specified {@link IFakePlayer} visible for this player.
	 * 
	 * @param fakePlayer {@link IFakePlayer}
	 */
	void setCanSeeFakePlayer(IFakePlayer fakePlayer);

	/**
	 * Clears the visible fake players cache to make it appear for this player.
	 */
	void removeAllVisibleFakePlayer();

}
