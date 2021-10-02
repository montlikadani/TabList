package hu.montlikadani.tablist.bukkit.api;

import java.util.UUID;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.tablist.TabTitle;
import hu.montlikadani.tablist.bukkit.tablist.TabToggleBase;
import hu.montlikadani.tablist.bukkit.tablist.fakeplayers.FakePlayers;
import hu.montlikadani.tablist.bukkit.tablist.fakeplayers.IFakePlayers;
import hu.montlikadani.tablist.bukkit.utils.reflection.ReflectionUtils;

/**
 * The API methods for TabList.
 * 
 * @author montlikadani
 */
public final class TabListAPI {

	private static java.lang.reflect.Field pingField, recentTpsField;
	private static java.lang.reflect.Method serverMethod;

	static {
		try {
			(serverMethod = Bukkit.getServer().getClass().getDeclaredMethod("getServer")).setAccessible(true);
		} catch (NoSuchMethodException ex) {
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
		return TabToggleBase.TAB_TOGGLE.contains(uuid);
	}

	/**
	 * Sends the tab header and footer to the given player
	 * 
	 * @param p      Player
	 * @param string Header
	 * @param string Footer
	 */
	public static void sendTabList(Player p, String header, String footer) {
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
	 * @param name the fake player name
	 * @return {@link IFakePlayers}
	 * @see IFakePlayers#createFakePlayer(String, int)
	 */
	public static IFakePlayers createFakePlayer(String name) {
		IFakePlayers fp = new FakePlayers();
		fp.setName(name);
		fp.createFakePlayer("", -1);
		return fp;
	}

	/**
	 * Returns the amount of latency of the given player.
	 * 
	 * @param player Player
	 * @return the current amount of ping of the given player
	 */
	public static int getPing(Player player) {
		try {
			return player.getPing();
		} catch (NoSuchMethodError e) {
			try {
				Object entityPlayer = ReflectionUtils.getPlayerHandle(player);

				if (pingField == null) {
					(pingField = entityPlayer.getClass().getField("ping")).setAccessible(true);
				}

				return pingField.getInt(entityPlayer);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		return 0;
	}

	/**
	 * Returns the current TPS (ticks per second) value of the server.
	 * 
	 * @return The first value of TPS array according to
	 *         {@link org.bukkit.Server#getTPS()}
	 */
	public static double getTPS() {
		try {
			return Bukkit.getServer().getTPS()[0];
		} catch (NoSuchMethodError e) {
			try {
				Object server = serverMethod.invoke(Bukkit.getServer());

				if (recentTpsField == null) {
					(recentTpsField = server.getClass().getField("recentTps")).setAccessible(true);
				}

				return ((double[]) recentTpsField.get(server))[0];
			} catch (Exception ex) {
			}
		}

		return 0d;
	}
}
