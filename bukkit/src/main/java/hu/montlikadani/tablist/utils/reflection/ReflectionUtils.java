package hu.montlikadani.tablist.utils.reflection;

import hu.montlikadani.tablist.tablist.TabText;

import java.util.concurrent.locks.ReentrantLock;

public final class ReflectionUtils {

	public static final Object EMPTY_COMPONENT;

	private static JsonComponent jsonComponent;

	private static final ReentrantLock LOCK = new ReentrantLock();

	static {
		EMPTY_COMPONENT = asComponent("");
	}

	private ReflectionUtils() {
	}

	private static JsonComponent getJsonComponent() {
		if (jsonComponent == null) {
			try {
				jsonComponent = new JsonComponent();
			} catch (NoClassDefFoundError ignored) {
			}
		}

		return jsonComponent;
	}

	public static Object asComponent(TabText text) {
		if (text.getJsonElements().isEmpty()) {
			return asComponent(text.getPlainText());
		}

		// JsonComponent#parseProperty takes a bit longer time than expected and in some
		// circumstances it can cause ThreadDeath (deadlock) due to long operation.
		// With this lock now the current thread will be paused until the operation
		// is working. So multiple thread can await for it to be done.
		LOCK.lock();

		Object component;
		try {
			component = getJsonComponent().parseProperty(text.getPlainText(), text.getJsonElements());
		} finally {
			LOCK.unlock();
		}

		return component;

		/*StringBuilder result = new StringBuilder("[");
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

		return PacketNM.NMS_PACKET.fromJson(Util.applyTextFormat(result.toString()));*/
	}

	public static Object asComponent(final String text) {
		LOCK.lock();

		Object component;
		try {
			component = getJsonComponent().parseProperty(text, null);
		} finally {
			LOCK.unlock();
		}

		return component;

		//return PacketNM.NMS_PACKET.fromJson("{\"text\":\"" + Util.applyTextFormat(text) + "\"}");
	}
}
