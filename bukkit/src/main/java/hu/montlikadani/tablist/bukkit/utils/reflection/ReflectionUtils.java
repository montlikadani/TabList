package hu.montlikadani.tablist.bukkit.utils.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import hu.montlikadani.tablist.bukkit.utils.ServerVersion;

public final class ReflectionUtils {

	private static Field modifiersField;
	private static JsonComponent jsonComponent;

	static {
		try {
			modifiersField = Field.class.getDeclaredField("modifiers");
		} catch (NoSuchFieldException e) { // Java 12+
			try {
				Method meth;
				if (JavaAccessibilities.getCurrentVersion() >= 15) {
					meth = Class.class.getDeclaredMethod("getDeclaredFieldsImpl");
				} else {
					meth = Class.class.getDeclaredMethod("getDeclaredFields0", boolean.class);
				}

				boolean accessibleBeforeSet = JavaAccessibilities.isAccessible(meth, null);
				meth.setAccessible(true);

				Field[] fields = (Field[]) (JavaAccessibilities.getCurrentVersion() >= 15 ? meth.invoke(Field.class)
						: meth.invoke(Field.class, false));
				for (Field f : fields) {
					if ("modifiers".equals(f.getName())) {
						modifiersField = f;
						break;
					}
				}

				meth.setAccessible(accessibleBeforeSet);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
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
		Class<?> iChatBaseComponent = getNMSClass("IChatBaseComponent");

		if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_16_R1)) {
			return getJsonComponent().parseProperty(text, iChatBaseComponent);
		}

		if (ServerVersion.isCurrentLower(ServerVersion.v1_8_R2)) {
			Class<?> chatSerializer = getNMSClass("ChatSerializer");
			return iChatBaseComponent.cast(
					chatSerializer.getMethod("a", String.class).invoke(chatSerializer, "{\"text\":\"" + text + "\"}"));
		}

		Method m = iChatBaseComponent.getDeclaredClasses()[0].getMethod("a", String.class);
		return m.invoke(iChatBaseComponent, "{\"text\":\"" + text + "\"}");
	}

	public static Object invokeMethod(Object obj, String name) throws Exception {
		return invokeMethod(obj, name, true, false);
	}

	public static Object invokeMethod(Object obj, String name, boolean superClass) throws Exception {
		return invokeMethod(obj, name, true, superClass);
	}

	public static Object invokeMethod(Object obj, String name, boolean declared, boolean superClass) throws Exception {
		Class<?> c = superClass ? obj.getClass().getSuperclass() : obj.getClass();
		Method met = declared ? c.getDeclaredMethod(name) : c.getMethod(name);

		if (!JavaAccessibilities.isAccessible(met, obj)) {
			met.setAccessible(true);
		}

		return met.invoke(obj);
	}

	public static Class<?> getNMSClass(String name) throws ClassNotFoundException {
		return Class.forName("net.minecraft.server." + ServerVersion.getArrayVersion()[3] + "." + name);
	}

	public static Class<?> getCraftClass(String className) throws ClassNotFoundException {
		return Class.forName("org.bukkit.craftbukkit." + ServerVersion.getArrayVersion()[3] + "." + className);
	}

	public static Field getField(Object clazz, String name) throws Exception {
		return getField(clazz.getClass(), name, true);
	}

	public static Field getField(Class<?> clazz, String name) throws Exception {
		return getField(clazz, name, true);
	}

	public static Field getField(Class<?> clazz, String name, boolean declared) throws Exception {
		Field field = declared ? clazz.getDeclaredField(name) : clazz.getField(name);

		if (!JavaAccessibilities.isAccessible(field, null)) {
			field.setAccessible(true);
		}

		return field;
	}

	public static void modifyFinalField(Field field, Object target, Object newValue) throws Exception {
		if (modifiersField == null) {
			return;
		}

		field.setAccessible(true);

		if (JavaAccessibilities.getCurrentVersion() < 13) {
			modifiersField.setAccessible(true);
			modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
			field.set(target, newValue);
		} else {
			boolean accessibleBeforeSet = JavaAccessibilities.isAccessible(modifiersField, null);
			modifiersField.setAccessible(true);
			modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
			field.set(target, newValue);
			modifiersField.setAccessible(accessibleBeforeSet);
		}
	}

	public static void setField(Object object, String fieldName, Object fieldValue) throws Exception {
		getField(object, fieldName).set(object, fieldValue);
	}

	public static void sendPacket(Player player, Object packet) {
		try {
			Object playerHandle = getHandle(player);
			Object playerConnection = getField(playerHandle, "playerConnection").get(playerHandle);
			playerConnection.getClass().getDeclaredMethod("sendPacket", getNMSClass("Packet")).invoke(playerConnection,
					packet);
		} catch (Exception e) {
		}
	}

	public static class Classes {

		public static Object getPlayerConstructor(Player player, Object profile) {
			Class<?> server = getMinecraftServer();
			Object serverIns = getServer(server);

			try {
				Class<?> manager = getNMSClass("PlayerInteractManager");
				Object managerIns = null, world = null;
				if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_14_R1)) {
					world = getHandle(player.getWorld());
					managerIns = manager.getConstructor(world.getClass()).newInstance(world);
				} else if (ServerVersion.isCurrentEqual(ServerVersion.v1_13_R1)
						|| ServerVersion.isCurrentEqual(ServerVersion.v1_13_R2)) {
					world = getHandle(player.getWorld());
				} else {
					world = server.getDeclaredMethod("getWorldServer", int.class).invoke(serverIns, 0);
				}

				if (managerIns == null) {
					managerIns = manager.getConstructors()[0].newInstance(world);
				}

				return getHandle(player).getClass()
						.getConstructor(server, world.getClass(), profile.getClass(), manager)
						.newInstance(serverIns, world, profile, managerIns);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return null;
		}

		public static Class<?> getMinecraftServer() {
			try {
				return getNMSClass("MinecraftServer");
			} catch (ClassNotFoundException c) {
				try {
					return getNMSClass("DedicatedServer");
				} catch (ClassNotFoundException e) {
				}
			}

			return null;
		}

		public static Object getServer(Class<?> server) {
			try {
				return server.getMethod("getServer")
						.invoke(ReflectionUtils.getCraftClass("CraftServer").cast(Bukkit.getServer()));
			} catch (ReflectiveOperationException x) {
				try {
					return server.getMethod("getServer").invoke(server);
				} catch (ReflectiveOperationException e) {
				}
			}

			return null;
		}

		public static Class<?> getEnumPlayerInfoAction(Class<?> packetPlayOutPlayerInfo) {
			try {
				if (ServerVersion.isCurrentEqual(ServerVersion.v1_8_R1)) {
					return getNMSClass("EnumPlayerInfoAction");
				} else if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_11_R1)) {
					return packetPlayOutPlayerInfo.getDeclaredClasses()[1];
				}

				return packetPlayOutPlayerInfo.getDeclaredClasses()[2];
			} catch (ReflectiveOperationException e) {
				e.printStackTrace();
			}

			return null;
		}
	}

	public abstract static class JavaAccessibilities {

		public static boolean isAccessible(Field field, Object target) {
			if (getCurrentVersion() >= 9 && target != null) {
				try {
					return (boolean) field.getClass().getDeclaredMethod("canAccess", Object.class).invoke(field,
							target);
				} catch (NoSuchMethodException e) {
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			return field.isAccessible();
		}

		public static boolean isAccessible(Method method, Object target) {
			if (getCurrentVersion() >= 9 && target != null) {
				try {
					return (boolean) method.getClass().getDeclaredMethod("canAccess", Object.class).invoke(method,
							target);
				} catch (NoSuchMethodException e) {
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			return method.isAccessible();
		}

		private static int jVersion = 0;

		public static int getCurrentVersion() {
			if (jVersion != 0) {
				return jVersion;
			}

			String currentVersion = System.getProperty("java.version");
			if (currentVersion.contains("_")) {
				currentVersion = currentVersion.split("_")[0];
			}

			currentVersion = currentVersion.replaceAll("[^\\d]|_", "");

			for (int i = 8; i <= 18; i++) {
				if (currentVersion.contains(Integer.toString(i))) {
					return jVersion = i;
				}
			}

			return 0;
		}
	}
}