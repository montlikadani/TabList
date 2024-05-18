package hu.montlikadani.tablist.utils.reflection;

import hu.montlikadani.tablist.tablist.TabText;

import java.util.concurrent.locks.ReentrantLock;

public final class ComponentParser {

	public static final Object EMPTY_COMPONENT;

	private static JsonComponent jsonComponent;

	private static final ReentrantLock LOCK = new ReentrantLock();

	static {
		EMPTY_COMPONENT = asComponent("");
	}

	private ComponentParser() {
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
		java.util.List<TabText.JsonElementData> json = text.getJsonElements();

		// JsonComponent#parseProperty takes a bit longer time than expected and in some
		// circumstances it can cause ThreadDeath (deadlock) due to long operation.
		// With this lock now the current thread will be paused until the operation
		// is working. So multiple thread can await for it to be done.
		LOCK.lock();

		Object component;
		try {
			component = getJsonComponent().parseProperty(text.getPlainText(), json.isEmpty() ? null : json);
		} finally {
			LOCK.unlock();
		}

		return component;
	}

	public static Object asComponent(String text) {
		LOCK.lock();

		Object component;
		try {
			component = getJsonComponent().parseProperty(text, null);
		} finally {
			LOCK.unlock();
		}

		return component;
	}
}
