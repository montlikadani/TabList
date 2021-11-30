package hu.montlikadani.tablist.utils.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.bukkit.entity.Player;

import hu.montlikadani.tablist.utils.ServerVersion;

public final class ReflectionUtils {

	public static Method jsonComponentMethod;

	private static JsonComponent jsonComponent;
	private static Method playerHandleMethod, sendPacketMethod;
	private static Field playerConnectionField;

	static {
		try {
			Class<?>[] declaredClasses = ClazzContainer.getIChatBaseComponent().getDeclaredClasses();

			if (declaredClasses.length > 0) {
				jsonComponentMethod = declaredClasses[0].getMethod("a", String.class);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private ReflectionUtils() {
	}

	public static JsonComponent getJsonComponent() {
		if (jsonComponent == null) {
			jsonComponent = new JsonComponent();
		}

		return jsonComponent;
	}

	public static Object getPlayerHandle(Player player) throws Exception {
		if (playerHandleMethod == null) {
			playerHandleMethod = player.getClass().getDeclaredMethod("getHandle");
		}

		return playerHandleMethod.invoke(player);
	}

	public static Object getAsIChatBaseComponent(final String text) throws Exception {
		if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_16_R1)) {
			return getJsonComponent().parseProperty(text);
		}

		if (ServerVersion.isCurrentLower(ServerVersion.v1_8_R2)) {
			Class<?> chatSerializer = getPacketClass(null, "ChatSerializer");
			return ClazzContainer.getIChatBaseComponent().cast(
					chatSerializer.getMethod("a", String.class).invoke(chatSerializer, "{\"text\":\"" + text + "\"}"));
		}

		return jsonComponentMethod.invoke(ClazzContainer.getIChatBaseComponent(), "{\"text\":\"" + text + "\"}");
	}

	public static Class<?> getPacketClass(String newPackageName, String name) throws ClassNotFoundException {
		if (ServerVersion.isCurrentLower(ServerVersion.v1_17_R1) || newPackageName == null) {
			newPackageName = "net.minecraft.server." + ServerVersion.getArrayVersion()[3];
		}

		return Class.forName(newPackageName + "." + name);
	}

	public static Class<?> getCraftClass(String className) throws ClassNotFoundException {
		return Class.forName("org.bukkit.craftbukkit." + ServerVersion.getArrayVersion()[3] + "." + className);
	}

	public static void sendPacket(Player player, Object packet) {
		if (player == null) {
			return;
		}

		try {
			Object playerHandle = getPlayerHandle(player);

			if (playerConnectionField == null) {
				playerConnectionField = playerHandle.getClass().getDeclaredField(
						(ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_17_R1) ? "b" : "playerConnection"));
			}

			Object playerConnection = playerConnectionField.get(playerHandle);

			if (sendPacketMethod == null) {
				sendPacketMethod = playerConnection.getClass().getDeclaredMethod(
						ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_18_R1) ? "a" : "sendPacket",
						ClazzContainer.getPacket());
			}

			sendPacketMethod.invoke(playerConnection, packet);
		} catch (Exception e) {
		}
	}

	public static Object getNewEntityPlayer(Object profile) {
		Object serverIns = getServer(ClazzContainer.getMinecraftServer());

		try {
			// Only get the first world
			org.bukkit.World world = org.bukkit.Bukkit.getServer().getWorlds().get(0);
			Object worldServer = world.getClass().getDeclaredMethod("getHandle").invoke(world);

			if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_17_R1)) {
				return ClazzContainer.getEntityPlayerClass()
						.getConstructor(ClazzContainer.getMinecraftServer(), worldServer.getClass(), profile.getClass())
						.newInstance(serverIns, worldServer, profile);
			}

			Class<?> interactManager = getPacketClass("net.minecraft.server.level", "PlayerInteractManager");
			Object managerIns = null;

			if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_14_R1)) {
				managerIns = interactManager.getConstructor(worldServer.getClass()).newInstance(worldServer);
			}

			if (managerIns == null) {
				managerIns = interactManager.getConstructors()[0].newInstance(worldServer);
			}

			return ClazzContainer
					.getEntityPlayerClass().getConstructor(ClazzContainer.getMinecraftServer(), worldServer.getClass(),
							profile.getClass(), interactManager)
					.newInstance(serverIns, worldServer, profile, managerIns);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	private static Object getServer(Class<?> server) {
		try {
			return server.getMethod("getServer")
					.invoke(getCraftClass("CraftServer").cast(org.bukkit.Bukkit.getServer()));
		} catch (Exception x) {
			try {
				return server.getMethod("getServer").invoke(server);
			} catch (ReflectiveOperationException e) {
				e.printStackTrace();
			}
		}

		return null;
	}
}