package hu.montlikadani.tablist.bukkit.tablist.groups.impl;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.List;

import com.mojang.authlib.GameProfile;

import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.API.TabListAPI;
import hu.montlikadani.tablist.bukkit.config.constantsLoader.TabEntryValues;
import hu.montlikadani.tablist.bukkit.tablist.entry.row.RowPlayer;
import hu.montlikadani.tablist.bukkit.user.TabListUser;
import hu.montlikadani.tablist.bukkit.utils.ServerVersion;
import hu.montlikadani.tablist.bukkit.utils.Util;
import hu.montlikadani.tablist.bukkit.utils.reflection.ReflectionUtils;

public class ReflectionHandled implements ITabScoreboard {

	//private final TabScoreboardReflection scoreRef = new TabScoreboardReflection();

	private final TabList plugin = TabListAPI.getPlugin();

	private Object playerConst, entityPlayerArray, packetPlayOutPlayerInfo;
	private List<Object> infoList;
	private TabListUser tabListUser;

	public ReflectionHandled(TabListUser tabListUser) {
		this.tabListUser = tabListUser;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void registerTeam(String teamName) {
		if (TabEntryValues.isEnabled() || packetPlayOutPlayerInfo != null) {
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

			for (TabListUser user : plugin.getUsers()) {
				//ReflectionUtils.sendPacket(user.getPlayer(), packet);
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
		if (!TabEntryValues.isEnabled() && packetPlayOutPlayerInfo == null) {
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

		if (TabEntryValues.isEnabled()) {
			final String text = name;

			// TODO Improve?
			plugin.getTabManager().getTabEntries().getEntry(entry -> {
				java.util.Optional<org.bukkit.entity.Player> opt = entry.getRow().asPlayer();
				return opt.isPresent() && opt.get().getUniqueId().equals(tabListUser.getUniqueId());
			}).ifPresent(
					entry -> ((RowPlayer) entry.getRow()).getInfoName().updateDisplayName(tabListUser, text, null));

			return;
		}

		for (Object infoData : infoList) {
			GameProfile profile = (GameProfile) ReflectionUtils.invokeMethod(infoData, "a");

			if (profile.getId().equals(tabListUser.getUniqueId())) {
				ReflectionUtils.modifyFinalField(ReflectionUtils.getField(infoData, "e"), infoData,
						ReflectionUtils.getAsIChatBaseComponent(name));
				break;
			}
		}

		for (TabListUser user : plugin.getUsers()) {
			//ReflectionUtils.sendPacket(user.getPlayer(), packet);
			ReflectionUtils.sendPacket(user.getPlayer(), packetPlayOutPlayerInfo);
		}
	}
}
