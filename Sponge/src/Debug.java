package hu.montlikadani.tablist.sponge;

import org.slf4j.Logger;

public class Debug {

	private static final Logger LOG = TabList.get().getPluginContainer().getLogger();

	public static void debug(String msg) {
		LOG.debug(msg);
	}

	public static void error(String msg) {
		LOG.error(msg);
	}

	public static void warn(String msg) {
		LOG.warn(msg);
	}

	public static void info(String msg) {
		LOG.info(msg);
	}
}
