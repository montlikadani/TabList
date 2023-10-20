package hu.montlikadani.tablist.utils.reflection;

import hu.montlikadani.tablist.utils.Util;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Optional;

import com.mojang.authlib.GameProfile;

public final class ClazzContainer {

	private static Field infoList, packetScoreboardTeamName, scoreboardTeamDisplayName, scoreboardTeamNames, packetScoreboardTeamMode, nameTagVisibility,
			playerInfoDataProfileField, playerInfoDataGameMode, actionField, playerInfoDataPing, playerInfoDisplayName, packetScoreboardTeamPrefix,
			packetScoreboardTeamSuffix, packetScoreboardTeamChatFormatColorField;

	private static Class<?> iChatBaseComponent, packet, enumPlayerInfoAction, packetPlayOutScoreboardTeam;

	private static Object addPlayer, removePlayer, updateLatency, updateDisplayName, updateGameMode, enumScoreboardHealthDisplayInteger, enumScoreboardActionChange,
			enumScoreboardActionRemove, iScoreboardCriteriaDummy, gameModeSpectator, gameModeSurvival, nameTagVisibilityAlways, nameTagVisibilityNever;

	private static Method scoreboardTeamSetNameTagVisibility, scoreboardTeamSetDisplayName, packetScoreboardTeamRemove, packetScoreboardTeamUpdateCreate,
			playerNameSetMethod, nameTagVisibilityByNameMethod, iChatBaseComponentGetStringMethod, enumChatFormatByIntMethod,
			scoreboardTeamSetPrefix, scoreboardTeamSetSuffix, scoreboardTeamSetChatFormat, parametersNameTagVisibility, parametersTeamPrefix, parametersTeamSuffix,
			packetScoreboardTeamParametersMethod, scoreboardTeamName, scoreboardTeamColor;

	private static Constructor<?> playerInfoDataConstr, playOutPlayerInfoConstructor, scoreboardConstructor, scoreboardTeamConstructor, packetPlayOutScoreboardTeamConstructor,
			packetPlayOutScoreboardScoreConstructor, packetPlayOutScoreboardObjectiveConstructor, firstScoreboardObjectiveConstructor,
			packetPlayOutScoreboardDisplayObjectiveConstructor;

	private static boolean isTeamOptionStatusEnumExist = false;

	static {
		try {
			iChatBaseComponent = classByName("net.minecraft.network.chat", "IChatBaseComponent");
			packet = classByName("net.minecraft.network.protocol", "Packet");
			packetPlayOutScoreboardTeam = classByName("net.minecraft.network.protocol.game", "PacketPlayOutScoreboardTeam");
			Class<?> packetPlayOutPlayerInfo = classByName("net.minecraft.network.protocol.game", "PacketPlayOutPlayerInfo");

			// Somehow the 1.8.8 server realizes that Team.OptionStatus enum class is existing
			//Class.forName("org.bukkit.scoreboard.Team$OptionStatus");
			try {
				org.bukkit.scoreboard.Team.class.getDeclaredMethod("getOption", org.bukkit.scoreboard.Team.Option.class);
				isTeamOptionStatusEnumExist = true;
			} catch (Throwable ignored) {
			}

			iChatBaseComponentGetStringMethod = methodByTypeAndName(iChatBaseComponent, String.class, null, "getString", "getText", "e");

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

			Class<?> enumChatFormat = classByName("net.minecraft", "EnumChatFormat");
			enumChatFormatByIntMethod = methodByTypeAndName(enumChatFormat, enumChatFormat, new Class<?>[] { String.class }, "getById", "a");

			addPlayer = fieldObjectByTypeOrName(enumPlayerInfoAction, null, null, "ADD_PLAYER", "a");
			updateLatency = fieldObjectByTypeOrName(enumPlayerInfoAction, null, null, "UPDATE_LATENCY", "c");
			updateDisplayName = fieldObjectByTypeOrName(enumPlayerInfoAction, null, null, "UPDATE_DISPLAY_NAME", "d");
			updateGameMode = fieldObjectByTypeOrName(enumPlayerInfoAction, null, null, "UPDATE_GAME_MODE", "b");
			removePlayer = fieldObjectByTypeOrName(enumPlayerInfoAction, null, null, "REMOVE_PLAYER", "e");

			packetScoreboardTeamParametersMethod = methodByTypeAndName(packetPlayOutScoreboardTeam, Optional.class, null, "f", "k", "parameters");

			Class<?> scoreboardClass = classByName("net.minecraft.world.scores", "Scoreboard");
			scoreboardConstructor = scoreboardClass.getConstructor();

			Class<?> scoreboardNameTagVisibility = classByName("net.minecraft.world.scores", "ScoreboardTeamBase$EnumNameTagVisibility");
			nameTagVisibilityByNameMethod = methodByTypeAndName(scoreboardNameTagVisibility, String.class, new Class<?>[] { String.class }, "a", "byName");

			nameTagVisibilityAlways = fieldObjectByTypeOrName(scoreboardNameTagVisibility, null, null, "a", "ALWAYS");
			nameTagVisibilityNever = fieldObjectByTypeOrName(scoreboardNameTagVisibility, null, null, "b", "NEVER");

			Class<?>[] classes = packetPlayOutScoreboardTeam.getDeclaredClasses();

			if (classes.length > 1) {
				Class<?> parameterType = classes[1];

				parametersNameTagVisibility = methodByTypeAndName(parameterType, scoreboardNameTagVisibility, null, "getNametagVisibility", "d");
				parametersTeamPrefix = methodByTypeAndName(parameterType, iChatBaseComponent, null, "getPlayerPrefix", "f");
				parametersTeamSuffix = methodByTypeAndName(parameterType, iChatBaseComponent, null, "getPlayerSuffix", "g");
			}

			if ((packetScoreboardTeamPrefix = fieldByTypeOrName(packetPlayOutScoreboardTeam, iChatBaseComponent, "c", "playerPrefix")) == null) {
				packetScoreboardTeamPrefix = fieldByTypeOrName(packetPlayOutScoreboardTeam, String.class, "c", "playerPrefix");
				packetScoreboardTeamSuffix = fieldByTypeOrName(packetPlayOutScoreboardTeam, String.class, "d", "playerSuffix");
			} else {
				packetScoreboardTeamSuffix = fieldByTypeOrName(packetPlayOutScoreboardTeam, iChatBaseComponent, "d", "playerSuffix");
			}

			Class<?> scoreboardTeamClass = classByName("net.minecraft.world.scores", "ScoreboardTeam");
			scoreboardTeamConstructor = scoreboardTeamClass.getConstructor(scoreboardClass, String.class);

			scoreboardTeamName = methodByTypeAndName(scoreboardTeamClass, String.class, null, "getName", "b");
			playerNameSetMethod = methodByTypeAndName(scoreboardTeamClass, Collection.class, null, "g", "getPlayers", "getPlayerNameSet");
			scoreboardTeamSetNameTagVisibility = methodByTypeAndName(scoreboardTeamClass, null, new Class<?>[] { scoreboardNameTagVisibility },
					"a", "setNameTagVisibility");

			scoreboardTeamSetPrefix = methodByTypeAndName(scoreboardTeamClass, null, new Class<?>[] { iChatBaseComponent }, "b", "setPlayerPrefix");
			if ((scoreboardTeamSetSuffix = methodByTypeAndName(scoreboardTeamClass, null, new Class<?>[] { iChatBaseComponent }, "c", "setPlayerSuffix")) == null) {
				scoreboardTeamSetPrefix = methodByTypeAndName(scoreboardTeamClass, null, new Class<?>[] { String.class }, "b", "setPrefix");
				scoreboardTeamSetSuffix = methodByTypeAndName(scoreboardTeamClass, null, new Class<?>[] { String.class }, "c", "setSuffix");
			}

			scoreboardTeamSetChatFormat = methodByTypeAndName(scoreboardTeamClass, null, new Class<?>[] { enumChatFormat }, "a", "m", "setColor");
			scoreboardTeamColor = methodByTypeAndName(scoreboardTeamClass, null, null, "getColor", "n");
			scoreboardTeamSetDisplayName = methodByTypeAndName(scoreboardTeamClass, null, new Class<?>[] { iChatBaseComponent },
					"a", "setDisplayName");

			try {
				packetPlayOutScoreboardTeamConstructor = packetPlayOutScoreboardTeam.getDeclaredConstructor();
			} catch (NoSuchMethodException e) {
				try {
					packetPlayOutScoreboardTeamConstructor = packetPlayOutScoreboardTeam.getDeclaredConstructor(classByName(null,
							"ScoreboardTeam"), int.class);
				} catch (NoSuchMethodException ex) {
					scoreboardTeamNames = fieldByTypeOrName(scoreboardTeamClass, java.util.Set.class, "e", "f", "players");
					packetScoreboardTeamRemove = packetPlayOutScoreboardTeam.getMethod("a", scoreboardTeamClass);
					packetScoreboardTeamUpdateCreate = packetPlayOutScoreboardTeam.getMethod("a", scoreboardTeamClass, boolean.class);
				}
			}

			if (packetPlayOutScoreboardTeamConstructor != null) {
				packetPlayOutScoreboardTeamConstructor.setAccessible(true);

				(packetScoreboardTeamName = packetPlayOutScoreboardTeam.getDeclaredField("a")).setAccessible(true);
				(scoreboardTeamDisplayName = packetPlayOutScoreboardTeam.getDeclaredField("b")).setAccessible(true);
				(nameTagVisibility = packetPlayOutScoreboardTeam.getDeclaredField("e")).setAccessible(true);
				packetScoreboardTeamMode = fieldByTypeOrName(packetPlayOutScoreboardTeam, int.class, "i", "h");
				scoreboardTeamNames = fieldByTypeOrName(packetPlayOutScoreboardTeam, Collection.class, "h", "e");
				(packetScoreboardTeamChatFormatColorField = packetPlayOutScoreboardTeam.getDeclaredField("g")).setAccessible(true);
			}

			// Objectives
			Class<?> scoreboardObjective = classByName("net.minecraft.world.scores", "ScoreboardObjective");

			packetPlayOutScoreboardDisplayObjectiveConstructor = classByName("net.minecraft.network.protocol.game", "PacketPlayOutScoreboardDisplayObjective")
					.getConstructor(int.class, scoreboardObjective);
			firstScoreboardObjectiveConstructor = scoreboardObjective.getConstructors()[0];
			packetPlayOutScoreboardObjectiveConstructor = classByName("net.minecraft.network.protocol.game", "PacketPlayOutScoreboardObjective")
					.getConstructor(scoreboardObjective, int.class);

			Class<?> enumScoreboardHealthDisplay;
			try {
				enumScoreboardHealthDisplay = classByName("net.minecraft.world.scores.criteria", "IScoreboardCriteria$EnumScoreboardHealthDisplay");
			} catch (ClassNotFoundException e) {
				enumScoreboardHealthDisplay = classByName("net.minecraft.world.scores.criteria", "EnumScoreboardHealthDisplay");
			}
			enumScoreboardHealthDisplayInteger = fieldObjectByTypeOrName(enumScoreboardHealthDisplay, null, null, "a", "INTEGER");

			Class<?> iScoreboardCriteria = classByName("net.minecraft.world.scores.criteria", "IScoreboardCriteria");
			iScoreboardCriteriaDummy = fieldObjectByTypeOrName(iScoreboardCriteria, null, iScoreboardCriteria);

			Class<?> enumScoreboardAction;

			try {
				enumScoreboardAction = classByName("net.minecraft.server", "ScoreboardServer$Action");
			} catch (ClassNotFoundException e) {
				try {
					enumScoreboardAction = classByName("net.minecraft.server", "PacketPlayOutScoreboardScore$EnumScoreboardAction");
				} catch (ClassNotFoundException ex) {
					enumScoreboardAction = classByName("net.minecraft.server", "EnumScoreboardAction");
				}
			}

			enumScoreboardActionChange = fieldObjectByTypeOrName(enumScoreboardAction, null, null, "a", "CHANGE");
			enumScoreboardActionRemove = fieldObjectByTypeOrName(enumScoreboardAction, null, null, "b", "REMOVE");

			try {
				packetPlayOutScoreboardScoreConstructor = classByName("net.minecraft.network.protocol.game", "PacketPlayOutScoreboardScore")
						.getConstructor(enumScoreboardAction, String.class, String.class, int.class);
			} catch (NoSuchMethodException excep) {
				packetPlayOutScoreboardScoreConstructor = classByName("net.minecraft.network.protocol.game", "PacketPlayOutScoreboardScore")
						.getConstructor(String.class);
			}

			(infoList = packetPlayOutPlayerInfo.getDeclaredField("b")).setAccessible(true);
			(actionField = packetPlayOutPlayerInfo.getDeclaredField("a")).setAccessible(true);

			Class<?> playerInfoData;
			try {
				playerInfoData = classByName("net.minecraft.network.protocol.game", "PacketPlayOutPlayerInfo$PlayerInfoData");
			} catch (ClassNotFoundException e) {
				playerInfoData = classByName(null, "PlayerInfoData");
			}

			for (Constructor<?> constr : playerInfoData.getConstructors()) {
				int paramCount = constr.getParameterCount();

				if (paramCount == 4 || paramCount == 5) {
					(playerInfoDataConstr = constr).setAccessible(true);
					break;
				}
			}

			for (Constructor<?> constructor : packetPlayOutPlayerInfo.getConstructors()) {
				if (constructor.getParameterCount() == 2 && constructor.getParameters()[1].getType().isArray()) {
					(playOutPlayerInfoConstructor = constructor).setAccessible(true);
					break;
				}
			}

			Class<?> enumGameMode;
			try {
				enumGameMode = classByName("net.minecraft.world.level", "EnumGamemode");
			} catch (ClassNotFoundException e) {
				enumGameMode = classByName(null, "WorldSettings$EnumGamemode");
			}

			gameModeSurvival = fieldObjectByTypeOrName(enumGameMode, null, null, "SURVIVAL", "a");
			gameModeSpectator = fieldObjectByTypeOrName(enumGameMode, null, null, "SPECTATOR", "d");

			playerInfoDataProfileField = fieldByTypeOrName(playerInfoData, GameProfile.class, "c", "d");
			playerInfoDataPing = fieldByTypeOrName(playerInfoData, int.class, "a", "b");
			playerInfoDataGameMode = fieldByTypeOrName(playerInfoData, enumGameMode, "b", "c");
			playerInfoDisplayName = fieldByTypeOrName(playerInfoData, iChatBaseComponent, "d", "e");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private ClazzContainer() {
	}

	public static Class<?> classByName(String newPackageName, String name) throws ClassNotFoundException {
		if (newPackageName == null) {
			return Class.forName("net.minecraft.server." + Util.legacyNmsVersion() + "." + name);
		}

		try {
			return Class.forName(newPackageName + "." + name);
		} catch (ClassNotFoundException ex) {
			return Class.forName("net.minecraft.server." + Util.legacyNmsVersion() + "." + name);
		}
	}

	public static Field fieldByTypeOrName(Class<?> from, Class<?> type, String... names) {
		Field[] fields = from.getDeclaredFields();

		if (type == null) {
			for (Field field : fields) {
				for (String name : names) {
					if (field.getName().equals(name)) {
						field.setAccessible(true);
						return field;
					}
				}
			}

			return null;
		}

		for (Field field : fields) {
			if (field.getType() != type) {
				continue;
			}

			if (names.length == 0) {
				field.setAccessible(true);
				return field;
			}

			for (String name : names) {
				if (field.getName().equals(name)) {
					field.setAccessible(true);
					return field;
				}
			}
		}

		return null;
	}

	private static Object fieldObjectByTypeOrName(Class<?> from, Object obj, Class<?> type, String... names) {
		Field field = fieldByTypeOrName(from, type, names);

		if (field != null) {
			try {
				return field.get(obj == null ? from : obj);
			} catch (IllegalAccessException ex) {
				ex.printStackTrace();
			}
		}

		return null;
	}

	public static Method methodByTypeAndName(Class<?> from, Class<?> returnType, Class<?>[] parameters, String... names) {
		Method[] methods = from.getDeclaredMethods();

		if (parameters != null) {
			for (Method method : methods) {
				if (java.util.Arrays.equals(method.getParameterTypes(), parameters)) {
					return method;
				}
			}
		}

		if (returnType == null) {
			for (Method method : methods) {
				for (String name : names) {
					if (method.getName().equals(name)) {
						return method;
					}
				}
			}

			return null;
		}

		for (Method method : methods) {
			if (method.getReturnType() != returnType) {
				continue;
			}

			if (names.length == 0) {
				return method;
			}

			for (String name : names) {
				if (method.getName().equals(name)) {
					return method;
				}
			}
		}

		return null;
	}

	public static Object newInstanceOfPacketPlayOutScoreboardScore(Object action, String objectiveName, String scoreName, int score) {
		try {
			if (packetPlayOutScoreboardScoreConstructor.getParameterCount() == 1) {
				return packetPlayOutScoreboardScoreConstructor.newInstance(objectiveName);
			}

			return packetPlayOutScoreboardScoreConstructor.newInstance(action, objectiveName, scoreName, score);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

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

	public static GameProfile getPlayerInfoDataProfile(Object infoData) {
		try {
			return (GameProfile) playerInfoDataProfileField.get(infoData);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}

		return null;
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

	public static Constructor<?> getPlayerInfoDataConstructor() {
		return playerInfoDataConstr;
	}

	public static Constructor<?> getPlayOutPlayerInfoConstructor() {
		return playOutPlayerInfoConstructor;
	}

	public static Object getAddPlayer() {
		return addPlayer;
	}

	public static Object getRemovePlayer() {
		return removePlayer;
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

	public static Method getScoreboardTeamSetNameTagVisibility() {
		return scoreboardTeamSetNameTagVisibility;
	}

	public static Method getScoreboardTeamSetDisplayName() {
		return scoreboardTeamSetDisplayName;
	}

	public static Field getPacketScoreboardTeamName() {
		return packetScoreboardTeamName;
	}

	public static Field getScoreboardTeamDisplayName() {
		return scoreboardTeamDisplayName;
	}

	public static Field getScoreboardTeamNames() {
		return scoreboardTeamNames;
	}

	public static Field getNameTagVisibility() {
		return nameTagVisibility;
	}

	public static Field getPlayerInfoDataGameMode() {
		return playerInfoDataGameMode;
	}

	public static Method getPlayerNameSetMethod() {
		return playerNameSetMethod;
	}

	public static Constructor<?> getPacketPlayOutScoreboardObjectiveConstructor() {
		return packetPlayOutScoreboardObjectiveConstructor;
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

	public static Object getEnumUpdateGameMode() {
		return updateGameMode;
	}

	public static Field getActionField() {
		return actionField;
	}

	public static Object getGameModeSpectator() {
		return gameModeSpectator;
	}

	public static Field getPlayerInfoDataPing() {
		return playerInfoDataPing;
	}

	public static Field getPlayerInfoDisplayName() {
		return playerInfoDisplayName;
	}

	public static Object getGameModeSurvival() {
		return gameModeSurvival;
	}

	public static Class<?> packetPlayOutScoreboardTeam() {
		return packetPlayOutScoreboardTeam;
	}

	public static Method getNameTagVisibilityByNameMethod() {
		return nameTagVisibilityByNameMethod;
	}

	public static Object getNameTagVisibilityAlways() {
		return nameTagVisibilityAlways;
	}

	public static Object getNameTagVisibilityNever() {
		return nameTagVisibilityNever;
	}

	public static Field getPacketScoreboardTeamPrefix() {
		return packetScoreboardTeamPrefix;
	}

	public static Field getPacketScoreboardTeamSuffix() {
		return packetScoreboardTeamSuffix;
	}

	public static Method getiChatBaseComponentGetStringMethod() {
		return iChatBaseComponentGetStringMethod;
	}

	public static Method getEnumChatFormatByIntMethod() {
		return enumChatFormatByIntMethod;
	}

	public static Field getPacketScoreboardTeamChatFormatColorField() {
		return packetScoreboardTeamChatFormatColorField;
	}

	public static Method getScoreboardTeamSetPrefix() {
		return scoreboardTeamSetPrefix;
	}

	public static Method getScoreboardTeamSetSuffix() {
		return scoreboardTeamSetSuffix;
	}

	public static Method getScoreboardTeamSetChatFormat() {
		return scoreboardTeamSetChatFormat;
	}

	public static Method getParametersNameTagVisibility() {
		return parametersNameTagVisibility;
	}

	public static Method getParametersTeamPrefix() {
		return parametersTeamPrefix;
	}

	public static Method getParametersTeamSuffix() {
		return parametersTeamSuffix;
	}

	public static Method getPacketScoreboardTeamParametersMethod() {
		return packetScoreboardTeamParametersMethod;
	}

	public static Field getPacketScoreboardTeamMode() {
		return packetScoreboardTeamMode;
	}

	public static Method getScoreboardTeamName() {
		return scoreboardTeamName;
	}

	public static Method getScoreboardTeamColor() {
		return scoreboardTeamColor;
	}
}
