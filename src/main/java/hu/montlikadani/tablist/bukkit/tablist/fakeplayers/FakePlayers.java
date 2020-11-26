package hu.montlikadani.tablist.bukkit.tablist.fakeplayers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.codec.binary.Base64;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import hu.montlikadani.tablist.bukkit.utils.ReflectionUtils;
import hu.montlikadani.tablist.bukkit.utils.Util;

public class FakePlayers implements IFakePlayers {

	private String name;

	private int ping = -1;

	private Object fakePl;
	private GameProfile profile;
	private Class<?> enumPlayerInfoAction;
	private Object packetPlayOutPlayerInfo;

	public FakePlayers(String name) {
		this(UUID.randomUUID(), name);
	}

	public FakePlayers(UUID id, String name) {
		if (name == null) {
			name = "";
		}

		if (name.length() > 16) {
			name = name.substring(0, 15);
		}

		this.name = name;

		profile = new GameProfile(id, name);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		if (name == null) {
			name = "";
		}

		if (name.length() > 16) {
			name = name.substring(0, 16);
		}

		this.name = name;

		if (fakePl == null) {
			return;
		}

		try {
			profile = new GameProfile(profile.getId(), name);
			ReflectionUtils.setField(fakePl, "listName", ReflectionUtils.getAsIChatBaseComponent(name));

			Object updateDisplayNamneInfoAction = ReflectionUtils.getFieldObject(enumPlayerInfoAction,
					enumPlayerInfoAction.getDeclaredField("UPDATE_DISPLAY_NAME"));
			if (!ReflectionUtils
					.getFieldObject(packetPlayOutPlayerInfo,
							ReflectionUtils.getField(packetPlayOutPlayerInfo.getClass(), "a"))
					.equals(updateDisplayNamneInfoAction)) {
				Object entityPlayerArray = Array.newInstance(fakePl.getClass(), 1);
				Array.set(entityPlayerArray, 0, fakePl);

				packetPlayOutPlayerInfo = ReflectionUtils.getNMSClass("PacketPlayOutPlayerInfo")
						.getConstructor(enumPlayerInfoAction, entityPlayerArray.getClass()).newInstance(
								ReflectionUtils.getFieldObject(enumPlayerInfoAction,
										enumPlayerInfoAction.getDeclaredField("UPDATE_DISPLAY_NAME")),
								entityPlayerArray);
			}

			for (Player aOnline : Bukkit.getOnlinePlayers()) {
				ReflectionUtils.sendPacket(aOnline, packetPlayOutPlayerInfo);
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@Override
	public int getPingLatency() {
		return ping;
	}

	@Override
	public void createFakePlayer(Player p) {
		createFakePlayer(p, "", -1);
	}

	@Override
	public void createFakePlayer(Player p, int pingLatency) {
		createFakePlayer(p, "", pingLatency);
	}

	@Override
	public void createFakePlayer(Player p, String headUUID, int pingLatency) {
		if (p == null) {
			return;
		}

		try {
			setSkin(headUUID);

			fakePl = ReflectionUtils.Classes.getPlayerConstructor(p, profile);

			ReflectionUtils.setField(fakePl, "listName", ReflectionUtils.getAsIChatBaseComponent(profile.getName()));

			Class<?> packetPlayOutPlayerInfoClass = ReflectionUtils.getNMSClass("PacketPlayOutPlayerInfo");
			enumPlayerInfoAction = ReflectionUtils.Classes.getEnumPlayerInfoAction(packetPlayOutPlayerInfoClass);

			Object entityPlayerArray = Array.newInstance(fakePl.getClass(), 1);
			Array.set(entityPlayerArray, 0, fakePl);

			packetPlayOutPlayerInfo = packetPlayOutPlayerInfoClass
					.getConstructor(enumPlayerInfoAction, entityPlayerArray.getClass()).newInstance(ReflectionUtils
							.getFieldObject(enumPlayerInfoAction, enumPlayerInfoAction.getDeclaredField("ADD_PLAYER")),
							entityPlayerArray);

			for (Player aOnline : Bukkit.getOnlinePlayers()) {
				ReflectionUtils.sendPacket(aOnline, packetPlayOutPlayerInfo);
			}

			// Setting ping should be in this place, after the player added
			setPing(pingLatency);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@Override
	public void setPing(int pingAmount) {
		if (pingAmount < 0) {
			return;
		}

		ping = pingAmount;

		try {
			Object updateLatencyInfoAction = ReflectionUtils.getFieldObject(enumPlayerInfoAction,
					enumPlayerInfoAction.getDeclaredField("UPDATE_LATENCY"));
			if (!ReflectionUtils
					.getFieldObject(packetPlayOutPlayerInfo,
							ReflectionUtils.getField(packetPlayOutPlayerInfo.getClass(), "a"))
					.equals(updateLatencyInfoAction)) {
				Object entityPlayerArray = Array.newInstance(fakePl.getClass(), 1);
				Array.set(entityPlayerArray, 0, fakePl);

				packetPlayOutPlayerInfo = ReflectionUtils.getNMSClass("PacketPlayOutPlayerInfo")
						.getConstructor(enumPlayerInfoAction, entityPlayerArray.getClass())
						.newInstance(updateLatencyInfoAction, entityPlayerArray);
			}

			@SuppressWarnings("unchecked")
			List<Object> infoList = (List<Object>) ReflectionUtils.getField(packetPlayOutPlayerInfo, "b")
					.get(packetPlayOutPlayerInfo);
			for (Object infoData : infoList) {
				Object profile = ReflectionUtils.invokeMethod(infoData, "a");
				Object id = ReflectionUtils.invokeMethod(profile, "getId");
				if (id.equals(this.profile.getId())) {
					ReflectionUtils.modifyFinalField(ReflectionUtils.getField(infoData, "b"), infoData, ping);
					break;
				}
			}

			for (Player aOnline : Bukkit.getOnlinePlayers()) {
				ReflectionUtils.sendPacket(aOnline, packetPlayOutPlayerInfo);
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@Override
	public void setSkin(String skinUUID) {
		if (skinUUID == null || skinUUID.trim().isEmpty() || !Util.isRealUUID(skinUUID)) {
			return;
		}

		if (!Bukkit.getServer().getOnlineMode()) {
			Util.logConsole(java.util.logging.Level.WARNING, "Can't set skin for offline servers.");
			return;
		}

		getSkinValue(skinUUID).thenAcceptAsync((map) -> {
			java.util.Map.Entry<String, String> e = map.pollFirstEntry();
			profile.getProperties().get("textures").clear();
			profile.getProperties().put("textures", new Property("textures", e.getKey(), e.getValue()));
		});
	}

	@Override
	public void removeFakePlayer() {
		if (fakePl == null) {
			return;
		}

		try {
			Object entityPlayerArray = Array.newInstance(fakePl.getClass(), 1);
			Array.set(entityPlayerArray, 0, fakePl);

			packetPlayOutPlayerInfo = ReflectionUtils.getNMSClass("PacketPlayOutPlayerInfo")
					.getConstructor(enumPlayerInfoAction, entityPlayerArray.getClass())
					.newInstance(ReflectionUtils.getFieldObject(enumPlayerInfoAction,
							enumPlayerInfoAction.getDeclaredField("REMOVE_PLAYER")), entityPlayerArray);

			for (Player aOnline : Bukkit.getOnlinePlayers()) {
				ReflectionUtils.sendPacket(aOnline, packetPlayOutPlayerInfo);
			}

			fakePl = null; // Mark as removed and garbaged
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	private final JsonParser parser = new JsonParser();

	private CompletableFuture<NavigableMap<String, String>> getSkinValue(String uuid) {
		return CompletableFuture.supplyAsync(() -> {
			NavigableMap<String, String> map = new TreeMap<>();
			String json = getContent("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);
			if (json == null) {
				return map;
			}

			JsonObject o = parser.parse(json).getAsJsonObject();
			String value = o.get("properties").getAsJsonArray().get(0).getAsJsonObject().get("value").getAsString();

			o = parser.parse(new String(Base64.decodeBase64(value))).getAsJsonObject();
			String texture = o.get("textures").getAsJsonObject().get("SKIN").getAsJsonObject().get("url").getAsString();
			map.put(value, texture);
			return map;
		});
	}

	private String getContent(String link) {
		try {
			HttpsURLConnection conn = (HttpsURLConnection) new URL(link).openConnection();
			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));

			String inputLine;
			while ((inputLine = br.readLine()) != null) {
				return inputLine;
			}

			br.close();
			conn.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}
}