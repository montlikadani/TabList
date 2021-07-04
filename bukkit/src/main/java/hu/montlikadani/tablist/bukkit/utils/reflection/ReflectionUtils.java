package hu.montlikadani.tablist.bukkit.utils.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.bukkit.entity.Player;

import hu.montlikadani.tablist.bukkit.utils.ServerVersion;

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

	public static Object getHandle(Object obj) throws Exception {
		return invokeMethod(obj, "getHandle");
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

	public static Object invokeMethod(Object obj, String name) throws Exception {
		return invokeMethod(obj, name, true, false);
	}

	public static Object invokeMethod(Object obj, String name, boolean superClass) throws Exception {
		return invokeMethod(obj, name, true, superClass);
	}

	public static Object invokeMethod(Object obj, String name, boolean declared, boolean superClass) throws Exception {
		Class<?> target = superClass ? obj.getClass().getSuperclass() : obj.getClass();
		Method met = declared ? target.getDeclaredMethod(name) : target.getMethod(name);

		met.setAccessible(true);
		return met.invoke(obj);
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

	public static Field getField(Class<?> clazz, String name) throws Exception {
		return getField(clazz, name, true);
	}

	public static Field getField(Class<?> clazz, String name, boolean declared) throws Exception {
		Field field = declared ? clazz.getDeclaredField(name) : clazz.getField(name);
		field.setAccessible(true);
		return field;
	}

	public static void setField(Object object, String fieldName, Object fieldValue) throws Exception {
		getField(object.getClass(), fieldName).set(object, fieldValue);
	}

	public static void sendPacket(Player player, Object packet) {
		if (player == null || packet == null) {
			return;
		}

		try {
			if (playerHandleMethod == null) {
				playerHandleMethod = player.getClass().getDeclaredMethod("getHandle");
			}

			Object playerHandle = playerHandleMethod.invoke(player);

			if (playerConnectionField == null) {
				playerConnectionField = playerHandle.getClass().getDeclaredField(
						(ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_17_R1) ? "b" : "playerConnection"));
			}

			Object playerConnection = playerConnectionField.get(playerHandle);

			if (sendPacketMethod == null) {
				sendPacketMethod = playerConnection.getClass().getDeclaredMethod("sendPacket",
						ClazzContainer.getPacket());
			}

			sendPacketMethod.invoke(playerConnection, packet);
		} catch (Exception e) {
		}
	}

	public static class Classes {

		public static Object getNewEntityPlayer(Object profile) {
			Object serverIns = getServer(ClazzContainer.getMinecraftServer());

			try {
				// Only get the first world
				Object worldServer = getHandle(org.bukkit.Bukkit.getServer().getWorlds().get(0));

				if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_17_R1)) {
					return ClazzContainer
							.getEntityPlayerClass().getConstructor(ClazzContainer.getMinecraftServer(),
									worldServer.getClass(), profile.getClass())
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
						.getEntityPlayerClass().getConstructor(ClazzContainer.getMinecraftServer(),
								worldServer.getClass(), profile.getClass(), interactManager)
						.newInstance(serverIns, worldServer, profile, managerIns);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return null;
		}

		private static Object getServer(Class<?> server) {
			try {
				return server.getMethod("getServer")
						.invoke(ReflectionUtils.getCraftClass("CraftServer").cast(org.bukkit.Bukkit.getServer()));
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
}