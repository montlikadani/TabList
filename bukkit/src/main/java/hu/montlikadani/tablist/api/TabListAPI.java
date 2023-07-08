package hu.montlikadani.tablist.api;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.packets.PacketNM;
import hu.montlikadani.tablist.tablist.TabText;

/**
 * The API methods for TabList.
 * 
 * @author montlikadani
 */
public final class TabListAPI {

	private static boolean isTpsMethodExists = false;
	private static boolean isPingMethodExists = false;

	static {
		try {
			Bukkit.getServer().getTPS();
			isTpsMethodExists = true;
		} catch (NoSuchMethodError ignored) {
		}

		try {
			Player.class.getDeclaredMethod("getPing");
			isPingMethodExists = true;
		} catch (NoSuchMethodException ignored) {
		}
	}

	/**
	 * Returns TabListAPI as a plugin
	 * 
	 * @return Plugin
	 */
	public static TabList getPlugin() {
		return JavaPlugin.getPlugin(TabList.class);
	}

	/**
	 * Sends the tab header and footer to the given player
	 * 
	 * @param p      {@link Player}
	 * @param header the header to display
	 * @param footer the footer to display
	 */
	public static void sendTabList(Player p, String header, String footer) {
		if (p != null) {
			PacketNM.NMS_PACKET.sendTabTitle(p, TabText.parseFromText(header).toComponent(), TabText.parseFromText(footer).toComponent());
		}
	}

	/**
	 * Sends the tab header and footer to all online players
	 * 
	 * @param header the header to display
	 * @param footer the footer to display
	 */
	public static void sendTabList(String header, String footer) {
		Object head = TabText.parseFromText(header).toComponent();
		Object foot = TabText.parseFromText(footer).toComponent();

		for (Player player : Bukkit.getOnlinePlayers()) {
			PacketNM.NMS_PACKET.sendTabTitle(player, head, foot);
		}
	}

	/**
	 * Returns the amount of latency of the given player.
	 * 
	 * @param player Player
	 * @return the current amount of ping of the given player
	 */
	public static int getPing(Player player) {
		return isPingMethodExists ? player.getPing() : PacketNM.NMS_PACKET.playerPing(player);
	}

	/**
	 * Returns the current TPS (ticks per second) value of the server.
	 * 
	 * @return The first value of TPS array according to {@link org.bukkit.Server#getTPS()}
	 */
	public static double getTPS() {
		return isTpsMethodExists ? Bukkit.getServer().getTPS()[0] : PacketNM.NMS_PACKET.serverTps();
	}
}
