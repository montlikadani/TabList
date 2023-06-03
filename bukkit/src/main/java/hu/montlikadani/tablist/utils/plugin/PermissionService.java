package hu.montlikadani.tablist.utils.plugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import net.milkbowl.vault.permission.Permission;

public final class PermissionService {

	public final boolean hasLuckPerms;

	private Permission perm;

	public PermissionService() {
		org.bukkit.plugin.RegisteredServiceProvider<Permission> rsp = Bukkit.getServer().getServicesManager().getRegistration(Permission.class);

		if (rsp != null) {
			perm = rsp.getProvider();
		}

		hasLuckPerms = Bukkit.getServer().getPluginManager().isPluginEnabled("LuckPerms");
	}

	public Permission getPermission() {
		return perm;
	}

	public Object groupObjectByName(String groupName) {
		return hasLuckPerms ? net.luckperms.api.LuckPermsProvider.get().getGroupManager().getGroup(groupName) : null;
	}

	public String getPrimaryGroup(Player player) {
		try {
			return perm.getPrimaryGroup(player);
		} catch (UnsupportedOperationException ignored) {
		}

		return null;
	}

	public boolean playerInGroup(Player player, String group) {
		try {
			if (perm.playerInGroup(player, group)) {
				return true;
			}

			// Secondary check because the above may fail at some cases
			for (String playerGroup : perm.getPlayerGroups(player)) {
				if (group.equalsIgnoreCase(playerGroup)) {
					return true;
				}
			}
		} catch (UnsupportedOperationException ignored) {
		}

		return false;
	}

	public boolean playerInGroup(Player player, String world, String group) {
		try {
			if (perm.playerInGroup(world, player, group)) {
				return true;
			}

			// Secondary check because the above may fail at some cases
			for (String playerGroup : perm.getPlayerGroups(world, player)) {
				if (group.equalsIgnoreCase(playerGroup)) {
					return true;
				}
			}
		} catch (UnsupportedOperationException ignored) {
		}

		return false;
	}

	public String[] getGroups() {
		try {
			return perm.getGroups();
		} catch (UnsupportedOperationException ignored) {
		}

		return new String[0];
	}
}
