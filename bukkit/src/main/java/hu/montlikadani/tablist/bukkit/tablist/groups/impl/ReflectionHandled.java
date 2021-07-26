package hu.montlikadani.tablist.bukkit.tablist.groups.impl;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.List;

import com.mojang.authlib.GameProfile;

import hu.montlikadani.tablist.bukkit.TabList;
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

	private final TabList tl;

	private Object packetPlayOutPlayerInfo;
	private List<Object> infoList;

	public ReflectionHandled(TabList tl) {
		this.tl = tl;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void registerTeam(GroupPlayer groupPlayer) {
		if (packetPlayOutPlayerInfo != null && !tl.getGroups().isToSort()) {
			return;
		}

		try {
			unregisterTeam(groupPlayer);

			Player player = groupPlayer.getUser().getPlayer();
			if (player == null) {
				return;
			}

			String teamName = groupPlayer.getFullGroupTeamName();
			Object newTeamPacket;

			if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_17_R1)) {
				Object scoreTeam = ClazzContainer.getScoreboardTeamConstructor()
						.newInstance(ClazzContainer.getScoreboardConstructor().newInstance(), teamName);

				java.util.Collection<String> playerNameSet = (java.util.Collection<String>) ClazzContainer
						.getPlayerNameSetMethod().invoke(scoreTeam);
				playerNameSet.add(player.getName());

				ClazzContainer.getScoreboardTeamNames().set(scoreTeam, playerNameSet);

				ClazzContainer.getScoreboardTeamSetDisplayName().invoke(scoreTeam,
						ReflectionUtils.getAsIChatBaseComponent(teamName));
				newTeamPacket = ClazzContainer.scoreboardTeamPacketByAction(scoreTeam, 0);
			} else {
				newTeamPacket = ClazzContainer.getPacketPlayOutScoreboardTeamConstructor().newInstance();

				ClazzContainer.getScoreboardTeamName().set(newTeamPacket, teamName);
				ClazzContainer.getScoreboardTeamMode().set(newTeamPacket, 0);
				ClazzContainer.getScoreboardTeamDisplayName().set(newTeamPacket,
						ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_13_R1)
								? ReflectionUtils.getAsIChatBaseComponent(teamName)
								: teamName);
				ClazzContainer.getScoreboardTeamNames().set(newTeamPacket, Collections.singletonList(player.getName()));
			}

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

				if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_17_R1)) {
					for (Object f : ClazzContainer.getScoreboardNameTagVisibilityEnumConstants()) {
						String name = (String) ClazzContainer.getNameTagVisibilityNameField().get(f);

						if (optionName.equalsIgnoreCase(name)) {
							ClazzContainer.getScoreboardTeamSetNameTagVisibility().invoke(newTeamPacket, f);
							break;
						}
					}
				} else {
					ClazzContainer.getNameTagVisibility().set(newTeamPacket, optionName);
				}

				break;
			}

			Object handle = ReflectionUtils.getPlayerHandle(player);
			Object[] entityPlayerArray = (Object[]) Array.newInstance(handle.getClass(), 1);

			Array.set(entityPlayerArray, 0, handle);

			packetPlayOutPlayerInfo = ClazzContainer.getPlayOutPlayerInfoConstructor()
					.newInstance(ClazzContainer.getUpdateDisplayName(), entityPlayerArray);

			infoList = (List<Object>) ClazzContainer.getInfoList().get(packetPlayOutPlayerInfo);

			for (TabListUser user : tl.getUsers()) {
				Player pl = user.getPlayer();

				if (pl != null) {
					ReflectionUtils.sendPacket(pl, packetPlayOutPlayerInfo);
					ReflectionUtils.sendPacket(pl, newTeamPacket);
				}
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
		try {
			Object oldTeamPacket;

			if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_17_R1)) {
				oldTeamPacket = ClazzContainer
						.scoreboardTeamPacketByAction(ClazzContainer.getScoreboardTeamConstructor().newInstance(
								ClazzContainer.getScoreboardConstructor().newInstance(),
								groupPlayer.getFullGroupTeamName()), 1);
			} else {
				oldTeamPacket = ClazzContainer.getPacketPlayOutScoreboardTeamConstructor().newInstance();

				ClazzContainer.getScoreboardTeamName().set(oldTeamPacket, groupPlayer.getFullGroupTeamName());
				ClazzContainer.getScoreboardTeamMode().set(oldTeamPacket, 1);
			}

			for (TabListUser user : tl.getUsers()) {
				ReflectionUtils.sendPacket(user.getPlayer(), oldTeamPacket);
			}
		} catch (Exception exception) {
			exception.printStackTrace();
		}

		packetPlayOutPlayerInfo = null;
	}

	private void updateName(GroupPlayer groupPlayer) {
		if (packetPlayOutPlayerInfo == null) {
			return;
		}

		try {
			Object infoPacket = null;

			for (Object infoData : infoList) {
				GameProfile profile;

				if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_17_R1)) {
					profile = (GameProfile) ClazzContainer.getPlayerInfoDataProfileField().get(infoData);
				} else {
					profile = (GameProfile) ClazzContainer.getPlayerInfoDataProfileMethod().invoke(infoData);
				}

				if (!profile.getId().equals(groupPlayer.getUser().getUniqueId())) {
					continue;
				}

				String name = groupPlayer.getTabNameWithPrefixSuffix();

				if (ServerVersion.isCurrentLower(ServerVersion.v1_16_R1)) {
					name = Util.colorMsg(name);
				}

				Object nameComponent = ReflectionUtils.getAsIChatBaseComponent(name);
				Constructor<?> playerInfoDataConstr = ClazzContainer.getPlayerInfoDataConstructor();
				Object gameMode = ClazzContainer.getPlayerInfoDataGameMode().get(infoData);
				int ping = (int) ClazzContainer.getPlayerInfoDataPing().get(infoData);

				if (playerInfoDataConstr.getParameterCount() == 5) {
					infoPacket = playerInfoDataConstr.newInstance(packetPlayOutPlayerInfo, profile, ping, gameMode,
							nameComponent);
				} else if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_17_R1)) {
					infoPacket = playerInfoDataConstr.newInstance(profile, ping, gameMode, nameComponent);
				} else {
					infoPacket = playerInfoDataConstr.newInstance(profile, ping, gameMode, nameComponent);
				}

				break;
			}

			if (infoPacket == null || packetPlayOutPlayerInfo == null) {
				return; // Somehow the 2nd condition is null sometimes
			}

			ClazzContainer.getInfoList().set(packetPlayOutPlayerInfo, Collections.singletonList(infoPacket));

			for (TabListUser user : tl.getUsers()) {
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
