package hu.montlikadani.tablist.bukkit.utils.reflection;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

public class NMSContainer {

	private static Field infoList;
	private static Constructor<?> playerInfoDataConstr, playOutPlayerInfoConstror;
	private static Class<?> packet, packetPlayOutPlayerInfo, enumPlayerInfoAction, entityPlayerClass, enumGameMode,
			playerInfoData;
	private static Object gameMode, addPlayer, removePlayer, updateGameMode, updateLatency, updateDisplayName;
	private static Constructor<?>[] playerInfoDataConstructors;

	static {
		try {
			packet = ReflectionUtils.getNMSClass("Packet");
			packetPlayOutPlayerInfo = ReflectionUtils.getNMSClass("PacketPlayOutPlayerInfo");
			entityPlayerClass = ReflectionUtils.getNMSClass("EntityPlayer");
			enumPlayerInfoAction = ReflectionUtils.Classes.getEnumPlayerInfoAction(packetPlayOutPlayerInfo);

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

			(playOutPlayerInfoConstror = packetPlayOutPlayerInfo.getDeclaredConstructor(enumPlayerInfoAction,
					Array.newInstance(entityPlayerClass, 0).getClass())).setAccessible(true);

			try {
				enumGameMode = ReflectionUtils.getNMSClass("EnumGamemode");
			} catch (ClassNotFoundException e) {
				enumGameMode = ReflectionUtils.getNMSClass("WorldSettings$EnumGamemode");
			}

			gameMode = enumGameMode.getDeclaredField("NOT_SET").get(enumGameMode);
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
		return playOutPlayerInfoConstror;
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

	public static Object getGameMode() {
		return gameMode;
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
}
