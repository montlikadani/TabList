package hu.montlikadani.tablist.bukkit.tablist.fakeplayers;

import org.bukkit.entity.Player;

/**
 * The interface for creating fake players
 */
public interface IFakePlayers {

	/**
	 * Gets the fake player name.
	 * 
	 * @return the name of the fake player
	 */
	String getName();

	/**
	 * Gets the fake player ping current set of latency.
	 * 
	 * @return the amount of latency
	 */
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
	 * @param headUUID    an uuid of valid user
	 * @param pingLatency ping value (> 0)
	 */
	void createFakePlayer(Player p, String headUUID, int pingLatency);

	/**
	 * Attempts to set the fake player ping to a new one. If the fake player is not
	 * added before, returns.
	 * 
	 * @param pingAmount ping value (> 0)
	 */
	void setPing(int pingAmount);

	/**
	 * Attempts to set the valid user skin uuid to player list for fake player
	 * before their name.
	 * 
	 * @param skinUUID an valid user skin uuid
	 */
	void setSkin(String skinUUID);

	/**
	 * Attempts to remove an added fake player.
	 */
	void removeFakePlayer();
}
