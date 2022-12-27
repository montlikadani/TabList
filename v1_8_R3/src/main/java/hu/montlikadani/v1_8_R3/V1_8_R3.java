package hu.montlikadani.v1_8_R3;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
import net.minecraft.server.v1_8_R3.PacketPlayInArmAnimation;
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

	private final List<ScoreboardTeam> playerTeams = new ArrayList<>();
	private final List<ObjectiveStorage> objectiveStorage = new ArrayList<>();

	private Field headerField, footerField, entriesField, infoList, playerInfoAction;
	private final IChatBaseComponent emptyComponent = IChatBaseComponent.ChatSerializer.a("");

	public V1_8_R3() {
		try {
			(headerField = PacketPlayOutPlayerListHeaderFooter.class.getDeclaredField("a")).setAccessible(true);
			(footerField = PacketPlayOutPlayerListHeaderFooter.class.getDeclaredField("b")).setAccessible(true);
			(infoList = PacketPlayOutPlayerInfo.class.getDeclaredField("b")).setAccessible(true);
			(playerInfoAction = PacketPlayOutPlayerInfo.class.getDeclaredField("a")).setAccessible(true);
		} catch (NoSuchFieldException | SecurityException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void sendPacket(Player player, Object packet) {
		getPlayerHandle(player).playerConnection.sendPacket((Packet<?>) packet);
	}

	private void sendPacket(EntityPlayer player, Packet<?> packet) {
		player.playerConnection.sendPacket(packet);
	}

	@Override
	public void addPlayerChannelListener(Player player, Class<?>... classesToListen) {
		EntityPlayer entityPlayer = getPlayerHandle(player);

		if (entityPlayer.playerConnection.networkManager.channel.pipeline().get("PacketInjector") == null) {
			entityPlayer.playerConnection.networkManager.channel.pipeline().addBefore("packet_handler", "PacketInjector",
					new PacketReceivingListener(entityPlayer.getUniqueID(), classesToListen));
		}
	}

	@Override
	public void removePlayerChannelListener(Player player) {
		EntityPlayer entityPlayer = getPlayerHandle(player);

		if (entityPlayer.playerConnection.networkManager.channel.pipeline().get("PacketInjector") != null) {
			entityPlayer.playerConnection.networkManager.channel.pipeline().remove("PacketInjector");
		}
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
	public void addPlayersToTab(Player source, Player... targets) {
		List<EntityPlayer> players = new ArrayList<>(targets.length);

		for (Player player : targets) {
			players.add(getPlayerHandle(player));
		}

		sendPacket(source, new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, players));
	}

	@Override
	public void removePlayersFromTab(Player source, Collection<? extends Player> players) {
		sendPacket(getPlayerHandle(source),
				new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, players.stream().map(this::getPlayerHandle).collect(Collectors.toList())));
	}

	@Override
	public void appendPlayerWithoutListed(Player source) {
		EntityPlayer from = getPlayerHandle(source);
		PacketPlayOutPlayerInfo updatePacket = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, Collections.singletonList(from));

		setEntriesField(updatePacket, () -> updatePacket.new PlayerInfoData(from.getProfile(), from.ping, from.playerInteractManager.getGameMode(), emptyComponent));

		PacketPlayInArmAnimation animatePacket = new PacketPlayInArmAnimation();

		for (Player player : org.bukkit.Bukkit.getServer().getOnlinePlayers()) {
			EntityPlayer entityPlayer = getPlayerHandle(player);

			sendPacket(entityPlayer, updatePacket);
			sendPacket(entityPlayer, animatePacket);
		}
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
					setEntriesField(update, () -> update.new PlayerInfoData(playerInfo.a(), ping == -2 ? playerInfo.b() : ping, playerInfo.c(), (IChatBaseComponent) component));
					break;
				}
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	private void setEntriesField(PacketPlayOutPlayerInfo playerInfoPacket, java.util.function.Supplier<PacketPlayOutPlayerInfo.PlayerInfoData> supplier) {
		try {

			// Entries list is immutable, so use reflection to bypass
			if (entriesField == null) {
				entriesField = playerInfoPacket.getClass().getDeclaredField("b");
				entriesField.setAccessible(true);
			}

			entriesField.set(playerInfoPacket, Collections.singletonList(supplier.get()));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public PacketPlayOutScoreboardTeam createBoardTeam(Object teamName, Player player, boolean followNameTagVisibility) {
		String textComponent = ((IChatBaseComponent) teamName).getText();
		ScoreboardTeam playerTeam = new ScoreboardTeam(new Scoreboard(), textComponent);

		playerTeam.getPlayerNameSet().add(player.getName());
		playerTeam.setDisplayName(textComponent);

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

		playerTeams.add(playerTeam);
		return new PacketPlayOutScoreboardTeam(playerTeam, 0);
	}

	@Override
	public PacketPlayOutScoreboardTeam unregisterBoardTeam(Object playerTeam) {
		ScoreboardTeam team = (ScoreboardTeam) playerTeam;
		playerTeams.remove(team);

		return new PacketPlayOutScoreboardTeam(team, 1);
	}

	@Override
	public ScoreboardTeam findBoardTeamByName(String teamName) {
		for (ScoreboardTeam team : playerTeams) {
			if (team.getName().equals(teamName)) {
				return team;
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
		ObjectiveStorage objectiveStorage = findByObjective(objectiveName);

		if (objectiveStorage == null) {
			return null;
		}

		ScoreboardScore scoreboardScore = new ScoreboardScore(objectiveStorage.scoreboard, objectiveStorage.objective, scoreName);
		scoreboardScore.setScore(score);

		return new PacketPlayOutScoreboardScore(scoreboardScore);
	}

	@Override
	public PacketPlayOutScoreboardScore removeScoreboardScorePacket(String objectiveName, String scoreName, int score) {
		return objectiveStorage.removeIf(storage -> storage.objective.getName().equals(objectiveName)) ? new PacketPlayOutScoreboardScore(objectiveName) : null;
	}

	@Override
	public ScoreboardObjective createScoreboardHealthObjectivePacket(String objectiveName, Object nameComponent) {
		return new ScoreboardObjective(null, objectiveName, IScoreboardCriteria.g);
	}

	private ObjectiveStorage findByObjective(String name) {
		for (ObjectiveStorage objectiveStorage : objectiveStorage) {
			if (objectiveStorage.objective.getName().equals(name)) {
				return objectiveStorage;
			}
		}

		return null;
	}

	private final class ObjectiveStorage {

		private final Scoreboard scoreboard;
		private final ScoreboardObjective objective;

		public ObjectiveStorage(Scoreboard scoreboard, ScoreboardObjective objective) {
			this.scoreboard = scoreboard;
			this.objective = objective;
		}
	}

	private final class PacketReceivingListener extends io.netty.channel.ChannelDuplexHandler {

		private final UUID listenerPlayerId;
		private final Class<?>[] classesToListen;

		public PacketReceivingListener(UUID listenerPlayerId, Class<?>... classesToListen) {
			this.listenerPlayerId = listenerPlayerId;
			this.classesToListen = classesToListen;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void write(ChannelHandlerContext ctx, Object msg, io.netty.channel.ChannelPromise promise) throws Exception {
			Class<?> receivingClass = msg.getClass();

			for (Class<?> cl : classesToListen) {
				if (cl != receivingClass) {
					continue;
				}

				PacketPlayOutPlayerInfo playerInfoPacket = (PacketPlayOutPlayerInfo) msg;

				if (playerInfoAction.get(playerInfoPacket) == PacketPlayOutPlayerInfo.EnumPlayerInfoAction.UPDATE_GAME_MODE) {
					for (PacketPlayOutPlayerInfo.PlayerInfoData infoData : (List<PacketPlayOutPlayerInfo.PlayerInfoData>) infoList.get(playerInfoPacket)) {
						if (infoData.c() == EnumGamemode.SPECTATOR && !infoData.a().getId().equals(listenerPlayerId)) {
							setEntriesField(playerInfoPacket, () -> playerInfoPacket.new PlayerInfoData(infoData.a(), infoData.b(), EnumGamemode.CREATIVE, infoData.d()));
						}
					}
				}

				break;
			}

			super.write(ctx, msg, promise);
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			super.channelRead(ctx, msg);
		}
	}
}
