package hu.montlikadani.tablist.bukkit.tablist.entry.row;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.codec.binary.Base64;
import org.bukkit.entity.Player;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The interface for creating columns for tablist
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

	final JsonParser PARSER = new JsonParser();

	default CompletableFuture<NavigableMap<String, String>> getSkinValue(String uuid) {
		return CompletableFuture.supplyAsync(() -> {
			NavigableMap<String, String> map = new TreeMap<>();
			String content = getContent("https://sessionserver.mojang.com/session/minecraft/profile/"
					+ uuid.replace("-", "") + "?unsigned=false");
			if (content == null) {
				return map;
			}

			JsonObject json = PARSER.parse(content).getAsJsonObject();
			String value = json.get("properties").getAsJsonArray().get(0).getAsJsonObject().get("value").getAsString();

			json = PARSER.parse(new String(Base64.decodeBase64(value))).getAsJsonObject();
			String texture = json.get("textures").getAsJsonObject().get("SKIN").getAsJsonObject().get("url")
					.getAsString();
			map.put(value, texture);
			return map;
		});
	}

	default String getContent(String link) {
		try {
			HttpsURLConnection conn = (HttpsURLConnection) new URL(link).openConnection();
			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));

			String inputLine;
			while ((inputLine = br.readLine()) != null) {
				return inputLine;
			}

			br.close();
			conn.disconnect();
		} catch (java.io.IOException e) {
			e.printStackTrace();
		}

		return null;
	}
}
