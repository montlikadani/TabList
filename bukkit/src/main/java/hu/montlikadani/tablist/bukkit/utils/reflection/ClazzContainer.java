package hu.montlikadani.tablist.bukkit.utils.reflection;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import hu.montlikadani.tablist.bukkit.utils.ServerVersion;

public final class ClazzContainer {

	private static Field infoList;

	private static Constructor<?> playerInfoDataConstr, playOutPlayerInfoConstructor;

	private static Class<?> iChatBaseComponent, packet, packetPlayOutPlayerInfo, enumPlayerInfoAction,
			entityPlayerClass, enumGameMode, playerInfoData, minecraftServer;

	private static Object gameModeNotSet, gameModeSpectator, gameModeSurvival, addPlayer, removePlayer, updateGameMode,
			updateLatency, updateDisplayName;

	private static Constructor<?>[] playerInfoDataConstructors;

	static {
		try {
			iChatBaseComponent = classByName("net.minecraft.network.chat", "IChatBaseComponent");
			packet = classByName("net.minecraft.network.protocol", "Packet");
			packetPlayOutPlayerInfo = classByName("net.minecraft.network.protocol.game", "PacketPlayOutPlayerInfo");
			entityPlayerClass = classByName("net.minecraft.server.level", "EntityPlayer");

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

			infoList = ReflectionUtils.getField(packetPlayOutPlayerInfo, "b");

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

	public static Class<?> getPacket() {
		return packet;
	}

	public static Field getInfoList() {
		return infoList;
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
}
