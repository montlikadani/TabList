package hu.montlikadani.tablist.bukkit.tablist.groups.impl;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.List;

import com.mojang.authlib.GameProfile;

import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.api.TabListAPI;
import hu.montlikadani.tablist.bukkit.tablist.groups.GroupPlayer;
import hu.montlikadani.tablist.bukkit.user.TabListUser;
import hu.montlikadani.tablist.bukkit.utils.ServerVersion;
import hu.montlikadani.tablist.bukkit.utils.Util;
import hu.montlikadani.tablist.bukkit.utils.reflection.NMSContainer;
import hu.montlikadani.tablist.bukkit.utils.reflection.ReflectionUtils;
import org.bukkit.entity.Player;

public class ReflectionHandled implements ITabScoreboard {

	private final TabScoreboardReflection scoreRef = new TabScoreboardReflection();
	private final TabList plugin = TabListAPI.getPlugin();

	private Object packetPlayOutPlayerInfo;
	private java.lang.reflect.Field infoListField;
	private List<Object> infoList;

	@SuppressWarnings("unchecked")
	@Override
	public void registerTeam(GroupPlayer groupPlayer) {
		try {
			if (packetPlayOutPlayerInfo != null && !plugin.getGroups().isToSort()) {
				return;
			}

			scoreRef.init();

			unregisterTeam(groupPlayer);

			Player player = groupPlayer.getUser().getPlayer();
			Object handle = ReflectionUtils.getHandle(player);
			Object[] entityPlayerArray = (Object[]) Array.newInstance(handle.getClass(), 1);
			String teamName = groupPlayer.getFullGroupTeamName();

			Object newTeamPacket = scoreRef.getScoreboardTeamConstructor().newInstance();
			Object displayName = ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_13_R1)
					? ReflectionUtils.getAsIChatBaseComponent(teamName)
					: teamName;

			scoreRef.getScoreboardTeamName().set(newTeamPacket, teamName);
			scoreRef.getScoreboardTeamMode().set(newTeamPacket, 0);
			scoreRef.getScoreboardTeamDisplayName().set(newTeamPacket, displayName);
			scoreRef.getScoreboardTeamNames().set(newTeamPacket, Collections.singletonList(player.getName()));

			Array.set(entityPlayerArray, 0, handle);

			Class<?> playOutPlayerInfo = NMSContainer.getPacketPlayOutPlayerInfo();
			Class<?> playerInfoAction = NMSContainer.getEnumPlayerInfoAction();
			Constructor<?> constr = playOutPlayerInfo.getDeclaredConstructor(playerInfoAction,
					entityPlayerArray.getClass());

			constr.setAccessible(true);

			packetPlayOutPlayerInfo = constr.newInstance(NMSContainer.getUpdateDisplayName(), entityPlayerArray);

			infoListField = ReflectionUtils.getField(packetPlayOutPlayerInfo, "b");
			infoList = (List<Object>) infoListField.get(packetPlayOutPlayerInfo);

			for (TabListUser user : plugin.getUsers()) {
				ReflectionUtils.sendPacket(user.getPlayer(), newTeamPacket);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void setTeam(GroupPlayer groupPlayer) {
		registerTeam(groupPlayer);
		updateName(groupPlayer);
	}

	@Override
	public void unregisterTeam(GroupPlayer groupPlayer) {
		if (scoreRef.getScoreboardTeamConstructor() == null) {
			return;
		}

		try {
			Object oldTeamPacket = scoreRef.getScoreboardTeamConstructor().newInstance();

			scoreRef.getScoreboardTeamName().set(oldTeamPacket, groupPlayer.getFullGroupTeamName());
			scoreRef.getScoreboardTeamMode().set(oldTeamPacket, 1);

			for (TabListUser user : plugin.getUsers()) {
				ReflectionUtils.sendPacket(user.getPlayer(), oldTeamPacket);
			}
		} catch (Exception exception) {
			exception.printStackTrace();
		}

		packetPlayOutPlayerInfo = null;
	}

	private void updateName(GroupPlayer groupPlayer) {
		String name = groupPlayer.getCustomTabName();

		try {
			if (ServerVersion.isCurrentLower(ServerVersion.v1_16_R1)) {
				name = Util.colorMsg(name);
			}

			Object nameComponent = ReflectionUtils.getAsIChatBaseComponent(name);
			Object infoPacket = null;

			for (Object infoData : infoList) {
				GameProfile profile = (GameProfile) ReflectionUtils.invokeMethod(infoData, "a");

				if (!profile.getId().equals(groupPlayer.getUser().getUniqueId())) {
					continue;
				}

				Constructor<?> playerInfoDataConstr = NMSContainer.getPlayerInfoDataConstructor();
				Object gameMode = ReflectionUtils.getField(infoData, "c").get(infoData);
				int ping = (int) ReflectionUtils.getField(infoData, "b").get(infoData);

				infoPacket = playerInfoDataConstr.getParameterCount() == 5
						? playerInfoDataConstr.newInstance(packetPlayOutPlayerInfo, profile, ping, gameMode,
								nameComponent)
						: playerInfoDataConstr.newInstance(profile, ping, gameMode, nameComponent);

				break;
			}

			if (infoPacket == null) {
				return;
			}

			infoListField.set(packetPlayOutPlayerInfo, Collections.singletonList(infoPacket));

			for (TabListUser user : plugin.getUsers()) {
				Player player = user.getPlayer();

				ReflectionUtils.sendPacket(player, packetPlayOutPlayerInfo);
				ReflectionUtils.sendPacket(player, infoPacket);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
