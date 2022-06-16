package hu.montlikadani.tablist.utils.reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import hu.montlikadani.tablist.utils.ServerVersion;

public final class ClazzContainer {

	private static Field infoList, scoreboardTeamName, scoreboardTeamDisplayName, scoreboardTeamPrefix, scoreboardTeamSuffix,
			scoreboardTeamNames, scoreboardTeamMode, scoreboardPlayers, nameTagVisibility, playerInfoDataProfileField,
			playerInfoDataPing, playerInfoDataGameMode, nameTagVisibilityNameField, scoreboardObjectiveMethod,
			packetPlayOutScoreboardObjectiveNameField, packetPlayOutScoreboardObjectiveDisplayNameField,
			packetPlayOutScoreboardObjectiveRenderType;

	private static Class<?> iChatBaseComponent, packet, packetPlayOutPlayerInfo, enumPlayerInfoAction, enumGameMode,
			playerInfoData, packetPlayOutScoreboardTeam, scoreboardNameTagVisibility, scoreboardTeamClass, scoreboardClass,
			scoreboardObjective;

	private static Object gameModeNotSet, gameModeSpectator, gameModeSurvival, addPlayer, removePlayer, updateGameMode,
			updateLatency, updateDisplayName, enumScoreboardHealthDisplayInteger, enumScoreboardActionChange,
			enumScoreboardActionRemove, iScoreboardCriteriaDummy;

	private static Method scoreboardTeamSetPrefix, scoreboardTeamSetSuffix, scoreboardTeamSetNameTagVisibility,
			scoreboardTeamSetDisplayName, packetScoreboardTeamRemove, packetScoreboardTeamUpdateCreate,
			packetScoreboardTeamEntries, playerInfoDataProfileMethod, playerNameSetMethod, setScoreboardScoreMethod;

	private static Constructor<?> playerInfoDataConstr, playOutPlayerInfoConstructor, scoreboardConstructor,
			scoreboardTeamConstructor, packetPlayOutScoreboardTeamConstructor, packetPlayOutScoreboardScoreConstructor,
			packetPlayOutScoreboardObjectiveConstructor, firstScoreboardObjectiveConstructor,
			packetPlayOutScoreboardDisplayObjectiveConstructor, scoreboardScoreConstructor,
			packetPlayOutScoreboardScoreSbScoreConstructor;

	private static Constructor<?>[] playerInfoDataConstructors;

	private static Object[] scoreboardNameTagVisibilityEnumConstants;

	private static boolean isTeamOptionStatusEnumExist = false;

	static {
		try {
			iChatBaseComponent = classByName("net.minecraft.network.chat", "IChatBaseComponent");
			packet = classByName("net.minecraft.network.protocol", "Packet");
			packetPlayOutPlayerInfo = classByName("net.minecraft.network.protocol.game", "PacketPlayOutPlayerInfo");
			packetPlayOutScoreboardTeam = classByName("net.minecraft.network.protocol.game", "PacketPlayOutScoreboardTeam");

			// Somehow the 1.8.8 server realizes that Team.OptionStatus enum class is exists
			//Class.forName("org.bukkit.scoreboard.Team$OptionStatus");
			try {
				org.bukkit.scoreboard.Team.class.getDeclaredMethod("getOption");
				isTeamOptionStatusEnumExist = true;
			} catch (NoSuchMethodException e) {
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
				updateDisplayName = enumPlayerInfoAction.getDeclaredField("UPDATE_DISPLAY_NAME").get(enumPlayerInfoAction);
				removePlayer = enumPlayerInfoAction.getDeclaredField("REMOVE_PLAYER").get(enumPlayerInfoAction);
			} catch (NoSuchFieldException ex) {
				addPlayer = enumPlayerInfoAction.getDeclaredField("a").get(enumPlayerInfoAction);
				updateGameMode = enumPlayerInfoAction.getDeclaredField("b").get(enumPlayerInfoAction);
				updateLatency = enumPlayerInfoAction.getDeclaredField("c").get(enumPlayerInfoAction);
				updateDisplayName = enumPlayerInfoAction.getDeclaredField("d").get(enumPlayerInfoAction);
				removePlayer = enumPlayerInfoAction.getDeclaredField("e").get(enumPlayerInfoAction);
			}

			scoreboardClass = classByName("net.minecraft.world.scores", "Scoreboard");
			scoreboardConstructor = scoreboardClass.getConstructor();

			if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_17_R1)) {
				scoreboardNameTagVisibility = classByName("net.minecraft.world.scores",
						"ScoreboardTeamBase$EnumNameTagVisibility");
				scoreboardTeamClass = classByName("net.minecraft.world.scores", "ScoreboardTeam");

				if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_18_R1)) {
					scoreboardTeamSetPrefix = scoreboardTeamClass.getMethod("b", iChatBaseComponent);
					scoreboardTeamSetSuffix = scoreboardTeamClass.getMethod("c", iChatBaseComponent);
					scoreboardTeamSetDisplayName = scoreboardTeamClass.getMethod("a", iChatBaseComponent);
					scoreboardTeamSetNameTagVisibility = scoreboardTeamClass.getMethod("a", scoreboardNameTagVisibility);
					playerNameSetMethod = scoreboardTeamClass.getMethod("g"); // getPlayers
					(scoreboardTeamNames = scoreboardTeamClass.getDeclaredField("e")).setAccessible(true); // players
				} else {
					scoreboardTeamSetPrefix = scoreboardTeamClass.getMethod("setPrefix", iChatBaseComponent);
					scoreboardTeamSetSuffix = scoreboardTeamClass.getMethod("setSuffix", iChatBaseComponent);
					scoreboardTeamSetDisplayName = scoreboardTeamClass.getMethod("setDisplayName", iChatBaseComponent);
					scoreboardTeamSetNameTagVisibility = scoreboardTeamClass.getMethod("setNameTagVisibility",
							scoreboardNameTagVisibility);
					playerNameSetMethod = scoreboardTeamClass.getMethod("getPlayerNameSet");
					(scoreboardTeamNames = scoreboardTeamClass.getDeclaredField("f")).setAccessible(true);
				}

				packetScoreboardTeamRemove = packetPlayOutScoreboardTeam.getMethod("a", scoreboardTeamClass);
				packetScoreboardTeamUpdateCreate = packetPlayOutScoreboardTeam.getMethod("a", scoreboardTeamClass, boolean.class);

				packetScoreboardTeamEntries = packetPlayOutScoreboardTeam.getMethod("a", scoreboardTeamClass, String.class,
						classByName("net.minecraft.network.protocol.game", "PacketPlayOutScoreboardTeam$a"));

				scoreboardNameTagVisibilityEnumConstants = scoreboardNameTagVisibility.getEnumConstants();

				Class<?> enumConstantClass = scoreboardNameTagVisibilityEnumConstants[0].getClass();
				try {
					nameTagVisibilityNameField = enumConstantClass.getDeclaredField("name");
				} catch (NoSuchFieldException ns) { // In case if name field not exist
					for (Field fields : enumConstantClass.getDeclaredFields()) {
						if (fields.getType() == String.class) {
							nameTagVisibilityNameField = fields;
							break;
						}
					}
				}

				scoreboardTeamConstructor = scoreboardTeamClass.getConstructor(scoreboardClass, String.class);
			} else {
				try {
					packetPlayOutScoreboardTeamConstructor = packetPlayOutScoreboardTeam.getDeclaredConstructor();
				} catch (NoSuchMethodException e) {
					packetPlayOutScoreboardTeamConstructor = packetPlayOutScoreboardTeam
							.getDeclaredConstructor(scoreboardTeamClass = classByName(null, "ScoreboardTeam"), int.class);
				}

				packetPlayOutScoreboardTeamConstructor.setAccessible(true);

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

			// Objectives
			Class<?> packetPlayOutScoreboardDisplayObjective = classByName("net.minecraft.network.protocol.game",
					"PacketPlayOutScoreboardDisplayObjective");
			scoreboardObjective = classByName("net.minecraft.world.scores", "ScoreboardObjective");

			packetPlayOutScoreboardDisplayObjectiveConstructor = packetPlayOutScoreboardDisplayObjective.getConstructor(int.class,
					scoreboardObjective);

			Class<?> enumScoreboardHealthDisplay;
			try {
				enumScoreboardHealthDisplay = classByName("net.minecraft.world.scores.criteria",
						"IScoreboardCriteria$EnumScoreboardHealthDisplay");
			} catch (ClassNotFoundException e) {
				enumScoreboardHealthDisplay = classByName("net.minecraft.world.scores.criteria", "EnumScoreboardHealthDisplay");
			}

			Class<?> packetPlayOutScoreboardObjective = classByName("net.minecraft.network.protocol.game",
					"PacketPlayOutScoreboardObjective");
			Class<?> packetPlayOutScoreboardScore = classByName("net.minecraft.network.protocol.game",
					"PacketPlayOutScoreboardScore");

			Class<?> iScoreboardCriteria = classByName("net.minecraft.world.scores.criteria", "IScoreboardCriteria");

			iScoreboardCriteriaDummy = getFieldByType(iScoreboardCriteria, iScoreboardCriteria, null).get(iScoreboardCriteria);

			/*try {
				iScoreboardCriteriaDummy = iScoreboardCriteria.getDeclaredField("b").get(iScoreboardCriteria);
			} catch (NoSuchFieldException e) {
				try {
					iScoreboardCriteriaDummy = iScoreboardCriteria.getDeclaredField("a").get(iScoreboardCriteria);
				} catch (NoSuchFieldException ex) {
					iScoreboardCriteriaDummy = iScoreboardCriteria.getDeclaredField("DUMMY").get(iScoreboardCriteria);
				}
			}*/

			try {
				enumScoreboardHealthDisplayInteger = enumScoreboardHealthDisplay.getDeclaredField("a")
						.get(enumScoreboardHealthDisplay);
			} catch (NoSuchFieldException e) {
				enumScoreboardHealthDisplayInteger = enumScoreboardHealthDisplay.getDeclaredField("INTEGER")
						.get(enumScoreboardHealthDisplay);
			}

			firstScoreboardObjectiveConstructor = scoreboardObjective.getConstructors()[0];

			if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_13_R2)) {
				Class<?> enumScoreboardAction;

				try {
					enumScoreboardAction = classByName("net.minecraft.server", "ScoreboardServer$Action");
				} catch (ClassNotFoundException e) {
					try {
						enumScoreboardAction = classByName("net.minecraft.server",
								"PacketPlayOutScoreboardScore$EnumScoreboardAction");
					} catch (ClassNotFoundException ex) {
						enumScoreboardAction = classByName("net.minecraft.server", "EnumScoreboardAction");
					}
				}

				try {
					enumScoreboardActionChange = enumScoreboardAction.getDeclaredField("a").get(enumScoreboardAction);
					enumScoreboardActionRemove = enumScoreboardAction.getDeclaredField("b").get(enumScoreboardAction);
				} catch (NoSuchFieldException e) {
					enumScoreboardActionChange = enumScoreboardAction.getDeclaredField("CHANGE").get(enumScoreboardAction);
					enumScoreboardActionRemove = enumScoreboardAction.getDeclaredField("REMOVE").get(enumScoreboardAction);
				}

				packetPlayOutScoreboardObjectiveConstructor = packetPlayOutScoreboardObjective.getConstructor(scoreboardObjective,
						int.class);
				packetPlayOutScoreboardScoreConstructor = packetPlayOutScoreboardScore.getConstructor(enumScoreboardAction,
						String.class, String.class, int.class);
			}/* else {
				Class<?> scoreboardScore = classByName("net.minecraft.world.scores", "ScoreboardScore");
				scoreboardScoreConstructor = scoreboardScore.getConstructor(scoreboardClass, scoreboardObjective,
						String.class);

				try {
					setScoreboardScoreMethod = scoreboardScore.getDeclaredMethod("setScore", int.class);
				} catch (NoSuchMethodException e) {
					setScoreboardScoreMethod = scoreboardScore.getDeclaredMethod("b", int.class);
				}

				packetPlayOutScoreboardObjectiveConstructor = packetPlayOutScoreboardObjective.getConstructor();
				packetPlayOutScoreboardScoreConstructor = packetPlayOutScoreboardScore.getConstructor(String.class);
				packetPlayOutScoreboardScoreSbScoreConstructor = packetPlayOutScoreboardScore
						.getConstructor(scoreboardScore);

				scoreboardObjectiveMethod = getFieldByType(packetPlayOutScoreboardObjective, int.class,
						field -> !java.lang.reflect.Modifier.isStatic(field.getModifiers()));

				packetPlayOutScoreboardObjectiveNameField = getFieldByType(packetPlayOutScoreboardObjective,
						String.class, null);
				packetPlayOutScoreboardObjectiveDisplayNameField = getFieldsByType(packetPlayOutScoreboardObjective,
						String.class).get(1);
				packetPlayOutScoreboardObjectiveRenderType = getFieldByType(packetPlayOutScoreboardObjective,
						enumScoreboardHealthDisplay, null);
			}*/

			// PlayerInfoData
			(infoList = packetPlayOutPlayerInfo.getDeclaredField("b")).setAccessible(true);

			try {
				playerInfoData = classByName("net.minecraft.network.protocol.game", "PacketPlayOutPlayerInfo$PlayerInfoData");
			} catch (ClassNotFoundException e) {
				playerInfoData = classByName(null, "PlayerInfoData");
			}

			playerInfoDataConstructors = playerInfoData.getConstructors();

			for (Constructor<?> constr : playerInfoDataConstructors) {
				int paramCount = constr.getParameterCount();

				if (paramCount == 4 || paramCount == 5) {
					(playerInfoDataConstr = constr).setAccessible(true);
					break;
				}
			}

			if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_17_R1)) {
				(playerInfoDataProfileField = playerInfoData.getDeclaredField("c")).setAccessible(true);
				(playerInfoDataPing = playerInfoData.getDeclaredField("a")).setAccessible(true);
				(playerInfoDataGameMode = playerInfoData.getDeclaredField("b")).setAccessible(true);
			} else {
				(playerInfoDataProfileMethod = playerInfoData.getDeclaredMethod("a")).setAccessible(true);
				(playerInfoDataPing = playerInfoData.getDeclaredField("b")).setAccessible(true);
				(playerInfoDataGameMode = playerInfoData.getDeclaredField("c")).setAccessible(true);
			}

			(playOutPlayerInfoConstructor = packetPlayOutPlayerInfo.getDeclaredConstructor(enumPlayerInfoAction,
					java.lang.reflect.Array.newInstance(classByName("net.minecraft.server.level", "EntityPlayer"), 0).getClass()))
							.setAccessible(true);

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
	protected static Class<?> classByName(String newPackageName, String name) throws ClassNotFoundException {
		if (ServerVersion.isCurrentLower(ServerVersion.v1_17_R1) || newPackageName == null) {
			newPackageName = "net.minecraft.server." + ServerVersion.getArrayVersion()[3];
		}

		return Class.forName(newPackageName + "." + name);
	}

	private static Field getFieldByType(Class<?> from, Class<?> type, java.util.function.Predicate<Field> predicate) {
		for (Field field : from.getDeclaredFields()) {
			if (field.getType() == type && (predicate == null || predicate.test(field))) {
				field.setAccessible(true);
				return field;
			}
		}

		return null;
	}

	/*private static java.util.List<Field> getFieldsByType(Class<?> from, Class<?> type) {
		Field[] declaredFields = from.getDeclaredFields();
		java.util.List<Field> fields = new java.util.ArrayList<>(declaredFields.length);

		for (Field field : declaredFields) {
			if (field.getType() == type) {
				field.setAccessible(true);
				fields.add(field);
			}
		}

		return fields;
	}*/

	public static Object scoreboardTeamPacketByAction(Object scoreboardTeam, int action) throws Exception {
		switch (action) {
		case 0:
			return packetScoreboardTeamUpdateCreate.invoke(packetPlayOutScoreboardTeam, scoreboardTeam, true);
		case 1:
			return packetScoreboardTeamRemove.invoke(packetPlayOutScoreboardTeam, scoreboardTeam);
		case 2:
			return packetScoreboardTeamUpdateCreate.invoke(packetPlayOutScoreboardTeam, scoreboardTeam, false);
		default:
			return null;
		}
	}

	public static boolean isTeamOptionStatusEnumExist() {
		return isTeamOptionStatusEnumExist;
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

	public static Field getPlayerInfoDataProfileField() {
		return playerInfoDataProfileField;
	}

	public static Field getPlayerInfoDataPing() {
		return playerInfoDataPing;
	}

	public static Method getPlayerInfoDataProfileMethod() {
		return playerInfoDataProfileMethod;
	}

	public static Field getPlayerInfoDataGameMode() {
		return playerInfoDataGameMode;
	}

	public static Method getPlayerNameSetMethod() {
		return playerNameSetMethod;
	}

	public static Field getNameTagVisibilityNameField() {
		return nameTagVisibilityNameField;
	}

	public static Constructor<?> getPacketPlayOutScoreboardScoreConstructor() {
		return packetPlayOutScoreboardScoreConstructor;
	}

	public static Constructor<?> getPacketPlayOutScoreboardObjectiveConstructor() {
		return packetPlayOutScoreboardObjectiveConstructor;
	}

	public static Class<?> getScoreboardObjective() {
		return scoreboardObjective;
	}

	public static Constructor<?> getFirstScoreboardObjectiveConstructor() {
		return firstScoreboardObjectiveConstructor;
	}

	public static Object getEnumScoreboardHealthDisplayInteger() {
		return enumScoreboardHealthDisplayInteger;
	}

	public static Constructor<?> getPacketPlayOutScoreboardDisplayObjectiveConstructor() {
		return packetPlayOutScoreboardDisplayObjectiveConstructor;
	}

	public static Object getEnumScoreboardActionChange() {
		return enumScoreboardActionChange;
	}

	public static Object getEnumScoreboardActionRemove() {
		return enumScoreboardActionRemove;
	}

	public static Object getiScoreboardCriteriaDummy() {
		return iScoreboardCriteriaDummy;
	}

	public static Field getPacketPlayOutScoreboardObjectiveNameField() {
		return packetPlayOutScoreboardObjectiveNameField;
	}

	public static Field getScoreboardObjectiveMethod() {
		return scoreboardObjectiveMethod;
	}

	public static Field getPacketPlayOutScoreboardObjectiveDisplayNameField() {
		return packetPlayOutScoreboardObjectiveDisplayNameField;
	}

	public static Field getPacketPlayOutScoreboardObjectiveRenderType() {
		return packetPlayOutScoreboardObjectiveRenderType;
	}

	public static Constructor<?> getScoreboardScoreConstructor() {
		return scoreboardScoreConstructor;
	}

	public static Method getSetScoreboardScoreMethod() {
		return setScoreboardScoreMethod;
	}

	public static Constructor<?> getPacketPlayOutScoreboardScoreSbScoreConstructor() {
		return packetPlayOutScoreboardScoreSbScoreConstructor;
	}
}
