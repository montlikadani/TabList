package hu.montlikadani.tablist.bukkit.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.logging.Level;

import org.bukkit.entity.Player;

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
		if (ServerVersion.Version.isCurrentLower(ServerVersion.Version.v1_8_R2)) {
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
		} catch (IllegalAccessException il) {
			if (a < 8) {
				Util.logConsole(Level.WARNING,
						"You're using a plugin which overwrites sending packets. Remove that plugin.");
				a++;
			}
		}
	}

	public static String getPackageVersion() {
		return org.bukkit.Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
	}
}