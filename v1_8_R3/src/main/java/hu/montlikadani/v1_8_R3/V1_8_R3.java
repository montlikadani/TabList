package hu.montlikadani.v1_8_R3;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

import net.minecraft.server.v1_8_R3.EnumChatFormat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.NameTagVisibility;
import org.bukkit.scoreboard.Team;

import com.mojang.authlib.GameProfile;

import hu.montlikadani.api.IPacketNM;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.IScoreboardCriteria;
import net.minecraft.server.v1_8_R3.MinecraftServer;
import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.PacketPlayOutPlayerInfo;
import net.minecraft.server.v1_8_R3.PacketPlayOutPlayerListHeaderFooter;
import net.minecraft.server.v1_8_R3.PacketPlayOutScoreboardDisplayObjective;
import net.minecraft.server.v1_8_R3.PacketPlayOutScoreboardObjective;
import net.minecraft.server.v1_8_R3.PacketPlayOutScoreboardScore;
import net.minecraft.server.v1_8_R3.PacketPlayOutScoreboardTeam;
import net.minecraft.server.v1_8_R3.PlayerInteractManager;
import net.minecraft.server.v1_8_R3.Scoreboard;
import net.minecraft.server.v1_8_R3.ScoreboardObjective;
import net.minecraft.server.v1_8_R3.ScoreboardScore;
import net.minecraft.server.v1_8_R3.ScoreboardTeam;
import net.minecraft.server.v1_8_R3.ScoreboardTeamBase;
import net.minecraft.server.v1_8_R3.WorldServer;
import net.minecraft.server.v1_8_R3.WorldSettings.EnumGamemode;

public final class V1_8_R3 implements IPacketNM {

	private final List<ObjectiveStorage> objectiveStorage = new ArrayList<>();

	private Field headerField, footerField, entriesField, infoList, playerInfoAction, scoreboardTeamPlayers, scoreboardTeamPrefix, scoreboardTeamSuffix,
		scoreboardTeamNameTagVisibility, scoreboardTeamEnumChatFormat, scoreboardTeamName;

	private final Scoreboard scoreboard = new Scoreboard();

	private final Set<TagTeam> tagTeams = new HashSet<>();

	private final List<PacketReceivingListener> packetReceivingListeners = new java.util.concurrent.CopyOnWriteArrayList<>();

	public V1_8_R3() {
		try {
			(headerField = PacketPlayOutPlayerListHeaderFooter.class.getDeclaredField("a")).setAccessible(true);
			(footerField = PacketPlayOutPlayerListHeaderFooter.class.getDeclaredField("b")).setAccessible(true);
			(infoList = PacketPlayOutPlayerInfo.class.getDeclaredField("b")).setAccessible(true);
			(playerInfoAction = PacketPlayOutPlayerInfo.class.getDeclaredField("a")).setAccessible(true);

			(scoreboardTeamPlayers = PacketPlayOutScoreboardTeam.class.getDeclaredField("g")).setAccessible(true);
			(scoreboardTeamPrefix = PacketPlayOutScoreboardTeam.class.getDeclaredField("c")).setAccessible(true);
			(scoreboardTeamSuffix = PacketPlayOutScoreboardTeam.class.getDeclaredField("d")).setAccessible(true);
			(scoreboardTeamNameTagVisibility = PacketPlayOutScoreboardTeam.class.getDeclaredField("e")).setAccessible(true);
			(scoreboardTeamEnumChatFormat = PacketPlayOutScoreboardTeam.class.getDeclaredField("f")).setAccessible(true);
			(scoreboardTeamName = PacketPlayOutScoreboardTeam.class.getDeclaredField("a")).setAccessible(true);
		} catch (NoSuchFieldException | SecurityException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void packetListeningAllowed(Player player) {
		PacketReceivingListener receivingListener = listenerByPlayer(player.getUniqueId());

		if (receivingListener != null) {
			receivingListener.packetListeningAllowed = !receivingListener.packetListeningAllowed;
		}
	}

	private PacketReceivingListener listenerByPlayer(UUID playerId) {
		for (PacketReceivingListener receivingListener : packetReceivingListeners) {
			if (receivingListener.listenerPlayerId.equals(playerId)) {
				return receivingListener;
			}
		}

		return null;
	}

	@Override
	public void sendPacket(Player player, Object packet) {
		getPlayerHandle(player).playerConnection.sendPacket((Packet<?>) packet);
	}

	private void sendPacket(EntityPlayer player, Packet<?> packet) {
		player.playerConnection.sendPacket(packet);
	}

	@Override
	public void addPlayerChannelListener(Player player, List<Class<?>> classesToListen) {
		UUID playerId = player.getUniqueId();

		if (listenerByPlayer(playerId) != null) {
			return;
		}

		EntityPlayer entityPlayer = getPlayerHandle(player);

		if (entityPlayer.playerConnection.networkManager.channel.pipeline().get(PACKET_INJECTOR_NAME) == null) {
			PacketReceivingListener packetReceivingListener = new PacketReceivingListener(playerId, classesToListen);

			packetReceivingListeners.add(packetReceivingListener);

			try {
				entityPlayer.playerConnection.networkManager.channel.pipeline().addBefore("packet_handler", PACKET_INJECTOR_NAME,
						packetReceivingListener);
			} catch (NoSuchElementException ex) {
				// packet_handler not exists, sure then, ignore
			}
		}
	}

	@Override
	public void removePlayerChannelListener(Player player) {
		EntityPlayer entityPlayer = getPlayerHandle(player);

		if (entityPlayer.playerConnection.networkManager.channel != null) {
			try {
				entityPlayer.playerConnection.networkManager.channel.pipeline().remove(PACKET_INJECTOR_NAME);
			} catch (NoSuchElementException ignored) {
			}
		}

		packetReceivingListeners.removeIf(pr -> pr.listenerPlayerId.equals(player.getUniqueId()));
	}

	@Override
	public EntityPlayer getPlayerHandle(Player player) {
		return ((org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer) player).getHandle();
	}

	@Override
	public IChatBaseComponent fromJson(String json) {
		return IChatBaseComponent.ChatSerializer.a(json);
	}

	@Override
	public void sendTabTitle(Player player, Object header, Object footer) {
		PacketPlayOutPlayerListHeaderFooter packet = new PacketPlayOutPlayerListHeaderFooter();

		try {
			headerField.set(packet, header);
			footerField.set(packet, footer);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}

		sendPacket(player, packet);
	}

	@Override
	public EntityPlayer getNewEntityPlayer(GameProfile profile) {
		WorldServer worldServer = MinecraftServer.getServer().getWorldServer(0);

		return new EntityPlayer(MinecraftServer.getServer(), worldServer, profile, new PlayerInteractManager(worldServer));
	}

	@Override
	public int playerPing(Player player) {
		return getPlayerHandle(player).ping;
	}

	@Override
	public double serverTps() {
		return MinecraftServer.getServer().recentTps[0];
	}

	@Override
	public PacketPlayOutPlayerInfo updateDisplayNamePacket(Object entityPlayer, Object component, boolean listName) {
		if (listName) {
			setListName(entityPlayer, component);
		}

		return new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.UPDATE_DISPLAY_NAME, (EntityPlayer) entityPlayer);
	}

	@Override
	public void setListName(Object entityPlayer, Object component) {
		((EntityPlayer) entityPlayer).listName = (IChatBaseComponent) component;
	}

	@Override
	public PacketPlayOutPlayerInfo newPlayerInfoUpdatePacketAdd(Object... entityPlayers) {
		List<EntityPlayer> players = new ArrayList<>(entityPlayers.length);

		for (Object one : entityPlayers) {
			players.add((EntityPlayer) one);
		}

		return new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, players);
	}

	@Override
	public PacketPlayOutPlayerInfo updateLatency(Object entityPlayer) {
		return new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.UPDATE_LATENCY, (EntityPlayer) entityPlayer);
	}

	@Override
	public PacketPlayOutPlayerInfo removeEntityPlayers(Object... entityPlayers) {
		List<EntityPlayer> players = new ArrayList<>(entityPlayers.length);

		for (Object one : entityPlayers) {
			players.add((EntityPlayer) one);
		}

		return new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, players);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setInfoData(Object info, UUID id, int ping, Object component) {
		PacketPlayOutPlayerInfo update = (PacketPlayOutPlayerInfo) info;

		try {
			for (PacketPlayOutPlayerInfo.PlayerInfoData playerInfo : (List<PacketPlayOutPlayerInfo.PlayerInfoData>) infoList.get(update)) {
				if (playerInfo.a().getId().equals(id)) {
					setEntriesField(update, Collections.singletonList(update.new PlayerInfoData(playerInfo.a(), ping == -2 ? playerInfo.b() : ping, playerInfo.c(), (IChatBaseComponent) component)));
					break;
				}
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	private void setEntriesField(PacketPlayOutPlayerInfo playerInfoPacket, List<PacketPlayOutPlayerInfo.PlayerInfoData> list) {
		try {

			// Entries list is immutable, so use reflection to bypass
			if (entriesField == null) {
				entriesField = playerInfoPacket.getClass().getDeclaredField("b");
				entriesField.setAccessible(true);
			}

			entriesField.set(playerInfoPacket, list);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void createBoardTeam(String teamName, Player player, boolean followNameTagVisibility) {
		ScoreboardTeam playerTeam = scoreboard.createTeam(teamName);

		scoreboard.addPlayerToTeam(player.getName(), teamName);

		if (followNameTagVisibility) {
			ScoreboardTeamBase.EnumNameTagVisibility visibility = null;

			for (Team team : player.getScoreboard().getTeams()) {
				NameTagVisibility optionStatus = team.getNameTagVisibility();

				switch (optionStatus) {
				case HIDE_FOR_OTHER_TEAMS:
					visibility = ScoreboardTeamBase.EnumNameTagVisibility.HIDE_FOR_OTHER_TEAMS;
					break;
				case HIDE_FOR_OWN_TEAM:
					visibility = ScoreboardTeamBase.EnumNameTagVisibility.HIDE_FOR_OWN_TEAM;
					break;
				default:
					if (optionStatus != NameTagVisibility.ALWAYS) {
						visibility = ScoreboardTeamBase.EnumNameTagVisibility.NEVER;
					}

					break;
				}
			}

			if (visibility != null) {
				playerTeam.setNameTagVisibility(visibility);
			}
		}

		EntityPlayer handle = getPlayerHandle(player);

		if (tagTeams.isEmpty()) {
			sendPacket(handle, new PacketPlayOutScoreboardTeam(playerTeam, 0));
		} else {
			for (TagTeam tagTeam : tagTeams) {
				if (!tagTeam.playerName.equals(player.getName())) {
					continue;
				}

				tagTeam.scoreboardTeam.setDisplayName(playerTeam.getDisplayName());
				tagTeam.scoreboardTeam.setNameTagVisibility(playerTeam.getNameTagVisibility());

				sendPacket(handle, new PacketPlayOutScoreboardTeam(playerTeam, 0));
				sendPacket(handle, new PacketPlayOutScoreboardTeam(tagTeam.scoreboardTeam, 0));
				break;
			}
		}
	}

	@Override
	public PacketPlayOutScoreboardTeam unregisterBoardTeam(String teamName) {
		java.util.Collection<ScoreboardTeam> teams = scoreboard.getTeams();

		synchronized (teams) {
			for (ScoreboardTeam team : new ArrayList<>(teams)) {
				if (team.getName().equals(teamName)) {
					scoreboard.removeTeam(team);
					return new PacketPlayOutScoreboardTeam(team, 1);
				}
			}
		}

		return null;
	}

	@Override
	public ScoreboardObjective createObjectivePacket(String objectiveName, Object nameComponent) {
		Scoreboard scoreboard = new Scoreboard();
		ScoreboardObjective objective = new ScoreboardObjective(scoreboard, objectiveName, IScoreboardCriteria.b);
		objectiveStorage.add(new ObjectiveStorage(scoreboard, objective));

		return objective;
	}

	@Override
	public PacketPlayOutScoreboardObjective scoreboardObjectivePacket(Object objective, int mode) {
		return new PacketPlayOutScoreboardObjective((ScoreboardObjective) objective, mode);
	}

	@Override
	public PacketPlayOutScoreboardDisplayObjective scoreboardDisplayObjectivePacket(Object objective, int slot) {
		return new PacketPlayOutScoreboardDisplayObjective(slot, (ScoreboardObjective) objective);
	}

	@Override
	public PacketPlayOutScoreboardScore changeScoreboardScorePacket(String objectiveName, String scoreName, int score) {
		for (ObjectiveStorage objectiveStorage : objectiveStorage) {
			if (objectiveStorage.objective.getName().equals(objectiveName)) {
				ScoreboardScore scoreboardScore = new ScoreboardScore(objectiveStorage.scoreboard, objectiveStorage.objective, scoreName);
				scoreboardScore.setScore(score);

				return new PacketPlayOutScoreboardScore(scoreboardScore);
			}
		}

		return null;
	}

	@Override
	public PacketPlayOutScoreboardScore removeScoreboardScorePacket(String objectiveName, String scoreName, int score) {
		return objectiveStorage.removeIf(storage -> storage.objective.getName().equals(objectiveName)) ? new PacketPlayOutScoreboardScore(objectiveName) : null;
	}

	@Override
	public ScoreboardObjective createScoreboardHealthObjectivePacket(String objectiveName, Object nameComponent) {
		return new ScoreboardObjective(null, objectiveName, IScoreboardCriteria.g);
	}

	private static class ObjectiveStorage {

		private final Scoreboard scoreboard;
		private final ScoreboardObjective objective;

		public ObjectiveStorage(Scoreboard scoreboard, ScoreboardObjective objective) {
			this.scoreboard = scoreboard;
			this.objective = objective;
		}
	}

	private final class PacketReceivingListener extends io.netty.channel.ChannelDuplexHandler {

		private final UUID listenerPlayerId;
		private final List<Class<?>> classesToListen;

		private boolean packetListeningAllowed = true;

		public PacketReceivingListener(UUID listenerPlayerId, List<Class<?>> classesToListen) {
			this.listenerPlayerId = listenerPlayerId;
			this.classesToListen = classesToListen;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void write(ChannelHandlerContext ctx, Object msg, io.netty.channel.ChannelPromise promise) throws Exception {
			if (!packetListeningAllowed) {
				super.write(ctx, msg, promise);
				return;
			}

			Class<?> receivingClass = msg.getClass();

			for (Class<?> cl : classesToListen) {
				if (cl != receivingClass) {
					continue;
				}

				// Temporal and disgusting solution to fix players name tag overwriting
				if (cl == PacketPlayOutScoreboardTeam.class) {
					PacketPlayOutScoreboardTeam packetScoreboardTeam = (PacketPlayOutScoreboardTeam) msg;
					Collection<String> players = (Collection<String>) scoreboardTeamPlayers.get(packetScoreboardTeam);

					if (players != null && !players.isEmpty()) {
						ScoreboardTeamBase.EnumNameTagVisibility enumNameTagVisibility = ScoreboardTeamBase.EnumNameTagVisibility
								.a((String) scoreboardTeamNameTagVisibility.get(packetScoreboardTeam));

						if (enumNameTagVisibility == null) {
							enumNameTagVisibility = ScoreboardTeamBase.EnumNameTagVisibility.ALWAYS;
						}

						if (enumNameTagVisibility == ScoreboardTeamBase.EnumNameTagVisibility.NEVER) {
							return;
						}

						String prefix = (String) scoreboardTeamPrefix.get(packetScoreboardTeam);
						String suffix = (String) scoreboardTeamSuffix.get(packetScoreboardTeam);

						if (!prefix.isEmpty() || !suffix.isEmpty()) {
							String playerName = players.iterator().next();

							for (TagTeam team : tagTeams) {
								if (team.playerName.equals(playerName)) {
									return;
								}
							}

							Player player = Bukkit.getPlayer(playerName);

							if (player == null) {
								return;
							}

							EnumChatFormat enumChatFormat = EnumChatFormat.a((int) scoreboardTeamEnumChatFormat.get(packetScoreboardTeam));

							if (enumChatFormat == null) {
								enumChatFormat = EnumChatFormat.RESET;
							}

							ScoreboardTeam scoreboardTeam = new ScoreboardTeam(((org.bukkit.craftbukkit.v1_8_R3.scoreboard.CraftScoreboard) player.getScoreboard()).getHandle(),
									(String) scoreboardTeamName.get(packetScoreboardTeam));

							scoreboardTeam.setPrefix(prefix);
							scoreboardTeam.setSuffix(suffix);
							scoreboardTeam.setNameTagVisibility(enumNameTagVisibility);
							scoreboardTeam.a(enumChatFormat);
							scoreboardTeam.getPlayerNameSet().add(playerName);

							tagTeams.add(new TagTeam(playerName, scoreboardTeam));
						}
					}

					super.write(ctx, msg, promise);
					return;
				}

				PacketPlayOutPlayerInfo playerInfoPacket = (PacketPlayOutPlayerInfo) msg;

				if (playerInfoAction.get(playerInfoPacket) == PacketPlayOutPlayerInfo.EnumPlayerInfoAction.UPDATE_GAME_MODE) {
					Player player = Bukkit.getPlayer(listenerPlayerId);

					if (player == null) {
						break;
					}

					PacketPlayOutPlayerInfo updatePacket = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.UPDATE_GAME_MODE,
							Collections.emptyList());
					List<PacketPlayOutPlayerInfo.PlayerInfoData> players = new ArrayList<>();

					for (PacketPlayOutPlayerInfo.PlayerInfoData infoData : (List<PacketPlayOutPlayerInfo.PlayerInfoData>) infoList.get(playerInfoPacket)) {
						if (infoData.c() == EnumGamemode.SPECTATOR && !infoData.a().getId().equals(listenerPlayerId)) {
							players.add(playerInfoPacket.new PlayerInfoData(infoData.a(), infoData.b(), EnumGamemode.SURVIVAL, infoData.d()));
						}
					}

					setEntriesField(updatePacket, players);
					sendPacket(player, updatePacket);
				}

				break;
			}

			super.write(ctx, msg, promise);
		}
	}

	private static class TagTeam {

		public final String playerName;
		public final ScoreboardTeam scoreboardTeam;

		public TagTeam(String playerName, ScoreboardTeam scoreboardTeam) {
			this.playerName = playerName;
			this.scoreboardTeam = scoreboardTeam;
		}

		@Override
		public boolean equals(Object other) {
			return other != null && getClass() == other.getClass() && playerName.equals(((TagTeam) other).playerName);
		}

		@Override
		public int hashCode() {
			return java.util.Objects.hash(playerName);
		}
	}
}
