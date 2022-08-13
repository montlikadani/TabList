package hu.montlikadani.tablist.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Containers.CMIUser;

import de.myzelyam.api.vanish.VanishAPI;
import hu.montlikadani.tablist.config.constantsLoader.ConfigValues;
import me.xtomyserrax.StaffFacilities.SFAPI;
import net.ess3.api.IEssentials;

public final class PluginUtils {

	private static final Plugin ESSENTIALS, CMIP, PEX, SUPER_VANISH, PREMIUM_VANISH, STAFF_FACILITIES;

	private static boolean isPurpurIsAfkMethodExists;

	static {
		org.bukkit.plugin.PluginManager pm = Bukkit.getServer().getPluginManager();

		// Some people still uses "PlugMan" things so we caches the plugin's object and
		// checks with isEnabled if its enabled
		ESSENTIALS = pm.getPlugin("Essentials");
		CMIP = pm.getPlugin("CMI");
		PEX = pm.getPlugin("PermissionsEx");
		SUPER_VANISH = pm.getPlugin("SuperVanish");
		PREMIUM_VANISH = pm.getPlugin("PremiumVanish");
		STAFF_FACILITIES = pm.getPlugin("StaffFacilities");

		// Purpur (not a plugin) - isAfk method
		try {
			Player.class.getMethod("isAfk");
			isPurpurIsAfkMethodExists = true;
		} catch (NoSuchMethodException e) {
			isPurpurIsAfkMethodExists = false;
		}
	}

	// Just a fast check instead of caching if the software is purpur
	public static boolean isPurpur() {
		return isPurpurIsAfkMethodExists;
	}

	public static boolean isAfk(Player player) {
		if (player == null) {
			return false;
		}

		if (ESSENTIALS != null && ESSENTIALS.isEnabled()) {
			return ((IEssentials) ESSENTIALS).getUser(player).isAfk();
		}

		if (CMIP != null && CMIP.isEnabled()) {
			CMIUser user = CMI.getInstance().getPlayerManager().getUser(player.getUniqueId());
			return user != null && user.isAfk();
		}

		return isPurpurIsAfkMethodExists && player.isAfk();
	}

	public static boolean isVanished(Player player) {
		if ((SUPER_VANISH != null && SUPER_VANISH.isEnabled())
				|| (PREMIUM_VANISH != null && PREMIUM_VANISH.isEnabled())) {
			return VanishAPI.isInvisibleOffline(player.getUniqueId());
		}

		if (ESSENTIALS != null && ESSENTIALS.isEnabled()) {
			return ((IEssentials) ESSENTIALS).getUser(player).isVanished();
		}

		if (STAFF_FACILITIES != null && STAFF_FACILITIES.isEnabled()) {
			return SFAPI.isPlayerVanished(player);
		}

		if (CMIP != null && CMIP.isEnabled()) {
			CMIUser user = CMI.getInstance().getPlayerManager().getUser(player.getUniqueId());
			return user != null && user.isVanished();
		}

		return false;
	}

	public static int countVanishedPlayers() {
		final int plSize = Bukkit.getServer().getOnlinePlayers().size();

		if (!ConfigValues.isIgnoreVanishedPlayers()) {
			return plSize;
		}

		int vanishedPlayers = getVanishedPlayers();
		return vanishedPlayers == 0 ? plSize : plSize - vanishedPlayers;
	}

	public static int getVanishedPlayers() {
		if ((SUPER_VANISH != null && SUPER_VANISH.isEnabled())
				|| (PREMIUM_VANISH != null && PREMIUM_VANISH.isEnabled())) {
			return VanishAPI.getInvisiblePlayers().size();
		}

		if (ESSENTIALS != null && ESSENTIALS.isEnabled()) {
			return ((IEssentials) ESSENTIALS).getVanishedPlayersNew().size();
		}

		if (STAFF_FACILITIES != null && STAFF_FACILITIES.isEnabled()) {
			return SFAPI.vanishedPlayersList().size();
		}

		if (CMIP != null && CMIP.isEnabled()) {
			return CMI.getInstance().getVanishManager().getAllVanished().size();
		}

		return 0;
	}

	public static boolean hasPermission(Player player, String perm) {
		if (PEX != null && PEX.isEnabled()) {
			try {
				return ru.tehkode.permissions.bukkit.PermissionsEx.getPermissionManager().has(player, perm);
			} catch (Throwable e) {
				// Pex2 supports bukkit provided "hasPermission" check
			}
		}

		return player.hasPermission(perm);
	}
}
