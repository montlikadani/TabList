package hu.montlikadani.tablist.bukkit.utils.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.bukkit.entity.Player;

import hu.montlikadani.tablist.bukkit.utils.ServerVersion;

public final class ReflectionUtils {

	private static JsonComponent jsonComponent;

	public static Method jsonComponentMethod;

	static {
		try {
			Class<?>[] declaredClasses = NMSContainer.getIChatBaseComponent().getDeclaredClasses();

			if (declaredClasses.length > 0) {
				jsonComponentMethod = declaredClasses[0].getMethod("a", String.class);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private ReflectionUtils() {
	}

	private static int jVersion = 0;

	public static int getJavaVersion() {
		if (jVersion != 0) {
			return jVersion;
		}

		String currentVersion = System.getProperty("java.version");
		String[] split = currentVersion.split("_", 2);

		if (split.length > 0) {
			currentVersion = split[0];
		}

		currentVersion = currentVersion.replaceAll("[^\\d]|_", "");

		for (int i = 8; i <= 18; i++) {
			if (currentVersion.contains(Integer.toString(i))) {
				return jVersion = i;
			}
		}

		return 0;
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
			return getJsonComponent().parseProperty(text, NMSContainer.getIChatBaseComponent());
		}

		if (ServerVersion.isCurrentLower(ServerVersion.v1_8_R2)) {
			Class<?> chatSerializer = getNMSClass("ChatSerializer");
			return NMSContainer.getIChatBaseComponent().cast(
					chatSerializer.getMethod("a", String.class).invoke(chatSerializer, "{\"text\":\"" + text + "\"}"));
		}

		return jsonComponentMethod.invoke(NMSContainer.getIChatBaseComponent(), "{\"text\":\"" + text + "\"}");
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

	public static Class<?> getNMSClass(String name) throws ClassNotFoundException {
		return Class.forName("net.minecraft.server." + ServerVersion.getArrayVersion()[3] + "." + name);
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
		if (player == null) {
			return;
		}

		try {
			Object playerHandle = getHandle(player);
			Object playerConnection = getField(playerHandle.getClass(), "playerConnection").get(playerHandle);

			playerConnection.getClass().getDeclaredMethod("sendPacket", NMSContainer.getPacket())
					.invoke(playerConnection, packet);
		} catch (Exception e) {
		}
	}

	public static class Classes {

		public static Object getNewEntityPlayer(Object profile) {
			Object serverIns = getServer(NMSContainer.getMinecraftServer());

			try {
				Class<?> interactManager = getNMSClass("PlayerInteractManager");
				Object managerIns = null;

				// Only get the first world
				Object worldServer = getHandle(org.bukkit.Bukkit.getServer().getWorlds().get(0));

				if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_14_R1)) {
					managerIns = interactManager.getConstructor(worldServer.getClass()).newInstance(worldServer);
				}

				if (managerIns == null) {
					managerIns = interactManager.getConstructors()[0].newInstance(worldServer);
				}

				return NMSContainer
						.getEntityPlayerClass().getConstructor(NMSContainer.getMinecraftServer(),
								worldServer.getClass(), profile.getClass(), interactManager)
						.newInstance(serverIns, worldServer, profile, managerIns);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return null;
		}

		public static Object getServer(Class<?> server) {
			try {
				return server.getMethod("getServer")
						.invoke(ReflectionUtils.getCraftClass("CraftServer").cast(org.bukkit.Bukkit.getServer()));
			} catch (Exception x) {
				try {
					return server.getMethod("getServer").invoke(server);
				} catch (ReflectiveOperationException e) {
				}
			}

			return null;
		}
	}
}