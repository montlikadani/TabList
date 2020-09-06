package hu.montlikadani.tablist.bukkit.tablist.groups;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.codec.binary.Base64;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import hu.montlikadani.tablist.bukkit.TabListPlayer;
import hu.montlikadani.tablist.bukkit.utils.ReflectionUtils;
import hu.montlikadani.tablist.bukkit.utils.ServerVersion.Version;

public class ReflectionHandled implements ITabScoreboard {

	private final TabScoreboardReflection scoreRef = new TabScoreboardReflection();

	private Object packet, playerConst, entityPlayerArray, packetPlayOutPlayerInfo;
	private GameProfile profile;

	private TabListPlayer tabPlayer;

	public ReflectionHandled(TabListPlayer tabPlayer) {
		this.tabPlayer = tabPlayer;
	}

	@Override
	public TabListPlayer getTabPlayer() {
		return tabPlayer;
	}

	@Override
	public void registerTeam(String teamName) {
		if (packet != null) {
			return;
		}

		profile = new GameProfile(tabPlayer.getPlayer().getUniqueId(), tabPlayer.getPlayer().getName());

		if (Bukkit.getServer().getOnlineMode()) {
			getSkinValue(tabPlayer.getPlayer().getUniqueId().toString()).thenAcceptAsync((map) -> {
				Entry<String, String> e = map.pollFirstEntry();
				profile.getProperties().get("textures").clear();
				profile.getProperties().put("textures", new Property("textures", e.getKey(), e.getValue()));
			});
		}

		playerConst = ReflectionUtils.Classes.getPlayerConstructor(tabPlayer.getPlayer(), profile);

		try {
			scoreRef.init();

			packet = scoreRef.getScoreboardTeamConstructor().newInstance();

			scoreRef.getScoreboardTeamName().set(packet, teamName);
			scoreRef.getScoreboardTeamDisplayName().set(packet,
					Version.isCurrentEqualOrHigher(Version.v1_13_R1) ? ReflectionUtils.getAsIChatBaseComponent(teamName)
							: teamName);

			scoreRef.getScoreboardTeamNames().set(packet, Collections.singletonList(tabPlayer.getPlayer().getName()));
			scoreRef.getScoreboardTeamMode().set(packet, 0);

			ReflectionUtils.setField(playerConst, "listName", ReflectionUtils.getAsIChatBaseComponent(
					tabPlayer.getPrefix() + tabPlayer.getPlayerName() + tabPlayer.getSuffix()));

			entityPlayerArray = Array.newInstance(playerConst.getClass(), 1);
			Array.set(entityPlayerArray, 0, playerConst);

			Class<?> enumPlayerInfoAction = ReflectionUtils.Classes.getEnumPlayerInfoAction();
			packetPlayOutPlayerInfo = ReflectionUtils.getNMSClass("PacketPlayOutPlayerInfo")
					.getConstructor(enumPlayerInfoAction, entityPlayerArray.getClass())
					.newInstance(ReflectionUtils.getFieldObject(enumPlayerInfoAction,
							enumPlayerInfoAction.getDeclaredField("UPDATE_DISPLAY_NAME")), entityPlayerArray);

			for (Player p : Bukkit.getOnlinePlayers()) {
				ReflectionUtils.sendPacket(p, packet);
				ReflectionUtils.sendPacket(p, packetPlayOutPlayerInfo);
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@Override
	public void setTeam(String teamName) {
		if (packet == null) {
			registerTeam(teamName);
		}

		try {
			scoreRef.getScoreboardTeamDisplayName().set(packet,
					Version.isCurrentEqualOrHigher(Version.v1_13_R1) ? ReflectionUtils.getAsIChatBaseComponent(teamName)
							: teamName);
			scoreRef.getScoreboardTeamMode().set(packet, 2);

			updateName(tabPlayer.getPrefix() + tabPlayer.getPlayerName() + tabPlayer.getSuffix());

			for (Player p : Bukkit.getOnlinePlayers()) {
				ReflectionUtils.sendPacket(p, packet);
				ReflectionUtils.sendPacket(p, packetPlayOutPlayerInfo);
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@Override
	public void unregisterTeam(String teamName) {
		try {
			scoreRef.getScoreboardTeamMode().set(packet, 1);

			updateName(tabPlayer.getPlayer().getName());

			for (Player p : Bukkit.getOnlinePlayers()) {
				ReflectionUtils.sendPacket(p, packet);
				ReflectionUtils.sendPacket(p, packetPlayOutPlayerInfo);
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	private void updateName(String name) throws Throwable {
		Object iChatBaseComponentName = ReflectionUtils.getAsIChatBaseComponent(name);
		ReflectionUtils.setField(playerConst, "listName", iChatBaseComponentName);

		@SuppressWarnings("unchecked")
		List<Object> infoList = (List<Object>) ReflectionUtils.getField(packetPlayOutPlayerInfo, "b")
				.get(packetPlayOutPlayerInfo);
		for (Object infoData : infoList) {
			if (profile.getId().equals(tabPlayer.getPlayer().getUniqueId())) {
				Field e = ReflectionUtils.getField(infoData, "e");
				ReflectionUtils.modifyFinalField(e, infoData, iChatBaseComponentName);
				break;
			}
		}

		Array.set(entityPlayerArray, 0, playerConst);
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

	@Override
	public Scoreboard getScoreboard() {
		return null;
	}

	@Override
	public void setScoreboard(Scoreboard board) {
	}
}
