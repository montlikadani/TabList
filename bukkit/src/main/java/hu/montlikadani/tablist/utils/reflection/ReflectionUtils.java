package hu.montlikadani.tablist.utils.reflection;

import java.lang.reflect.Method;
import java.util.concurrent.locks.ReentrantLock;

import hu.montlikadani.tablist.tablist.TabText;
import hu.montlikadani.tablist.utils.ServerVersion;

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

		EMPTY_COMPONENT = emptyComponent();
	}

	private ReflectionUtils() {
	}

	private static Object emptyComponent() {
		try {
			return asComponent("");
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
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

	public static Object asComponent(TabText text) throws Exception {
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

		// A bit complicated way to split json and texts from each other
		StringBuilder result = new StringBuilder();
		String strJson = text.getJsonElements().get(0).plainJson, plainText = text.getPlainText();
		int textLength = -1, from = 0, index = 0;

		int jsonIndex = plainText.indexOf(strJson);

		if (jsonIndex != -1) {
			result.append("{\"text\":\"" + plainText.substring(from, jsonIndex) + "\",\"extra\":" + strJson.replace("}]}]", "}]}"));
		}

		while (jsonIndex != -1) {
			if (textLength == -1) {
				textLength = plainText.length();
			}

			int length = strJson.length();

			if ((from = jsonIndex + length) >= textLength) {
				from = length;
			}

			result.append(",");
			index++;

			if (index >= text.getJsonElements().size()) {
				break;
			}

			strJson = text.getJsonElements().get(index).plainJson;

			if ((jsonIndex = plainText.indexOf(strJson, from)) == -1) {
				break;
			}

			result.append("{\"text\":\"" + plainText.substring(from, jsonIndex) + "\",\"extra\":" + strJson + "}");
		}

		// The remaining text after last json
		if (from != 0) {
			result.append("{\"text\":\"" + plainText.substring(from) + "\"}]}");
		} else {
			result.setLength(0);
			result.append("{\"text\":\"" + plainText + "\"}");
		}

		if (ServerVersion.isCurrentLower(ServerVersion.v1_8_R2)) {
			return asChatSerializer(result.toString());
		}

		return jsonComponentMethod().invoke(ClazzContainer.getIChatBaseComponent(), result.toString());
	}

	public static Object asComponent(final String text) throws Exception {
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

		if (ServerVersion.isCurrentLower(ServerVersion.v1_8_R2)) {
			return asChatSerializer("{\"text\":\"" + text + "\"}");
		}

		return jsonComponentMethod().invoke(ClazzContainer.getIChatBaseComponent(), "{\"text\":\"" + text + "\"}");
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