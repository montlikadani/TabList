package hu.montlikadani.tablist.API;

import java.lang.reflect.Field;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import hu.montlikadani.FakePlayer.FakePlayer_1_10_R1;
import hu.montlikadani.FakePlayer.FakePlayer_1_11_R1;
import hu.montlikadani.FakePlayer.FakePlayer_1_12_R1;
import hu.montlikadani.FakePlayer.FakePlayer_1_13_R1;
import hu.montlikadani.FakePlayer.FakePlayer_1_13_R2;
import hu.montlikadani.FakePlayer.FakePlayer_1_8_R3;
import hu.montlikadani.FakePlayer.FakePlayer_1_9_R2;
import hu.montlikadani.tablist.FakePlayer;
import hu.montlikadani.tablist.Packets;
import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.TabTitle;

public class TabListAPI {

	/**
	 * Returns TabListAPI as a plugin
	 *
	 * @return Plugin
	 */
	public static Plugin getPlugin() {
		if (TabList.getPlugin(TabList.class) == null) {
			throw new NullPointerException("plugin is null");
		}
		return TabList.getPlugin(TabList.class);
	}

	/**
	 * Sending the tab header and footer to player
	 * 
	 * @param player Player
	 * @param string Header
	 * @param string Footer
	 */
	public static void sendTabList(Player p, String header, String footer) {
		if (p == null) {
			throw new IllegalArgumentException("player is null");
		}
		TabTitle.sendTabTitle(p, header, footer);
	}

	/**
	 * Creates a fake player
	 * 
	 * @param name Fake player name
	 */
	public static void createFakePlayer(String name) {
		if (name == null || name == "") {
			throw new IllegalArgumentException("name is null");
		}
		String version = TabList.getInstance().version;
		FakePlayer fp = null;

		if (version.equals("v1_8_R3")) {
			fp = new FakePlayer_1_8_R3();
		} else if (version.equals("v1_9_R2")) {
			fp = new FakePlayer_1_9_R2();
		} else if (version.equals("v1_10_R1")) {
			fp = new FakePlayer_1_10_R1();
		} else if (version.equals("v1_11_R1")) {
			fp = new FakePlayer_1_11_R1();
		} else if (version.equals("v1_12_R1")) {
			fp = new FakePlayer_1_12_R1();
		} else if (version.equals("v1_13_R1")) {
			fp = new FakePlayer_1_13_R1();
		} else if (version.equals("v1_13_R2")) {
			fp = new FakePlayer_1_13_R2();
		}
		fp.create(TabList.getInstance().colorMsg(name));
	}

	/**
	 * Removes the exists fake player
	 * 
	 * @param name Fake player name
	 */
	public static void removeFakePlayer(String name) {
		if (name == null || name == "") {
			throw new IllegalArgumentException("name is null");
		}
		String version = TabList.getInstance().version;
		FakePlayer fp = null;

		if (version.equals("v1_8_R3")) {
			fp = new FakePlayer_1_8_R3();
		} else if (version.equals("v1_9_R2")) {
			fp = new FakePlayer_1_9_R2();
		} else if (version.equals("v1_10_R1")) {
			fp = new FakePlayer_1_10_R1();
		} else if (version.equals("v1_11_R1")) {
			fp = new FakePlayer_1_11_R1();
		} else if (version.equals("v1_12_R1")) {
			fp = new FakePlayer_1_12_R1();
		} else if (version.equals("v1_13_R1")) {
			fp = new FakePlayer_1_13_R1();
		} else if (version.equals("v1_13_R2")) {
			fp = new FakePlayer_1_13_R2();
		}
		fp.remove(name);
	}

	/**
	 * Gets the current ping of player
	 * 
	 * @throws Throwable
	 * @param player Player
	 * @return Ping integer
	 */
	public static int getPing(Player p) throws Throwable {
		if (p == null) {
			throw new IllegalArgumentException("player is null");
		}
		int pingInt = 0;
		Object nmsPlayer = Packets.getNMSPlayer(p);
		Field ping = nmsPlayer.getClass().getField("ping");
		ping.setAccessible(true);
		pingInt = ping.getInt(nmsPlayer);
		return pingInt;
	}
}
