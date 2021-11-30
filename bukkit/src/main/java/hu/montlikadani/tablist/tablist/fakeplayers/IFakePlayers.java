package hu.montlikadani.tablist.tablist.fakeplayers;

/**
 * The interface for creating fake players
 */
public interface IFakePlayers {

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
	 * @return the head uuid of this fake player
	 */
	String getHeadId();

	/**
	 * Gets the fake player ping current set of latency.
	 * 
	 * @return the amount of latency
	 */
	int getPingLatency();

	/**
	 * Attempts to create a fake player with custom head id and a specific amount of
	 * ping.
	 * 
	 * @param headId      a valid user uuid
	 * @param pingLatency ping value
	 */
	void createFakePlayer(String headId, int pingLatency);

	/**
	 * Attempts to set the fake player ping to a new one. If the fake player is not
	 * added before, returns.
	 * 
	 * @param pingAmount ping value
	 */
	void setPing(int pingAmount);

	/**
	 * Attempts to set the valid user skin uuid to player list for fake player
	 * before their name.
	 * 
	 * @param skinId a valid user skin uuid
	 */
	void setSkin(java.util.UUID skinId);

	/**
	 * Attempts to remove an added fake player.
	 */
	void removeFakePlayer();

}
