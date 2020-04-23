package hu.montlikadani.tablist.Sponge;

import org.slf4j.Logger;

public class Debug {

	private static Logger log = TabList.get().getPluginContainer().getLogger();

	static void debug(String msg) {
		log.debug(msg);
	}

	static void error(String msg) {
		log.error(msg);
	}

	static void warn(String msg) {
		log.warn(msg);
	}

	static void info(String msg) {
		log.info(msg);
	}
}
