package hu.montlikadani.tablist.bukkit.utils.plugin;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import net.milkbowl.vault.permission.Permission;

public final class VaultPermission {

	private Permission perm;

	public VaultPermission() {
		org.bukkit.plugin.RegisteredServiceProvider<Permission> rsp = Bukkit.getServer().getServicesManager()
				.getRegistration(Permission.class);
		if (rsp != null) {
			this.perm = rsp.getProvider();
		}
	}

	public Permission getPermission() {
		return perm;
	}

	public String getPrimaryGroup(String world, OfflinePlayer player) {
		try {
			return perm.getPrimaryGroup(world, player);
		} catch (UnsupportedOperationException e) {
		}

		return null;
	}

	public String getPrimaryGroup(Player player) {
		try {
			return perm.getPrimaryGroup(player);
		} catch (UnsupportedOperationException e) {
		}

		return null;
	}

	public String[] getPlayerGroups(Player player) {
		try {
			return perm.getPlayerGroups(player);
		} catch (UnsupportedOperationException e) {
		}

		return new String[0];
	}

	public String[] getGroups() {
		try {
			return perm.getGroups();
		} catch (UnsupportedOperationException e) {
		}

		return new String[0];
	}
}
