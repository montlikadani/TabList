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

			if (pls != null && worldId.equals(pls.getWorld().getUID())) {
				show(player, pls);
				show(pls, player);
			}
		}

		String playerWorldName = player.getWorld().getName();
		boolean isInPlayerWorld = false;

		for (List<String> keys : ConfigValues.PER_WORLD_LIST_NAMES) {

			// Find the proper player's world in the keys list
			for (String name : keys) {
				isInPlayerWorld = playerWorldName.equalsIgnoreCase(name);

				if (isInPlayerWorld) {
					break;
				}
			}

			if (!isInPlayerWorld) {
				continue; // If the player world not exist in the first list, continue
			}

			for (String name : keys) {
				if (playerWorldName.equalsIgnoreCase(name)) {
					continue; // The other players already visible in the player's world
				}

				org.bukkit.World world = plugin.getServer().getWorld(name);

				if (world != null) {
					for (Player worldPlayer : world.getPlayers()) {
						show(worldPlayer, player);
						show(player, worldPlayer);
					}
				}
			}

			// Break iteration, the other players in another world is now visible to the
			// source player
			break;
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
