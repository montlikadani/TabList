package hu.montlikadani.tablist.bukkit.tablist.groups;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;

import com.mojang.authlib.GameProfile;

import hu.montlikadani.tablist.bukkit.TabListPlayer;
import hu.montlikadani.tablist.bukkit.utils.ReflectionUtils;

public class ReflectionHandled implements ITabScoreboard {

	private final TabScoreboardReflection scoreRef = new TabScoreboardReflection();

	private Object packet, playerConst, packetPlayOutPlayerInfo;
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
		playerConst = ReflectionUtils.Classes.getPlayerContructor(tabPlayer.getPlayer(), profile);

		try {
			scoreRef.init();

			packet = scoreRef.getScoreboardTeamConstructor().newInstance();

			scoreRef.getScoreboardTeamName().set(packet, teamName);
			scoreRef.getScoreboardTeamDisplayName().set(packet, ReflectionUtils.getAsIChatBaseComponent(teamName));

			Class<?> enumPlayerInfoAction = ReflectionUtils.Classes.getEnumPlayerInfoAction();

			ReflectionUtils.setField(playerConst, "listName", ReflectionUtils.getAsIChatBaseComponent(
					tabPlayer.getPrefix() + tabPlayer.getPlayerName() + tabPlayer.getSuffix()));

			Object entityPlayerArray = Array.newInstance(playerConst.getClass(), 1);
			Array.set(entityPlayerArray, 0, playerConst);

			packetPlayOutPlayerInfo = ReflectionUtils.getNMSClass("PacketPlayOutPlayerInfo")
					.getConstructor(enumPlayerInfoAction, entityPlayerArray.getClass()).newInstance(ReflectionUtils
							.getFieldObject(enumPlayerInfoAction, enumPlayerInfoAction.getDeclaredField("ADD_PLAYER")),
							entityPlayerArray);

			scoreRef.getScoreboardTeamNames().set(packet, Collections.singletonList(tabPlayer.getPlayerName()));

			scoreRef.getScoreboardTeamMode().set(packet, 0);

			for (Player p : Bukkit.getOnlinePlayers()) {
				ReflectionUtils.sendPacket(p, packet);
				ReflectionUtils.sendPacket(p, packetPlayOutPlayerInfo);
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setTeam(String teamName) {
		try {
			scoreRef.getScoreboardTeamName().set(packet, teamName);
			scoreRef.getScoreboardTeamDisplayName().set(packet, ReflectionUtils.getAsIChatBaseComponent(teamName));

			ReflectionUtils.setField(playerConst, "listName", ReflectionUtils.getAsIChatBaseComponent(
					tabPlayer.getPrefix() + tabPlayer.getPlayerName() + tabPlayer.getSuffix()));

			List<Object> infoList = (List<Object>) ReflectionUtils.getField(packetPlayOutPlayerInfo, "b")
					.get(packetPlayOutPlayerInfo);
			for (Object infoData : infoList) {
				Field e = ReflectionUtils.getField(infoData, "e");
				ReflectionUtils.modifyFinalField(e, infoData,
						ReflectionUtils.getField(playerConst, "listName").get(playerConst));
				break;
			}

			scoreRef.getScoreboardTeamNames().set(packet, Collections.singletonList(tabPlayer.getPlayerName()));

			scoreRef.getScoreboardTeamMode().set(packet, 2);

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
			scoreRef.getScoreboardTeamPrefix().set(packet, ReflectionUtils.getAsIChatBaseComponent(""));
			scoreRef.getScoreboardTeamSuffix().set(packet, ReflectionUtils.getAsIChatBaseComponent(""));
			scoreRef.getScoreboardTeamMode().set(packet, 1);

			for (Player p : Bukkit.getOnlinePlayers()) {
				ReflectionUtils.sendPacket(p, packet);
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@Override
	public Scoreboard getScoreboard() {
		return null;
	}

	@Override
	public void setScoreboard(Scoreboard board) {
	}
}
