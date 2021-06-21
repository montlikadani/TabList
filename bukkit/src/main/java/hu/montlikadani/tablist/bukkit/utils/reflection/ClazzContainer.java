package hu.montlikadani.tablist.bukkit.utils.reflection;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import hu.montlikadani.tablist.bukkit.utils.ServerVersion;

public final class ClazzContainer {

	private static Field infoList, scoreboardTeamName, scoreboardTeamDisplayName, scoreboardTeamPrefix,
			scoreboardTeamSuffix, scoreboardTeamNames, scoreboardTeamMode, scoreboardPlayers, nameTagVisibility;

	private static Class<?> iChatBaseComponent, packet, packetPlayOutPlayerInfo, enumPlayerInfoAction,
			entityPlayerClass, enumGameMode, playerInfoData, minecraftServer, packetPlayOutScoreboardTeam,
			scoreboardNameTagVisibility, scoreboardTeamClass, scoreboardClass;

	private static Object gameModeNotSet, gameModeSpectator, gameModeSurvival, addPlayer, removePlayer, updateGameMode,
			updateLatency, updateDisplayName;

	private static Method scoreboardTeamSetPrefix, scoreboardTeamSetSuffix, scoreboardTeamSetNameTagVisibility,
			scoreboardTeamSetDisplayName, packetScoreboardTeamRemove, packetScoreboardTeamUpdateCreate,
			packetScoreboardTeamEntries;

	private static Constructor<?> playerInfoDataConstr, playOutPlayerInfoConstructor, scoreboardConstructor,
			scoreboardTeamConstructor, packetPlayOutScoreboardTeamConstructor;

	private static Constructor<?>[] playerInfoDataConstructors;

	private static Object[] scoreboardNameTagVisibilityEnumConstants;

	static {
		try {
			iChatBaseComponent = classByName("net.minecraft.network.chat", "IChatBaseComponent");
			packet = classByName("net.minecraft.network.protocol", "Packet");
			packetPlayOutPlayerInfo = classByName("net.minecraft.network.protocol.game", "PacketPlayOutPlayerInfo");
			entityPlayerClass = classByName("net.minecraft.server.level", "EntityPlayer");
			packetPlayOutScoreboardTeam = classByName("net.minecraft.network.protocol.game",
					"PacketPlayOutScoreboardTeam");

			try {
				minecraftServer = classByName("net.minecraft.server", "MinecraftServer");
			} catch (ClassNotFoundException c) {
				try {
					minecraftServer = classByName("net.minecraft.server.dedicated", "DedicatedServer");
				} catch (ClassNotFoundException e) {
				}
			}

			try {
				enumPlayerInfoAction = classByName(null, "EnumPlayerInfoAction");
			} catch (ClassNotFoundException c) {
				for (Class<?> clazz : packetPlayOutPlayerInfo.getClasses()) {
					if (clazz.getName().contains("EnumPlayerInfoAction")) {
						enumPlayerInfoAction = clazz;
						break;
					}
				}
			}

			try {
				addPlayer = enumPlayerInfoAction.getDeclaredField("ADD_PLAYER").get(enumPlayerInfoAction);
				updateGameMode = enumPlayerInfoAction.getDeclaredField("UPDATE_GAME_MODE").get(enumPlayerInfoAction);
				updateLatency = enumPlayerInfoAction.getDeclaredField("UPDATE_LATENCY").get(enumPlayerInfoAction);
				updateDisplayName = enumPlayerInfoAction.getDeclaredField("UPDATE_DISPLAY_NAME")
						.get(enumPlayerInfoAction);
				removePlayer = enumPlayerInfoAction.getDeclaredField("REMOVE_PLAYER").get(enumPlayerInfoAction);
			} catch (NoSuchFieldException ex) {
				addPlayer = enumPlayerInfoAction.getDeclaredField("a").get(enumPlayerInfoAction);
				updateGameMode = enumPlayerInfoAction.getDeclaredField("b").get(enumPlayerInfoAction);
				updateLatency = enumPlayerInfoAction.getDeclaredField("c").get(enumPlayerInfoAction);
				updateDisplayName = enumPlayerInfoAction.getDeclaredField("d").get(enumPlayerInfoAction);
				removePlayer = enumPlayerInfoAction.getDeclaredField("e").get(enumPlayerInfoAction);
			}

			if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_17_R1)) {
				scoreboardNameTagVisibility = classByName("net.minecraft.world.scores",
						"ScoreboardTeamBase$EnumNameTagVisibility");
				scoreboardTeamClass = classByName("net.minecraft.world.scores", "ScoreboardTeam");
				scoreboardClass = classByName("net.minecraft.world.scores", "Scoreboard");

				scoreboardConstructor = scoreboardClass.getConstructor();

				scoreboardTeamSetDisplayName = scoreboardTeamClass.getMethod("setDisplayName", iChatBaseComponent);
				scoreboardTeamSetPrefix = scoreboardTeamClass.getMethod("setPrefix", iChatBaseComponent);
				scoreboardTeamSetSuffix = scoreboardTeamClass.getMethod("setSuffix", iChatBaseComponent);
				scoreboardTeamSetNameTagVisibility = scoreboardTeamClass.getMethod("setNameTagVisibility",
						scoreboardNameTagVisibility);

				packetScoreboardTeamRemove = packetPlayOutScoreboardTeam.getMethod("a", scoreboardTeamClass);
				packetScoreboardTeamUpdateCreate = packetPlayOutScoreboardTeam.getMethod("a", scoreboardTeamClass,
						boolean.class);

				Class<?> packetPlayOutScoreboardTeam$a = classByName("net.minecraft.network.protocol.game",
						"PacketPlayOutScoreboardTeam$a");

				packetScoreboardTeamEntries = packetPlayOutScoreboardTeam.getMethod("a", scoreboardTeamClass,
						String.class, packetPlayOutScoreboardTeam$a);

				scoreboardNameTagVisibilityEnumConstants = scoreboardNameTagVisibility.getEnumConstants();
				scoreboardTeamConstructor = scoreboardTeamClass.getConstructor(scoreboardClass, String.class);
			} else {
				packetPlayOutScoreboardTeamConstructor = packetPlayOutScoreboardTeam.getConstructor();

				(scoreboardTeamName = packetPlayOutScoreboardTeam.getDeclaredField("a")).setAccessible(true);
				(scoreboardTeamDisplayName = packetPlayOutScoreboardTeam.getDeclaredField("b")).setAccessible(true);
				(scoreboardTeamPrefix = packetPlayOutScoreboardTeam.getDeclaredField("c")).setAccessible(true);
				(scoreboardTeamSuffix = packetPlayOutScoreboardTeam.getDeclaredField("d")).setAccessible(true);
				(nameTagVisibility = packetPlayOutScoreboardTeam.getDeclaredField("e")).setAccessible(true);

				(scoreboardTeamNames = packetPlayOutScoreboardTeam
						.getDeclaredField(ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_9_R1) ? "h" : "g"))
								.setAccessible(true);
				(scoreboardTeamMode = packetPlayOutScoreboardTeam
						.getDeclaredField(ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_9_R1) ? "i" : "h"))
								.setAccessible(true);
				(scoreboardPlayers = packetPlayOutScoreboardTeam
						.getDeclaredField(ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_9_R1) ? "h" : "e"))
								.setAccessible(true);
			}

			(infoList = packetPlayOutPlayerInfo.getDeclaredField("b")).setAccessible(true);

			try {
				playerInfoData = classByName("net.minecraft.network.protocol.game",
						"PacketPlayOutPlayerInfo$PlayerInfoData");
			} catch (ClassNotFoundException e) {
				playerInfoData = classByName(null, "PlayerInfoData");
			}

			if (playerInfoData != null) {
				playerInfoDataConstructors = playerInfoData.getConstructors();

				for (Constructor<?> constr : playerInfoDataConstructors) {
					if (constr.getParameterCount() == 4 || constr.getParameterCount() == 5) {
						(playerInfoDataConstr = constr).setAccessible(true);
						break;
					}
				}
			}

			Class<?> entityPlayerArrayClass = Array.newInstance(entityPlayerClass, 0).getClass();

			(playOutPlayerInfoConstructor = packetPlayOutPlayerInfo.getDeclaredConstructor(enumPlayerInfoAction,
					entityPlayerArrayClass)).setAccessible(true);

			try {
				enumGameMode = classByName("net.minecraft.world.level", "EnumGamemode");
			} catch (ClassNotFoundException e) {
				enumGameMode = classByName(null, "WorldSettings$EnumGamemode");
			}

			try {
				gameModeNotSet = enumGameMode.getDeclaredField("NOT_SET").get(enumGameMode);
				gameModeSpectator = enumGameMode.getDeclaredField("SPECTATOR").get(enumGameMode);
				gameModeSurvival = enumGameMode.getDeclaredField("SURVIVAL").get(enumGameMode);
			} catch (NoSuchFieldException ex) {
				Field field = enumGameMode.getDeclaredField("f");
				field.setAccessible(true);

				gameModeNotSet = field.get(enumGameMode);

				(field = enumGameMode.getDeclaredField("d")).setAccessible(true);
				gameModeSpectator = field.get(enumGameMode);

				(field = enumGameMode.getDeclaredField("a")).setAccessible(true);
				gameModeSurvival = field.get(enumGameMode);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private ClazzContainer() {
	}

	// In ReflectionUtils static clause calls this class again which causes NPE
	private static Class<?> classByName(String newPackageName, String name) throws ClassNotFoundException {
		if (ServerVersion.isCurrentLower(ServerVersion.v1_17_R1) || newPackageName == null) {
			newPackageName = "net.minecraft.server." + ServerVersion.getArrayVersion()[3];
		}

		return Class.forName(newPackageName + "." + name);
	}

	public static Object scoreboardTeamPacketByAction(Object scoreboardTeam, int action) throws Exception {
		switch (action) {
		case 0:
			return ClazzContainer.getPacketScoreboardTeamUpdateCreate()
					.invoke(ClazzContainer.getPacketPlayOutScoreboardTeam(), scoreboardTeam, true);
		case 1:
			return ClazzContainer.getPacketScoreboardTeamRemove()
					.invoke(ClazzContainer.getPacketPlayOutScoreboardTeam(), scoreboardTeam);
		case 2:
			return ClazzContainer.getPacketScoreboardTeamUpdateCreate()
					.invoke(ClazzContainer.getPacketPlayOutScoreboardTeam(), scoreboardTeam, false);
		default:
			return null;
		}
	}

	public static Class<?> getPacket() {
		return packet;
	}

	public static Field getInfoList() {
		return infoList;
	}

	public static Constructor<?> getScoreboardConstructor() {
		return scoreboardConstructor;
	}

	public static Constructor<?> getPacketPlayOutScoreboardTeamConstructor() {
		return packetPlayOutScoreboardTeamConstructor;
	}

	public static Constructor<?> getScoreboardTeamConstructor() {
		return scoreboardTeamConstructor;
	}

	public static Class<?> getPacketPlayOutScoreboardTeam() {
		return packetPlayOutScoreboardTeam;
	}

	public static Constructor<?> getPlayerInfoDataConstructor() {
		return playerInfoDataConstr;
	}

	public static Constructor<?> getPlayOutPlayerInfoConstructor() {
		return playOutPlayerInfoConstructor;
	}

	public static Class<?> getPacketPlayOutPlayerInfo() {
		return packetPlayOutPlayerInfo;
	}

	public static Class<?> getEnumPlayerInfoAction() {
		return enumPlayerInfoAction;
	}

	public static Class<?> getEntityPlayerClass() {
		return entityPlayerClass;
	}

	public static Class<?> getEnumGameMode() {
		return enumGameMode;
	}

	public static Class<?> getPlayerInfoData() {
		return playerInfoData;
	}

	public static Object getGameModeNotSet() {
		return gameModeNotSet;
	}

	public static Object getGameModeSpectator() {
		return gameModeSpectator;
	}

	public static Object getGameModeSurvival() {
		return gameModeSurvival;
	}

	public static Constructor<?>[] getPlayerInfoDataConstructors() {
		return playerInfoDataConstructors;
	}

	public static Object getAddPlayer() {
		return addPlayer;
	}

	public static Object getRemovePlayer() {
		return removePlayer;
	}

	public static Object getUpdateGameMode() {
		return updateGameMode;
	}

	public static Object getUpdateLatency() {
		return updateLatency;
	}

	public static Object getUpdateDisplayName() {
		return updateDisplayName;
	}

	public static Class<?> getMinecraftServer() {
		return minecraftServer;
	}

	public static Class<?> getIChatBaseComponent() {
		return iChatBaseComponent;
	}

	public static Class<?> getScoreboardNameTagVisibility() {
		return scoreboardNameTagVisibility;
	}

	public static Class<?> getScoreboardTeamClass() {
		return scoreboardTeamClass;
	}

	public static Method getScoreboardTeamSetPrefix() {
		return scoreboardTeamSetPrefix;
	}

	public static Method getScoreboardTeamSetSuffix() {
		return scoreboardTeamSetSuffix;
	}

	public static Method getScoreboardTeamSetNameTagVisibility() {
		return scoreboardTeamSetNameTagVisibility;
	}

	public static Method getScoreboardTeamSetDisplayName() {
		return scoreboardTeamSetDisplayName;
	}

	public static Method getPacketScoreboardTeamRemove() {
		return packetScoreboardTeamRemove;
	}

	public static Method getPacketScoreboardTeamUpdateCreate() {
		return packetScoreboardTeamUpdateCreate;
	}

	public static Method getPacketScoreboardTeamEntries() {
		return packetScoreboardTeamEntries;
	}

	public static Field getScoreboardTeamName() {
		return scoreboardTeamName;
	}

	public static Field getScoreboardTeamDisplayName() {
		return scoreboardTeamDisplayName;
	}

	public static Field getScoreboardTeamPrefix() {
		return scoreboardTeamPrefix;
	}

	public static Field getScoreboardTeamSuffix() {
		return scoreboardTeamSuffix;
	}

	public static Field getScoreboardTeamNames() {
		return scoreboardTeamNames;
	}

	public static Field getScoreboardTeamMode() {
		return scoreboardTeamMode;
	}

	public static Field getScoreboardPlayers() {
		return scoreboardPlayers;
	}

	public static Field getNameTagVisibility() {
		return nameTagVisibility;
	}

	public static Object[] getScoreboardNameTagVisibilityEnumConstants() {
		return scoreboardNameTagVisibilityEnumConstants;
	}
}
