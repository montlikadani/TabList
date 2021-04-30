package hu.montlikadani.tablist.bukkit.API;

import java.util.UUID;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.tablist.TabManager;
import hu.montlikadani.tablist.bukkit.tablist.TabTitle;
import hu.montlikadani.tablist.bukkit.tablist.fakeplayers.FakePlayers;
import hu.montlikadani.tablist.bukkit.tablist.fakeplayers.IFakePlayers;
import hu.montlikadani.tablist.bukkit.utils.reflection.ReflectionUtils;

/**
 * The API methods for TabList.
 * 
 * @author montlikadani
 */
public class TabListAPI {

	/**
	 * Returns TabListAPI as a plugin
	 * 
	 * @return Plugin
	 */
	public static TabList getPlugin() {
		return JavaPlugin.getPlugin(TabList.class);
	}

	/**
	 * Checks whatever the tablist toggled for the specified player uuid. This is
	 * much slower to respond because it converts the string to uuid.
	 * 
	 * @param uuid Player UUID
	 * @return true if toggled
	 */
	public static boolean isTabListToggled(String uuid) {
		Validate.notEmpty(uuid, "Player UUID can't be empty/null");

		return isTabListToggled(UUID.fromString(uuid));
	}

	/**
	 * Checks whatever the tablist toggled for the specified player.
	 * 
	 * @param uuid Player
	 * @return true if toggled
	 */
	public static boolean isTabListToggled(Player player) {
		Validate.notNull(player, "Player can't be null");

		return isTabListToggled(player.getUniqueId());
	}

	/**
	 * Checks whatever the tablist toggled for the specified player uuid.
	 * 
	 * @param uuid Player UUID
	 * @return true if toggled
	 */
	public static boolean isTabListToggled(UUID uuid) {
		Validate.notNull(uuid, "Player UUID can't be null");

		return TabManager.TABENABLED.getOrDefault(uuid, false);
	}

	/**
	 * Sends the tab header and footer to the given player
	 * 
	 * @param p      Player
	 * @param string Header
	 * @param string Footer
	 */
	public static void sendTabList(Player p, String header, String footer) {
		Validate.notNull(p, "Player can't be null");

		TabTitle.sendTabTitle(p, header, footer);
	}

	/**
	 * Sends the tab header and footer to all online players
	 * 
	 * @param string Header
	 * @param string Footer
	 */
	public static void sendTabList(String header, String footer) {
		Bukkit.getOnlinePlayers().forEach(pls -> TabTitle.sendTabTitle(pls, header, footer));
	}

	/**
	 * Creates a new fake player that only appear in tablist.
	 * 
	 * @param who  the player who's own that player
	 * @param name the fake player name
	 * @return {@link IFakePlayers}
	 * @see IFakePlayers#createFakePlayer(Player, String, int)
	 */
	public static IFakePlayers createFakePlayer(Player who, String name) {
		IFakePlayers fp = new FakePlayers();
		fp.setName(name);
		fp.createFakePlayer(who, "", -1);
		return fp;
	}

	/**
	 * Gets the current ping of player
	 * 
	 * @param player Player
	 * @return Ping integer
	 */
	public static int getPing(Player p) {
		Validate.notNull(p, "Player can't be null");

		try {
			Object nmsPlayer = ReflectionUtils.getHandle(p);
			return ReflectionUtils.getField(nmsPlayer.getClass(), "ping", false).getInt(nmsPlayer);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return 0;
	}

	/**
	 * Gets the current tps (ticks per second) value of the server.
	 * 
	 * @return The current TPS
	 */
	public static double getTPS() {
		try {
			Object server = ReflectionUtils.invokeMethod(Bukkit.getServer(), "getServer", false);
			return ((double[]) ReflectionUtils.getField(server.getClass(), "recentTps", false).get(server))[0];
		} catch (Exception e) {
		}

		return 0d;
	}
}
