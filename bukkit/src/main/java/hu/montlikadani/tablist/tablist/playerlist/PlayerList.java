package hu.montlikadani.tablist.tablist.playerlist;

import java.util.List;

import org.bukkit.entity.Player;

import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.config.constantsLoader.ConfigValues;
import hu.montlikadani.tablist.user.TabListUser;
import hu.montlikadani.tablist.utils.ServerVersion;

public class PlayerList {

	private final TabList plugin;
	private final TabListUser user;

	public PlayerList(TabList plugin, TabListUser user) {
		this.plugin = plugin;
		this.user = user;
	}

	public void hideShow() {
		hide();
		displayInWorld();
	}

	public void hide() {
		Player player = user.getPlayer();

		if (player == null) {
			return;
		}

		for (TabListUser user : plugin.getUsers()) {
			Player pl = user.getPlayer();

			if (pl == null) {
				continue;
			}

			hide(player, pl);
			hide(pl, player);
		}
	}

	public void showEveryone() {
		Player player = user.getPlayer();

		if (player == null) {
			return;
		}

		for (TabListUser user : plugin.getUsers()) {
			Player pls = user.getPlayer();

			if (pls == null) {
				continue;
			}

			show(pls, player);
			show(player, pls);
		}
	}

	public void displayInWorld() {
		Player player = user.getPlayer();

		if (player == null) {
			return;
		}

		boolean checked = false;
		java.util.UUID worldId = player.getWorld().getUID();

		for (TabListUser user : plugin.getUsers()) {
			if (!checked && user.getUniqueId().equals(this.user.getUniqueId())) {
				show(player, player);
				show(player, player);
				checked = true;
				continue;
			}

			Player pls = user.getPlayer();

			if (pls == null) {
				continue;
			}

			org.bukkit.World playerWorld = pls.getWorld();

			if (worldId.equals(playerWorld.getUID())) {
				show(player, pls);
				show(pls, player);
				continue;
			}

			if (ConfigValues.PER_WORLD_LIST_NAMES.isEmpty()) {
				continue;
			}

			String worldName = playerWorld.getName();

			for (List<String> keys : ConfigValues.PER_WORLD_LIST_NAMES) {
				for (String name : keys) {
					if (name.equalsIgnoreCase(worldName)) {
						show(pls, player);
						show(player, pls);
						break;
					}
				}
			}
		}
	}

	@SuppressWarnings("deprecation")
	public final void hide(Player to, Player pls) {
		if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_12_R1)) {
			to.hidePlayer(plugin, pls);
		} else {
			to.hidePlayer(pls);
		}
	}

	@SuppressWarnings("deprecation")
	public final void show(Player to, Player pls) {
		if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_12_R1)) {
			to.showPlayer(plugin, pls);
		} else {
			to.showPlayer(pls);
		}
	}
}
