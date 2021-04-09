package hu.montlikadani.tablist.bukkit.tablist.fakeplayers;

import java.util.Optional;
import java.util.UUID;

import org.bukkit.entity.Player;

import hu.montlikadani.tablist.bukkit.tablist.entry.row.IRowPlayer;

/**
 * The interface for creating fake players
 */
public interface IFakePlayers extends IRowPlayer {

	/**
	 * Gets the fake player name.
	 * <p>
	 * This should <b>not be confused</b> with display name!
	 * 
	 * @return the name of the fake player
	 */
	String getName();

	/**
	 * Sets the name of this fake player.
	 * 
	 * @param name the new name
	 */
	void setName(String name);

	/**
	 * Returns the identifier of this fake player.
	 * 
	 * @return the id of fake player
	 */
	int getId();

	/**
	 * Gets the display name of the fake player.
	 * 
	 * @return display name of fake player
	 */
	String getDisplayName();

	/**
	 * Sets the display name of the fake player.
	 * 
	 * @param displayName the new name
	 */
	void setDisplayName(String displayName);

	/**
	 * Gets the fake player ping current set of latency.
	 * 
	 * @return the amount of latency
	 */
	@Override
	int getPingLatency();

	/**
	 * Attempts to create a fake player for the given player.
	 * 
	 * @param p {@link Player}
	 * @see #createFakePlayer(Player, int)
	 */
	void createFakePlayer(Player p);

	/**
	 * Attempts to create a fake player for the given player and sets the ping
	 * latency.
	 * 
	 * @param p           {@link Player}
	 * @param pingLatency ping value (> 0)
	 * @see #createFakePlayer(Player, String, int)
	 */
	void createFakePlayer(Player p, int pingLatency);

	/**
	 * Attempts to create a fake player for the given player, setting their head
	 * skin from uuid and ping.
	 * 
	 * @param p           {@link Player}
	 * @param headId    an uuid of valid user
	 * @param pingLatency ping value (> 0)
	 */
	void createFakePlayer(Player p, String headId, int pingLatency);

	/**
	 * Attempts to set the fake player ping to a new one. If the fake player is not
	 * added before, returns.
	 * 
	 * @param pingAmount ping value (> 0)
	 */
	@Override
	void setPing(int pingAmount);

	/**
	 * Attempts to set the valid user skin uuid to player list for fake player
	 * before their name.
	 * 
	 * @param skinId an valid user skin uuid
	 */
	@Override
	void setSkin(UUID skinId);

	/**
	 * Attempts to remove an added fake player.
	 */
	void removeFakePlayer();

	@Override
	default void create(int rowIndex) {
		throw new UnsupportedOperationException("Use #createFakePlayer instead");
	}

	@Override
	default void remove() {
		throw new UnsupportedOperationException("Use #removeFakePlayer instead");
	}

	@Override
	default Optional<Player> asPlayer() {
		throw new UnsupportedOperationException("#asPlayer not supported");
	}

	@Override
	default void setPlayer(Player player) {
		throw new UnsupportedOperationException("#setPlayer not supported");
	}

	@Override
	default String getText() {
		throw new UnsupportedOperationException("#getText not supported");
	}

	@Override
	default String updateText(Player player, String text) {
		throw new UnsupportedOperationException("#updateText not supported");
	}

	@Override
	default void setText(String text) {
		throw new UnsupportedOperationException("#setText not supported");
	}

	@Override
	default UUID getHeadId() {
		throw new UnsupportedOperationException("#getHeadId not supported");
	}
}
