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
import hu.montlikadani.tablist.bukkit.utils.reflection.ClazzContainer;
import hu.montlikadani.tablist.bukkit.utils.reflection.ReflectionUtils;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.NameTagVisibility;
import org.bukkit.scoreboard.Team;

@SuppressWarnings("deprecation")
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
			if (player == null) {
				return;
			}

			Object handle = ReflectionUtils.getHandle(player);
			Object[] entityPlayerArray = (Object[]) Array.newInstance(handle.getClass(), 1);
			String teamName = groupPlayer.getFullGroupTeamName();

			Object newTeamPacket = scoreRef.getScoreboardTeamConstructor().newInstance();

			scoreRef.getScoreboardTeamName().set(newTeamPacket, teamName);
			scoreRef.getScoreboardTeamMode().set(newTeamPacket, 0);
			scoreRef.getScoreboardTeamDisplayName().set(newTeamPacket,
					ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_13_R1)
							? ReflectionUtils.getAsIChatBaseComponent(teamName)
							: teamName);
			scoreRef.getScoreboardTeamNames().set(newTeamPacket, Collections.singletonList(player.getName()));

			for (Team team : player.getScoreboard().getTeams()) {
				if (!team.hasEntry(player.getName())) {
					continue;
				}

				String optionName = "";

				if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_9_R1)) {
					Team.OptionStatus optionStatus = team.getOption(Team.Option.NAME_TAG_VISIBILITY);

					if (optionStatus == Team.OptionStatus.ALWAYS) {
						continue;
					}

					switch (optionStatus) {
					case FOR_OTHER_TEAMS:
						optionName = "hideForOtherTeams";
						break;
					case FOR_OWN_TEAM:
						optionName = "hideForOwnTeam";
						break;
					default:
						optionName = optionStatus.name().toLowerCase();
						break;
					}
				} else {
					NameTagVisibility visibility = team.getNameTagVisibility();

					if (visibility == NameTagVisibility.ALWAYS) {
						continue;
					}

					switch (visibility) {
					case HIDE_FOR_OTHER_TEAMS:
						optionName = "hideForOtherTeams";
						break;
					case HIDE_FOR_OWN_TEAM:
						optionName = "hideForOwnTeam";
						break;
					default:
						optionName = visibility.name().toLowerCase();
						break;
					}
				}

				scoreRef.getNameTagVisibility().set(newTeamPacket, optionName);
				break;
			}

			Array.set(entityPlayerArray, 0, handle);

			Class<?> playOutPlayerInfo = ClazzContainer.getPacketPlayOutPlayerInfo();
			Constructor<?> constr = playOutPlayerInfo.getDeclaredConstructor(ClazzContainer.getEnumPlayerInfoAction(),
					entityPlayerArray.getClass());

			constr.setAccessible(true);

			packetPlayOutPlayerInfo = constr.newInstance(ClazzContainer.getUpdateDisplayName(), entityPlayerArray);

			infoListField = ReflectionUtils.getField(playOutPlayerInfo, "b");
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
		if (infoListField == null || packetPlayOutPlayerInfo == null) {
			return;
		}

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

				Constructor<?> playerInfoDataConstr = ClazzContainer.getPlayerInfoDataConstructor();
				Class<?> infoDataClass = infoData.getClass();
				Object gameMode = ReflectionUtils.getField(infoDataClass, "c").get(infoData);
				int ping = (int) ReflectionUtils.getField(infoDataClass, "b").get(infoData);

				infoPacket = playerInfoDataConstr.getParameterCount() == 5
						? playerInfoDataConstr.newInstance(packetPlayOutPlayerInfo, profile, ping, gameMode,
								nameComponent)
						: playerInfoDataConstr.newInstance(profile, ping, gameMode, nameComponent);

				break;
			}

			if (infoPacket == null || packetPlayOutPlayerInfo == null) {
				return; // Somehow the 2nd condition is null sometimes
			}

			infoListField.set(packetPlayOutPlayerInfo, Collections.singletonList(infoPacket));

			for (TabListUser user : plugin.getUsers()) {
				Player player = user.getPlayer();

				if (player != null) {
					ReflectionUtils.sendPacket(player, packetPlayOutPlayerInfo);
					ReflectionUtils.sendPacket(player, infoPacket);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
