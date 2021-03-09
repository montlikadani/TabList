package hu.montlikadani.tablist.bukkit.tablist.groups.impl;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import hu.montlikadani.tablist.bukkit.user.TabListUser;
import hu.montlikadani.tablist.bukkit.utils.ServerVersion;
import hu.montlikadani.tablist.bukkit.utils.Util;
import hu.montlikadani.tablist.bukkit.utils.reflection.ReflectionUtils;

public class ReflectionHandled implements ITabScoreboard {

	//private final TabScoreboardReflection scoreRef = new TabScoreboardReflection();

	private Object playerConst, entityPlayerArray, packetPlayOutPlayerInfo;

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
			playerConst = ReflectionUtils.getHandle(tabListUser.getPlayer());

			entityPlayerArray = Array.newInstance(playerConst.getClass(), 1);
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

			Class<?> packetPlayOutPlayerInfoClass = ReflectionUtils.getNMSClass("PacketPlayOutPlayerInfo");
			Class<?> enumPlayerInfoAction = ReflectionUtils.Classes
					.getEnumPlayerInfoAction(packetPlayOutPlayerInfoClass);
			Constructor<?> constr = packetPlayOutPlayerInfoClass.getDeclaredConstructor(enumPlayerInfoAction,
					entityPlayerArray.getClass());
			constr.setAccessible(true);

			packetPlayOutPlayerInfo = constr.newInstance(
					enumPlayerInfoAction.getDeclaredField("UPDATE_DISPLAY_NAME").get(enumPlayerInfoAction),
					entityPlayerArray);

			infoList = (List<Object>) ReflectionUtils.getField(packetPlayOutPlayerInfo, "b")
					.get(packetPlayOutPlayerInfo);

			for (Player p : Bukkit.getOnlinePlayers()) {
				//ReflectionUtils.sendPacket(p, packet);
				ReflectionUtils.sendPacket(p, packetPlayOutPlayerInfo);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void setTeam(String teamName) {
		registerTeam(teamName);

		try {
			/*scoreRef.getScoreboardTeamMode().set(packet, 2);
			scoreRef.getScoreboardTeamDisplayName().set(packet,
					ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_13_R1) ? ReflectionUtils.getAsIChatBaseComponent(teamName)
							: teamName);*/

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
			//scoreRef.getScoreboardTeamMode().set(packet, 1);

			updateName(tabListUser.getPlayer().getName());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void updateName(String name) throws Exception {
		if (ServerVersion.isCurrentLower(ServerVersion.v1_16_R1)) {
			name = Util.colorMsg(name);
		}

		for (Object infoData : infoList) {
			Object profile = ReflectionUtils.invokeMethod(infoData, "a");
			Object id = ReflectionUtils.invokeMethod(profile, "getId");
			if (id.equals(tabListUser.getUniqueId())) {
				ReflectionUtils.modifyFinalField(ReflectionUtils.getField(infoData, "e"), infoData,
						ReflectionUtils.getAsIChatBaseComponent(name));
				break;
			}
		}

		for (Player p : Bukkit.getOnlinePlayers()) {
			//ReflectionUtils.sendPacket(p, packet);
			ReflectionUtils.sendPacket(p, packetPlayOutPlayerInfo);
		}
	}
}
