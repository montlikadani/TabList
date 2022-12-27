package hu.montlikadani.tablist.utils.reflection;

import hu.montlikadani.tablist.packets.PacketNM;
import hu.montlikadani.tablist.tablist.TabText;
import hu.montlikadani.tablist.utils.ServerVersion;

import java.lang.reflect.Method;
import java.util.concurrent.locks.ReentrantLock;

public final class ReflectionUtils {

	public static final Object EMPTY_COMPONENT;

	private static JsonComponent jsonComponent;
	private static Method chatSerializerMethodA, jsonComponentMethod;
	private static Class<?> chatSerializer;

	private static ReentrantLock LOCK;

	static {
		if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_16_R1)) {
			LOCK = new ReentrantLock();
		}

		EMPTY_COMPONENT = asComponent("");
	}

	private ReflectionUtils() {
	}

	public static Method jsonComponentMethod() {
		if (jsonComponentMethod != null) {
			return jsonComponentMethod;
		}

		try {
			Class<?>[] declaredClasses = ClazzContainer.getIChatBaseComponent().getDeclaredClasses();

			if (declaredClasses.length != 0) {
				return jsonComponentMethod = declaredClasses[0].getMethod("a", String.class);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return jsonComponentMethod;
	}

	public static JsonComponent getJsonComponent() {
		if (jsonComponent == null) {
			try {
				jsonComponent = new JsonComponent();
			} catch (NoClassDefFoundError e) {
			}
		}

		return jsonComponent;
	}

	public static Object asComponent(TabText text) {
		if (text.getJsonElements().isEmpty()) {
			return asComponent(text.getPlainText());
		}

		if (LOCK != null) {

			// JsonComponent#parseProperty takes a bit longer time than expected and in some
			// circumstances it can cause ThreadDeath (deadlock) because of the synchronized
			// method. With this lock now the current thread will be paused until the thread
			// unlocks this lock. So multiple thread can await for it to be done.
			LOCK.lock();

			Object component;
			try {
				component = getJsonComponent().parseProperty(text.getPlainText(), text.getJsonElements());
			} finally {
				LOCK.unlock();
			}

			return component;
		}

		StringBuilder result = new StringBuilder("[");
		int index = 0, jsonStartingIndex, beginIndex = 0;
		String strJson = text.getJsonElements().get(0).plainJson;
		String plainText = text.getPlainText();

		while ((jsonStartingIndex = plainText.indexOf(strJson)) != -1) {
			if (beginIndex + 1 != jsonStartingIndex) {
				result.append("{\"text\":\"").append(plainText.substring(beginIndex, jsonStartingIndex)).append("\"},");
			}

			result.append(strJson.substring(4).replace("}]", "}"));
			index++;

			if (index >= text.getJsonElements().size()) {
				break;
			}

			beginIndex = jsonStartingIndex + strJson.length();
			strJson = text.getJsonElements().get(index).plainJson;
			result.append(",");
		}

		result.append(",{\"text\":\"").append(plainText.substring(jsonStartingIndex + strJson.length())).append("\"}]");

		try {
			return PacketNM.NMS_PACKET.fromJson(result.toString());
		} catch (Exception ex) {
			try {
				if (ServerVersion.isCurrentLower(ServerVersion.v1_8_R2)) {
					return asChatSerializer(result.toString());
				}

				return jsonComponentMethod().invoke(ClazzContainer.getIChatBaseComponent(), result.toString());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return null;
	}

	public static Object asComponent(final String text) {
		if (LOCK != null) {
			LOCK.lock();

			Object component;
			try {
				component = getJsonComponent().parseProperty(text, null);
			} finally {
				LOCK.unlock();
			}

			return component;
		}

		try {
			return PacketNM.NMS_PACKET.fromJson("{\"text\":\"" + text + "\"}");
		} catch (Exception ex) {
			try {
				if (ServerVersion.isCurrentLower(ServerVersion.v1_8_R2)) {
					return asChatSerializer("{\"text\":\"" + text + "\"}");
				}

				return jsonComponentMethod().invoke(ClazzContainer.getIChatBaseComponent(), "{\"text\":\"" + text + "\"}");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return null;
	}

	private static Object asChatSerializer(String json) throws Exception {
		if (chatSerializer == null) {
			chatSerializer = Class.forName("net.minecraft.server." + ServerVersion.getArrayVersion()[3] + ".ChatSerializer");
		}

		if (chatSerializerMethodA == null) {
			chatSerializerMethodA = chatSerializer.getMethod("a", String.class);
		}

		return ClazzContainer.getIChatBaseComponent().cast(chatSerializerMethodA.invoke(chatSerializer, json));
	}
}
