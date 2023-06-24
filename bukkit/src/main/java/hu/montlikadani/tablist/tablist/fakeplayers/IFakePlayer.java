package hu.montlikadani.tablist.tablist.fakeplayers;

import java.util.UUID;

import com.mojang.authlib.GameProfile;
import hu.montlikadani.tablist.utils.PlayerSkinProperties;

/**
 * The interface for creating fake players
 */
public interface IFakePlayer {

	static FakePlayerBuilder builder() {
		return new FakePlayerBuilder();
	}

	/**
	 * Gets the fake player name.
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
	 * Gets the display name of this fake player.
	 * 
	 * @return display name of fake player
	 */
	String getDisplayName();

	/**
	 * Sets the display name of this fake player and re-sends.
	 * 
	 * @param displayName the new name to appear
	 */
	void setDisplayName(String displayName);

	/**
	 * @return the profile properties of this fake player
	 */
	hu.montlikadani.tablist.utils.PlayerSkinProperties profileProperties();

	/**
	 * Gets the fake player' ping. This does not change each time, as this fake
	 * player is not an existing player, it can change if it is changed by a
	 * repeating thread.
	 * 
	 * @return the amount of ping latency
	 */
	int getPingLatency();

	/**
	 * @return the {@link GameProfile} of this fake player
	 */
	GameProfile getProfile();

	/**
	 * Shows the fake player, if not exist it will creates a new one with the
	 * properties set (name, display name) and sends to the client.
	 */
	void show();

	/**
	 * Sets the ping value to a new one for this fake player and re-sends to appear
	 * immediately in tablist. The fake player must exists, otherwise returns.
	 * 
	 * @param ping the new value of ping
	 */
	void setPing(int ping);

	/**
	 * Sets a valid user skin for this fake player. You need to re-send
	 * the fake player to apply the skin.
	 *
	 * @param skinProperties an instance of {@link PlayerSkinProperties} containing player's skin data
	 */
	void setSkin(PlayerSkinProperties skinProperties);

	/**
	 * Removes this fake player from the tab if present.
	 */
	void remove();

	final class FakePlayerBuilder {

		private String name = "", displayName = "", headIdentifier;
		private int ping = -1;

		public FakePlayerBuilder name(String name) {
			this.name = name == null ? "" : name;
			return this;
		}

		public FakePlayerBuilder displayName(String displayName) {
			this.displayName = displayName == null ? "" : displayName;
			return this;
		}

		public FakePlayerBuilder headIdentifier(UUID headIdentifier) {
			if (headIdentifier != null) {
				this.headIdentifier = headIdentifier.toString();
			}

			return this;
		}

		public FakePlayerBuilder ping(int ping) {
			if (ping < -1) {
				ping = -1;
			}

			this.ping = ping;
			return this;
		}

		public IFakePlayer build() {
			return new FakePlayer(name, displayName, headIdentifier, ping);
		}
	}
}
