package hu.montlikadani.tablist.bukkit.tablist.entry.row;

import java.util.Optional;
import java.util.UUID;

import org.bukkit.entity.Player;

/**
 * The interface for creating rows for tablist
 */
public interface IRowPlayer {

	/**
	 * Attempts to create this player to the given column and row index.
	 * 
	 * @param rowIndex the index of row where to set
	 */
	void create(int rowIndex);

	/**
	 * Attempts to remove this player from the given column and row index.
	 */
	void remove();

	/**
	 * @return the {@link Player} of this column if present
	 */
	Optional<Player> asPlayer();

	/**
	 * @return the text of this row
	 */
	String getText();

	/**
	 * Returns the current set of ping latency of this column.
	 * 
	 * @return the amount of latency
	 */
	int getPingLatency();

	/**
	 * Returns the current set of head id.
	 * 
	 * @return head id
	 */
	UUID getHeadId();

	/**
	 * Sets the given player to this row.
	 * 
	 * @param player {@link Player}
	 */
	void setPlayer(Player player);

	/**
	 * @param text the new text of this row
	 */
	void setText(String text);

	/**
	 * @param player {@link Player} where to update
	 * @param text updates the text without modifying {@link #getText()}
	 * @return the updated text
	 */
	String updateText(Player player, String text);

	/**
	 * Attempts to set this column ping to a new one.
	 * 
	 * @param ping value of this row (> 0)
	 */
	void setPing(int ping);

	/**
	 * Attempts to set the valid user skin uuid to player list for this column
	 * before their name.
	 * 
	 * @param skinId a valid user skin uuid
	 */
	void setSkin(UUID skinId);
}
