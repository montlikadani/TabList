package hu.montlikadani.tablist.bukkit.tablist.groups.impl;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.List;

import com.mojang.authlib.GameProfile;

import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.API.TabListAPI;
import hu.montlikadani.tablist.bukkit.user.TabListUser;
import hu.montlikadani.tablist.bukkit.utils.ServerVersion;
import hu.montlikadani.tablist.bukkit.utils.Util;
import hu.montlikadani.tablist.bukkit.utils.reflection.NMSContainer;
import hu.montlikadani.tablist.bukkit.utils.reflection.ReflectionUtils;

public class ReflectionHandled implements ITabScoreboard {

	//private final TabScoreboardReflection scoreRef = new TabScoreboardReflection();

	private final TabList plugin = TabListAPI.getPlugin();

	private Object packetPlayOutPlayerInfo;
	private java.lang.reflect.Field infoListField;
	private List<Object> infoList;

	private TabListUser tabListUser;

	public ReflectionHandled(TabListUser tabListUser) {
		this.tabListUser = tabListUser;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void registerTeam(String teamName) {
		if (packetPlayOutPlayerInfo != null) {
			return;
		}

		try {
			Object playerConst = ReflectionUtils.getHandle(tabListUser.getPlayer());

			Object entityPlayerArray = Array.newInstance(playerConst.getClass(), 1);
			Array.set(entityPlayerArray, 0, playerConst);

			// TODO Fix client error when using teams
			/*scoreRef.init();

			packet = scoreRef.getScoreboardTeamConstructor().newInstance();

			scoreRef.getScoreboardTeamName().set(packet, teamName);
			scoreRef.getScoreboardTeamMode().set(packet, 0);
			scoreRef.getScoreboardTeamDisplayName().set(packet,
					ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_13_R1) ? ReflectionUtils.getAsIChatBaseComponent(teamName)
							: teamName);

			scoreRef.getScoreboardTeamNames().set(packet, Collections.singletonList(tabPlayer.getPlayer().getName()));*/

			Constructor<?> constr = NMSContainer.getPacketPlayOutPlayerInfo()
					.getDeclaredConstructor(NMSContainer.getEnumPlayerInfoAction(), entityPlayerArray.getClass());
			constr.setAccessible(true);

			packetPlayOutPlayerInfo = constr.newInstance(NMSContainer.getUpdateDisplayName(), entityPlayerArray);

			infoListField = ReflectionUtils.getField(packetPlayOutPlayerInfo, "b");
			infoList = (List<Object>) infoListField.get(packetPlayOutPlayerInfo);

			for (TabListUser user : plugin.getUsers()) {
				ReflectionUtils.sendPacket(user.getPlayer(), packetPlayOutPlayerInfo);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void setTeam(String teamName) {
		registerTeam(teamName);

		try {
			updateName(tabListUser.getGroupPlayer().getCustomTabName());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void unregisterTeam(String teamName) {
		if (packetPlayOutPlayerInfo == null) {
			return;
		}

		try {
			updateName(tabListUser.getPlayer().getName());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void updateName(String name) throws Exception {
		if (ServerVersion.isCurrentLower(ServerVersion.v1_16_R1)) {
			name = Util.colorMsg(name);
		}

		Object packet = null;
		for (Object infoData : infoList) {
			GameProfile profile = (GameProfile) ReflectionUtils.invokeMethod(infoData, "a");

			if (profile.getId().equals(tabListUser.getUniqueId())) {
				Object gameMode = ReflectionUtils.getField(infoData, "c").get(infoData);
				int ping = (int) ReflectionUtils.getField(infoData, "b").get(infoData);

				Constructor<?> playerInfoDataConstr = NMSContainer.getPlayerInfoDataConstructor();

				if (playerInfoDataConstr.getParameterCount() == 5) {
					packet = playerInfoDataConstr.newInstance(packetPlayOutPlayerInfo, profile, ping, gameMode,
							ReflectionUtils.getAsIChatBaseComponent(name));
				} else {
					packet = playerInfoDataConstr.newInstance(profile, ping, gameMode,
							ReflectionUtils.getAsIChatBaseComponent(name));
				}

				break;
			}
		}

		if (packet == null) {
			return;
		}

		infoListField.set(packetPlayOutPlayerInfo, java.util.Arrays.asList(packet));

		for (TabListUser user : plugin.getUsers()) {
			org.bukkit.entity.Player player = user.getPlayer();

			ReflectionUtils.sendPacket(player, packetPlayOutPlayerInfo);
			ReflectionUtils.sendPacket(player, packet);
		}
	}
}
