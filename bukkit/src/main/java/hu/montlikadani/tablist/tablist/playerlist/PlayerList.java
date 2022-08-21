package hu.montlikadani.tablist.tablist.playerlist;

import java.util.List;

import org.bukkit.World;
import org.bukkit.entity.Player;

import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.config.constantsLoader.ConfigValues;
import hu.montlikadani.tablist.user.TabListUser;
import hu.montlikadani.tablist.utils.ServerVersion;

public class PlayerList {

	private transient final TabList plugin;
	private transient final TabListUser user;

	public PlayerList(TabList plugin, TabListUser user) {
		this.plugin = plugin;
		this.user = user;
	}

	public void hide() {
		Player player = user.getPlayer();

		if (player == null) {
			return;
		}

		boolean checked = false;

		for (TabListUser user : plugin.getUsers()) {
			if (!checked && user.getUniqueId().equals(this.user.getUniqueId())) {
				checked = true;
				continue; // Only hide other players
			}

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

		boolean checked = false;

		for (TabListUser user : plugin.getUsers()) {
			if (!checked && user.getUniqueId().equals(this.user.getUniqueId())) {
				checked = true;
				continue; // Only hide other players
			}

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
		World playerWorld = player.getWorld();
		java.util.UUID worldId = playerWorld.getUID();

		for (TabListUser user : plugin.getUsers()) {
			if (!checked && user.getUniqueId().equals(this.user.getUniqueId())) {
				checked = true;
				continue; // Only show other players
			}

			Player pls = user.getPlayer();

			if (pls != null && worldId.equals(pls.getWorld().getUID())) {
				show(player, pls);
				show(pls, player);
			}
		}

		String playerWorldName = playerWorld.getName();
		int index = -1;

		checked = false;

		for (List<String> keys : ConfigValues.PER_WORLD_LIST_NAMES) {

			// Find the proper player's world in the keys list
			for (int i = 0; i < keys.size(); i++) {
				if (playerWorldName.equalsIgnoreCase(keys.get(i))) {
					index = i;
					break;
				}
			}

			if (index == -1) {
				continue; // If the player world not exist in the first list, continue
			}

			for (int a = 0; a < keys.size(); a++) {
				if (index == a) {
					continue; // The other players already visible in the player's world
				}

				World world = plugin.getServer().getWorld(keys.get(a));

				if (world == null) {
					continue;
				}

				for (Player worldPlayer : world.getPlayers()) {
					if (!checked && worldPlayer.getUniqueId().equals(this.user.getUniqueId())) {
						checked = true;
						continue;
					}

					show(worldPlayer, player);
					show(player, worldPlayer);
				}
			}

			// Break iteration, the other players in another world is now visible to the
			// source player
			break;
		}
	}

	@SuppressWarnings("deprecation")
	private void hide(Player to, Player pls) {
		if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_12_R1)) {
			to.hidePlayer(plugin, pls);
		} else {
			to.hidePlayer(pls);
		}
	}

	@SuppressWarnings("deprecation")
	private void show(Player to, Player pls) {
		if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_12_R1)) {
			to.showPlayer(plugin, pls);
		} else {
			to.showPlayer(pls);
		}
	}
}
