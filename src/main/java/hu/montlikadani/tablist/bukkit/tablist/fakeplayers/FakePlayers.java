package hu.montlikadani.tablist.bukkit.tablist.fakeplayers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.URL;
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

public class FakePlayers implements IFakePlayers {

	private String name;

	private Object fakePl;
	private GameProfile profile;
	private Class<?> enumPlayerInfoAction;

	public FakePlayers(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void createFakeplayer(Player p) {
		createFakeplayer(p, "");
	}

	@Override
	public void createFakeplayer(Player p, String headUUID) {
		try {
			profile = new GameProfile(UUID.randomUUID(), name);

			if (headUUID != null && !headUUID.trim().isEmpty() && Bukkit.getServer().getOnlineMode()) {
				getSkinValue(headUUID).thenAcceptAsync((map) -> {
					java.util.Map.Entry<String, String> e = map.pollFirstEntry();
					profile.getProperties().get("textures").clear();
					profile.getProperties().put("textures", new Property("textures", e.getKey(), e.getValue()));
				});
			}

			fakePl = ReflectionUtils.Classes.getPlayerConstructor(p, profile);

			ReflectionUtils.setField(fakePl, "listName", ReflectionUtils.getAsIChatBaseComponent(profile.getName()));

			enumPlayerInfoAction = ReflectionUtils.Classes.getEnumPlayerInfoAction();

			Object entityPlayerArray = Array.newInstance(fakePl.getClass(), 1);
			Array.set(entityPlayerArray, 0, fakePl);

			Object packetPlayOutPlayerInfo = ReflectionUtils.getNMSClass("PacketPlayOutPlayerInfo")
					.getConstructor(enumPlayerInfoAction, entityPlayerArray.getClass()).newInstance(ReflectionUtils
							.getFieldObject(enumPlayerInfoAction, enumPlayerInfoAction.getDeclaredField("ADD_PLAYER")),
							entityPlayerArray);

			for (Player aOnline : Bukkit.getOnlinePlayers()) {
				ReflectionUtils.sendPacket(aOnline, packetPlayOutPlayerInfo);
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@Override
	public void removeFakePlayer() {
		try {
			ReflectionUtils.setField(fakePl, "listName", ReflectionUtils.getAsIChatBaseComponent(profile.getName()));

			Object entityPlayerArray = Array.newInstance(fakePl.getClass(), 1);
			Array.set(entityPlayerArray, 0, fakePl);

			Object packetPlayOutPlayerInfo = ReflectionUtils.getNMSClass("PacketPlayOutPlayerInfo")
					.getConstructor(enumPlayerInfoAction, entityPlayerArray.getClass())
					.newInstance(ReflectionUtils.getFieldObject(enumPlayerInfoAction,
							enumPlayerInfoAction.getDeclaredField("REMOVE_PLAYER")), entityPlayerArray);

			for (Player aOnline : Bukkit.getOnlinePlayers()) {
				ReflectionUtils.sendPacket(aOnline, packetPlayOutPlayerInfo);
			}
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
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}
}