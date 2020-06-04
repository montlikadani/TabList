package hu.montlikadani.tablist.sponge;

import org.slf4j.Logger;

public class Debug {

	private static Logger log = TabList.get().getPluginContainer().getLogger();

	public static void debug(String msg) {
		log.debug(msg);
	}

	public static void error(String msg) {
		log.error(msg);
	}

	public static void warn(String msg) {
		log.warn(msg);
	}

	public static void info(String msg) {
		log.info(msg);
	}
}
