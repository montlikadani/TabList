package hu.montlikadani.tablist.bukkit.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import hu.montlikadani.tablist.bukkit.utils.ServerVersion.Version;

public class ReflectionUtils {

	public static Object getHandle(Object obj) throws Exception {
		Method method = obj.getClass().getDeclaredMethod("getHandle");
		if (!method.isAccessible()) {
			method.setAccessible(true);
		}

		return method.invoke(obj);
	}

	public static Object getAsIChatBaseComponent(String name) throws Exception {
		Class<?> iChatBaseComponent = getNMSClass("IChatBaseComponent");
		if (Version.isCurrentLower(Version.v1_8_R2)) {
			Class<?> chatSerializer = getNMSClass("ChatSerializer");
			Method m = chatSerializer.getMethod("a", String.class);
			Object t = iChatBaseComponent.cast(m.invoke(chatSerializer, "{\"text\":\"" + name + "\"}"));
			return t;
		}

		Method m = iChatBaseComponent.getDeclaredClasses()[0].getMethod("a", String.class);
		return m.invoke(iChatBaseComponent, "{\"text\":\"" + name + "\"}");
	}

	public static Object invokeMethod(Object obj, String name) throws Exception {
		return invokeMethod(obj, name, true);
	}

	public static Object invokeMethod(Object obj, String name, boolean declared) throws Exception {
		Method met = declared ? obj.getClass().getDeclaredMethod(name) : obj.getClass().getMethod(name);
		return met.invoke(obj);
	}

	public static Object getNMSPlayer(Player p) throws Exception {
		return getHandle(p);
	}

	public static Class<?> getNMSClass(String name) throws ClassNotFoundException {
		return Class.forName("net.minecraft.server." + getPackageVersion() + "." + name);
	}

	public static Class<?> getCraftClass(String className) throws ClassNotFoundException {
		return Class.forName("org.bukkit.craftbukkit." + getPackageVersion() + "." + className);
	}

	public static Field getField(Object clazz, String name) throws Exception {
		return getField(clazz, name, true);
	}

	public static Field getField(Object clazz, String name, boolean declared) throws Exception {
		return getField(clazz.getClass(), name, declared);
	}

	public static Field getField(Class<?> clazz, String name) throws Exception {
		return getField(clazz, name, true);
	}

	public static Field getField(Class<?> clazz, String name, boolean declared) throws Exception {
		Field field = declared ? clazz.getDeclaredField(name) : clazz.getField(name);
		if (!field.isAccessible()) {
			field.setAccessible(true);
		}

		return field;
	}

	public static void modifyFinalField(Field field, Object target, Object newValue) throws Exception {
		field.setAccessible(true);
		getField(Field.class, "modifiers").setInt(field, field.getModifiers() & ~Modifier.FINAL);
		field.set(target, newValue);
	}

	public static Object getFieldObject(Object object, Field field) throws Exception {
		return field.get(object);
	}

	public static void setField(Object object, String fieldName, Object fieldValue) throws Exception {
		getField(object, fieldName).set(object, fieldValue);
	}

	private static int a = 0;

	public static void sendPacket(Player player, Object packet) throws Exception {
		Object playerHandle = getNMSPlayer(player);
		Object playerConnection = getFieldObject(playerHandle, getField(playerHandle, "playerConnection"));
		try {
			playerConnection.getClass().getDeclaredMethod("sendPacket", getNMSClass("Packet")).invoke(playerConnection,
					packet);
		} catch (Exception e) {
			if (a < 8) {
				Util.logConsole(Level.WARNING,
						"You're using a plugin which overwrites sending packets. Remove that plugin.");
				a++;
			}
		}
	}

	public static String getPackageVersion() {
		return Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
	}

	public static class Classes {

		public static Class<?> getMinecraftServer() {
			Class<?> server = null;

			try {
				server = getNMSClass("MinecraftServer");
			} catch (ClassNotFoundException c) {
				try {
					server = getNMSClass("DedicatedServer");
				} catch (ClassNotFoundException e) {
				}
			}

			return server;
		}

		public static Object getServer(Class<?> server) {
			Object serverIns = null;

			try {
				serverIns = server.getMethod("getServer")
						.invoke(ReflectionUtils.getCraftClass("CraftServer").cast(Bukkit.getServer()));
			} catch (Exception x) {
				try {
					serverIns = server.getMethod("getServer").invoke(server);
				} catch (Exception e) {
				}
			}

			return serverIns;
		}

		public static Class<?> getEnumPlayerInfoAction() {
			Class<?> enumPlayerInfoAction = null;

			try {
				if (Version.isCurrentEqual(Version.v1_8_R1)) {
					enumPlayerInfoAction = getNMSClass("EnumPlayerInfoAction");
				} else if (Version.isCurrentEqualOrHigher(Version.v1_11_R1)) {
					enumPlayerInfoAction = getNMSClass("PacketPlayOutPlayerInfo").getDeclaredClasses()[1];
				}

				if (enumPlayerInfoAction == null) {
					enumPlayerInfoAction = getNMSClass("PacketPlayOutPlayerInfo").getDeclaredClasses()[2];
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			return enumPlayerInfoAction;
		}
	}
}