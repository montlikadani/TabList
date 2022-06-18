package hu.montlikadani.tablist.tablist.groups.impl;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.mojang.authlib.GameProfile;

import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.config.constantsLoader.ConfigValues;
import hu.montlikadani.tablist.tablist.groups.GroupPlayer;
import hu.montlikadani.tablist.user.TabListUser;
import hu.montlikadani.tablist.utils.ServerVersion;
import hu.montlikadani.tablist.utils.reflection.ClazzContainer;
import hu.montlikadani.tablist.utils.reflection.ReflectionUtils;
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
			Object newTeamPacket = null, scoreTeam = null;

			if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_17_R1)) {
				scoreTeam = ClazzContainer.getScoreboardTeamConstructor()
						.newInstance(ClazzContainer.getScoreboardConstructor().newInstance(), teamName);

				java.util.Collection<String> playerNameSet = (java.util.Collection<String>) ClazzContainer
						.getPlayerNameSetMethod().invoke(scoreTeam);
				playerNameSet.add(player.getName());

				ClazzContainer.getScoreboardTeamNames().set(scoreTeam, playerNameSet);

				ClazzContainer.getScoreboardTeamSetDisplayName().invoke(scoreTeam,
						ReflectionUtils.getAsIChatBaseComponent(teamName));
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

			if (ConfigValues.isFollowNameTagVisibility()) {
				String optionName = null;

				for (Team team : player.getScoreboard().getTeams()) {
					if (ClazzContainer.isTeamOptionStatusEnumExist()) {
						Team.OptionStatus optionStatus = team.getOption(Team.Option.NAME_TAG_VISIBILITY);

						switch (optionStatus) {
						case FOR_OTHER_TEAMS:
							optionName = "hideForOtherTeams";
							break;
						case FOR_OWN_TEAM:
							optionName = "hideForOwnTeam";
							break;
						default:
							if (optionStatus != Team.OptionStatus.ALWAYS) {
								optionName = optionStatus.name().toLowerCase(Locale.ENGLISH);
							}

							break;
						}
					} else {
						NameTagVisibility visibility = team.getNameTagVisibility();

						switch (visibility) {
						case HIDE_FOR_OTHER_TEAMS:
							optionName = "hideForOtherTeams";
							break;
						case HIDE_FOR_OWN_TEAM:
							optionName = "hideForOwnTeam";
							break;
						default:
							if (visibility != NameTagVisibility.ALWAYS) {
								optionName = visibility.name().toLowerCase(Locale.ENGLISH);
							}

							break;
						}
					}
				}

				if (optionName != null) {
					if (scoreTeam != null) {
						for (Object f : ClazzContainer.getScoreboardNameTagVisibilityEnumConstants()) {
							if (optionName.equalsIgnoreCase((String) ClazzContainer.getNameTagVisibilityNameField().get(f))) {
								ClazzContainer.getScoreboardTeamSetNameTagVisibility().invoke(scoreTeam, f);
								break;
							}
						}
					} else {
						ClazzContainer.getNameTagVisibility().set(newTeamPacket, optionName);
					}
				}
			}

			if (newTeamPacket == null) {
				newTeamPacket = ClazzContainer.scoreboardTeamPacketByAction(scoreTeam, 0);
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

			// Received packet for unknown team a02: team action: REMOVE, player action:
			// null
			// This means that the client cannot find the team with the name a02 and tries
			// to remove
			// This is not a bug, it can be ignored.

			if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_17_R1)) {
				oldTeamPacket = ClazzContainer.scoreboardTeamPacketByAction(ClazzContainer.getScoreboardTeamConstructor()
						.newInstance(ClazzContainer.getScoreboardConstructor().newInstance(), groupPlayer.getFullGroupTeamName()),
						1);
			} else {
				oldTeamPacket = ClazzContainer.getPacketPlayOutScoreboardTeamConstructor().newInstance();

				ClazzContainer.getScoreboardTeamName().set(oldTeamPacket, groupPlayer.getFullGroupTeamName());
				ClazzContainer.getScoreboardTeamMode().set(oldTeamPacket, 1);
			}

			for (TabListUser user : tl.getUsers()) {
				ReflectionUtils.sendPacket(user.getPlayer(), oldTeamPacket);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void updateName(GroupPlayer groupPlayer) {
		if (packetPlayOutPlayerInfo == null) {
			return;
		}

		try {
			GameProfile profile;

			for (Object infoData : infoList) {
				if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_17_R1)) {
					profile = (GameProfile) ClazzContainer.getPlayerInfoDataProfileField().get(infoData);
				} else {
					profile = (GameProfile) ClazzContainer.getPlayerInfoDataProfileMethod().invoke(infoData);
				}

				if (!groupPlayer.getUser().getUniqueId().equals(profile.getId())) {
					continue;
				}

				Object nameComponent = ReflectionUtils.getAsIChatBaseComponent(groupPlayer.getTabNameWithPrefixSuffix());
				int ping = (int) ClazzContainer.getPlayerInfoDataPing().get(infoData);
				Object gameMode = ClazzContainer.getPlayerInfoDataGameMode().get(infoData);
				Constructor<?> playerInfoDataConstr = ClazzContainer.getPlayerInfoDataConstructor();
				Object infoPacket;

				switch (playerInfoDataConstr.getParameterCount()) {
				case 5:
					if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_19_R1)) {
						infoPacket = playerInfoDataConstr.newInstance(profile, ping, gameMode, nameComponent, null);
					} else {
						infoPacket = playerInfoDataConstr.newInstance(packetPlayOutPlayerInfo, profile, ping, gameMode,
								nameComponent);
					}

					break;
				default:
					infoPacket = playerInfoDataConstr.newInstance(profile, ping, gameMode, nameComponent);
					break;
				}

				ClazzContainer.getInfoList().set(packetPlayOutPlayerInfo, Collections.singletonList(infoPacket));

				for (TabListUser user : tl.getUsers()) {
					ReflectionUtils.sendPacket(user.getPlayer(), packetPlayOutPlayerInfo);
				}

				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
