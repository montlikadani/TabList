package hu.montlikadani.tablist.packets;

import java.util.ArrayList;
import java.util.Collections;

import org.bukkit.craftbukkit.v1_19_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import com.mojang.authlib.GameProfile;

import hu.montlikadani.tablist.tablist.TabText;
import hu.montlikadani.tablist.utils.reflection.ReflectionUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

public final class V1_19_R2 implements IPacketNM {

	private final java.util.List<PlayerTeam> playerTeams = new ArrayList<>();

	@Override
	public void sendPacket(Player player, Object packet) {
		getPlayerHandle(player).connection.send((Packet<?>) packet);
	}

	@Override
	public ServerPlayer getPlayerHandle(Player player) {
		return ((CraftPlayer) player).getHandle();
	}

	@Override
	public Component fromJson(String json) {
		return Component.Serializer.fromJson(json);
	}

	@Override
	public void sendTabTitle(Player player, TabText header, TabText footer) {
		Object tabHeader = ReflectionUtils.EMPTY_COMPONENT;
		Object tabFooter = ReflectionUtils.EMPTY_COMPONENT;

		if (header != null && header != TabText.EMPTY) {
			try {
				tabHeader = ReflectionUtils.asComponent(header);
			} catch (Exception ex) {
				ex.printStackTrace();
				return;
			}
		}

		if (footer != null && footer != TabText.EMPTY) {
			try {
				tabFooter = ReflectionUtils.asComponent(footer);
			} catch (Exception e1) {
				e1.printStackTrace();
				return;
			}
		}

		if (tabHeader != null && tabFooter != null) {
			sendPacket(player, new ClientboundTabListPacket((Component) tabHeader, (Component) tabFooter));
		}
	}

	@SuppressWarnings("resource")
	@Override
	public Object getNewEntityPlayer(GameProfile profile) {
		//((CraftWorld) org.bukkit.Bukkit.getServer().getWorlds().get(0)).getHandle()
		return new ServerPlayer(MinecraftServer.getServer(), MinecraftServer.getServer().overworld(), profile);
	}

	@Override
	public void addPlayerToTab(Player source, Player target) {
		sendPacket(target, ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(Collections.singletonList(((CraftPlayer) source).getHandle())));
	}

	@Override
	public void removePlayerFromTab(Player source, Player target) {
		sendPacket(target, new ClientboundPlayerInfoRemovePacket(Collections.singletonList(source.getUniqueId())));
	}

	@Override
	public ClientboundPlayerInfoUpdatePacket updateDisplayNamePacket(Object entityPlayer, String component, boolean listName) {
		if (listName) {
			setListName(entityPlayer, component);
		}

		return new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME, (ServerPlayer) entityPlayer);
	}

	@Override
	public void setListName(Object entityPlayer, String component) {
		try {
			((ServerPlayer) entityPlayer).listName = (Component) ReflectionUtils.asComponent(component);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public Object newPlayerInfoUpdatePacketAdd(Object entityPlayer) {
		return new ClientboundPlayerInfoUpdatePacket(
				java.util.EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED),
				Collections.singletonList((ServerPlayer) entityPlayer));
	}

	@Override
	public Object updateLatency(Object entityPlayer) {
		return new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY, (ServerPlayer) entityPlayer);
	}

	@Override
	public Object removeEntityPlayer(Object entityPlayer) {
		return new ClientboundPlayerInfoRemovePacket(Collections.singletonList(((ServerPlayer) entityPlayer).getUUID()));
	}

	private java.lang.reflect.Field entriesField;

	@Override
	public void setInfoData(Object info, java.util.UUID id, int ping, Object component) {
		ClientboundPlayerInfoUpdatePacket update = (ClientboundPlayerInfoUpdatePacket) info;

		for (ClientboundPlayerInfoUpdatePacket.Entry playerInfo : update.entries()) {
			if (!playerInfo.profileId().equals(id)) {
				continue;
			}

			try {
				// Entries list is immutable, so use reflection to bypass
				if (entriesField == null) {
					entriesField = update.getClass().getDeclaredField("b");
					entriesField.setAccessible(true);
				}

				entriesField.set(update, Collections.singletonList(new ClientboundPlayerInfoUpdatePacket.Entry(playerInfo.profileId(), playerInfo.profile(), playerInfo.listed(),
						ping == -2 ? playerInfo.latency() : ping, playerInfo.gameMode(), (Component) component, playerInfo.chatSession())));
			} catch (Exception e) {
				e.printStackTrace();
			}

			break;
		}
	}

	@Override
	public Object createBoardTeam(String teamName, Player player) {
		PlayerTeam playerTeam = new PlayerTeam(new net.minecraft.world.scores.Scoreboard(), teamName);

		playerTeam.getPlayers().add(player.getName());

		try {
			playerTeam.setDisplayName((Component) ReflectionUtils.asComponent(teamName));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		if (hu.montlikadani.tablist.config.constantsLoader.ConfigValues.isFollowNameTagVisibility()) {
			net.minecraft.world.scores.Team.Visibility visibility = null;

			for (Team team : player.getScoreboard().getTeams()) {
				Team.OptionStatus optionStatus = team.getOption(Team.Option.NAME_TAG_VISIBILITY);

				switch (optionStatus) {
				case FOR_OTHER_TEAMS:
					visibility = net.minecraft.world.scores.Team.Visibility.HIDE_FOR_OTHER_TEAMS;
					break;
				case FOR_OWN_TEAM:
					visibility = net.minecraft.world.scores.Team.Visibility.HIDE_FOR_OWN_TEAM;
					break;
				default:
					if (optionStatus != Team.OptionStatus.ALWAYS) {
						visibility = net.minecraft.world.scores.Team.Visibility.NEVER;
					}

					break;
				}
			}

			if (visibility != null) {
				playerTeam.setNameTagVisibility(visibility);
			}
		}

		playerTeams.add(playerTeam);
		return ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(playerTeam, true);
	}

	@Override
	public Object unregisterBoardTeam(Object playerTeam) {
		PlayerTeam team = (PlayerTeam) playerTeam;
		playerTeams.remove(team);

		return ClientboundSetPlayerTeamPacket.createRemovePacket(team);
	}

	@Override
	public Object findBoardTeamByName(String teamName) {
		for (PlayerTeam team : playerTeams) {
			if (team.getName().equals(teamName)) {
				return team;
			}
		}

		return null;
	}

	@Override
	public Object createObjectivePacket(String objectiveName, Object nameComponent) {
		return new Objective(null, objectiveName, ObjectiveCriteria.DUMMY, (Component) nameComponent, ObjectiveCriteria.RenderType.INTEGER);
	}

	@Override
	public Object scoreboardObjectivePacket(Object objective, int mode) {
		return new net.minecraft.network.protocol.game.ClientboundSetObjectivePacket((Objective) objective, mode);
	}

	@Override
	public Object scoreboardDisplayObjectivePacket(Object objective, int slot) {
		return new net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket(slot, (Objective) objective);
	}

	@Override
	public Object changeScoreboardScorePacket(String objectiveName, String scoreName, int score) {
		return new ClientboundSetScorePacket(ServerScoreboard.Method.CHANGE, objectiveName, scoreName, score);
	}

	@Override
	public Object removeScoreboardScorePacket(String objectiveName, String scoreName, int score) {
		return new ClientboundSetScorePacket(ServerScoreboard.Method.REMOVE, objectiveName, scoreName, score);
	}

	@Override
	public Object createScoreboardHealthObjectivePacket(String objectiveName, Object nameComponent) {
		return new Objective(null, objectiveName, ObjectiveCriteria.DUMMY, (Component) nameComponent, ObjectiveCriteria.RenderType.HEARTS);
	}
}
