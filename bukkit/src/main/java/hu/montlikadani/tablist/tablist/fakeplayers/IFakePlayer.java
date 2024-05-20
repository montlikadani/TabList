package hu.montlikadani.tablist.tablist.fakeplayers;

import java.util.UUID;

import com.mojang.authlib.GameProfile;
import hu.montlikadani.tablist.utils.PlayerSkinProperties;

/**
 * The interface for changing fake player properties
 */
public interface IFakePlayer {

	static FakePlayerBuilder builder() {
		return new FakePlayerBuilder();
	}

	/**
	 * @return the name of this fake player
	 */
	String getName();

	/**
	 * Sets the new name of this fake player
	 * 
	 * @param name the new name
	 */
	void setName(String name);

	/**
	 * @return display name of this fake player
	 */
	String getDisplayName();

	/**
	 * Sets the display name of this fake player
	 * 
	 * @param displayName the new display name
	 */
	void setDisplayName(String displayName);

	/**
	 * @return the profile properties of this fake player
	 */
	hu.montlikadani.tablist.utils.PlayerSkinProperties profileProperties();

	/**
	 * Gets the fake player current set of ping (latency) value. This value will
	 * remain unchanged until the {@link #setPing(int)} is not called.
	 * 
	 * @return the amount of ping latency
	 */
	int getPingLatency();

	/**
	 * @return the {@link GameProfile} of this fake player
	 */
	GameProfile getProfile();

	/**
	 * Shows the fake player, if not exist it will create a new one with the
	 * properties set (name, display name) and sends to the client.
	 */
	void show();

	/**
	 * Sets the ping (latency) value for this fake player and updates the player
	 * list to appear next to this fake player name.
	 * 
	 * @param ping the new value of ping
	 */
	void setPing(int ping);

	/**
	 * Sets a valid user skin for this fake player to append the head icon in player
	 * list next to the name. To apply the skin execute {@link #show()} method
	 * once.
	 * <p>
	 * The skin icon to appear your server needs to be online {@link org.bukkit.Bukkit#getOnlineMode()}
	 * <p>
	 * To create a {@link PlayerSkinProperties} instance you can use these 3 options:
	 * <p>1. Using DataFetcher to retrieve a specified player's data from an online web service</p>
	 * <blockquote><pre>
	 *     hu.montlikadani.tablist.utils.datafetcher.URLDataFetcher.fetchProfile("exampleNameOrUUID")
	 *        .thenAccept(properties -> {
	 *     	    String raw = properties.textureRawValue;
	 *     	    String decoded = properties.decodedTextureValue;
	 *
	 *     	    // your code
	 *       });
	 * </blockquote></pre>
	 *
	 * <p>2. Using {@link PlayerSkinProperties} constructor to create texture data for specific player</p>
	 * <blockquote><pre>
	 *     PlayerSkinProperties properties = new PlayerSkinProperties(playerName, playerId, textureRawValue,
	 *     	decodedTextureValue);
	 * </pre></blockquote>
	 *
	 * <p>3. Retrieving from cache if it is stored by this plugin, {@code null} if no player found</p>
	 * <blockquote><pre>
	 *     PlayerSkinProperties properties = PlayerSkinProperties.findPlayerProperty(playerName, playerId);
	 * </pre></blockquote>
	 *
	 * @param skinProperties an instance of {@link PlayerSkinProperties}
	 *                       containing player skin data
	 */
	void setSkin(PlayerSkinProperties skinProperties);

	/**
	 * Removes this fake player from the player list if present.
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
