package hu.montlikadani.tablist.bukkit.utils.reflection;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

public class NMSContainer {

	private static Field infoList;
	private static Constructor<?> playerInfoDataConstr, playOutPlayerInfoConstructor;
	private static Class<?> packet, packetPlayOutPlayerInfo, enumPlayerInfoAction, entityPlayerClass, enumGameMode,
			playerInfoData, minecraftServer;
	private static Object gameModeNotSet, gameModeSpectator, gameModeSurvival, addPlayer, removePlayer, updateGameMode,
			updateLatency, updateDisplayName;
	private static Constructor<?>[] playerInfoDataConstructors;

	static {
		try {
			packet = ReflectionUtils.getNMSClass("Packet");
			packetPlayOutPlayerInfo = ReflectionUtils.getNMSClass("PacketPlayOutPlayerInfo");
			entityPlayerClass = ReflectionUtils.getNMSClass("EntityPlayer");
			try {
				minecraftServer = ReflectionUtils.getNMSClass("MinecraftServer");
			} catch (ClassNotFoundException c) {
				try {
					minecraftServer = ReflectionUtils.getNMSClass("DedicatedServer");
				} catch (ClassNotFoundException e) {
				}
			}

			try {
				enumPlayerInfoAction = ReflectionUtils.getNMSClass("EnumPlayerInfoAction");
			} catch (ClassNotFoundException c) {
				for (Class<?> clazz : packetPlayOutPlayerInfo.getDeclaredClasses()) {
					if (clazz.getName().contains("EnumPlayerInfoAction")) {
						enumPlayerInfoAction = clazz;
						break;
					}
				}
			}

			addPlayer = enumPlayerInfoAction.getDeclaredField("ADD_PLAYER").get(enumPlayerInfoAction);
			removePlayer = enumPlayerInfoAction.getDeclaredField("REMOVE_PLAYER").get(enumPlayerInfoAction);
			updateGameMode = enumPlayerInfoAction.getDeclaredField("UPDATE_GAME_MODE").get(enumPlayerInfoAction);
			updateLatency = enumPlayerInfoAction.getDeclaredField("UPDATE_LATENCY").get(enumPlayerInfoAction);
			updateDisplayName = enumPlayerInfoAction.getDeclaredField("UPDATE_DISPLAY_NAME").get(enumPlayerInfoAction);

			infoList = ReflectionUtils.getField(packetPlayOutPlayerInfo, "b");

			try {
				playerInfoData = ReflectionUtils.getNMSClass("PacketPlayOutPlayerInfo$PlayerInfoData");
			} catch (ClassNotFoundException e) {
				playerInfoData = ReflectionUtils.getNMSClass("PlayerInfoData");
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

			(playOutPlayerInfoConstructor = packetPlayOutPlayerInfo.getDeclaredConstructor(enumPlayerInfoAction,
					Array.newInstance(entityPlayerClass, 0).getClass())).setAccessible(true);

			try {
				enumGameMode = ReflectionUtils.getNMSClass("EnumGamemode");
			} catch (ClassNotFoundException e) {
				enumGameMode = ReflectionUtils.getNMSClass("WorldSettings$EnumGamemode");
			}

			gameModeNotSet = enumGameMode.getDeclaredField("NOT_SET").get(enumGameMode);
			gameModeSpectator = enumGameMode.getDeclaredField("SPECTATOR").get(enumGameMode);
			gameModeSurvival = enumGameMode.getDeclaredField("SURVIVAL").get(enumGameMode);
		} catch (Exception e) {
			e.printStackTrace();
		}
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
}