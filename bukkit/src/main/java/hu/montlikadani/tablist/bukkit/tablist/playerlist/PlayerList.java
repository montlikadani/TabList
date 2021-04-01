package hu.montlikadani.tablist.bukkit.tablist.playerlist;

import org.bukkit.entity.Player;

import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.user.TabListUser;
import hu.montlikadani.tablist.bukkit.utils.ServerVersion;

public class PlayerList {

	private final TabList plugin;
	private final TabListUser user;

	public PlayerList(TabList plugin, TabListUser user) {
		this.plugin = plugin;
		this.user = user;
	}

	public void hideShow() {
		hide();
		showForWorld();
	}

	public void hide() {
		Player player = user.getPlayer();

		for (TabListUser user : plugin.getUsers()) {
			Player pl = user.getPlayer();

			hide(player, pl);
			hide(pl, player);
		}
	}

	public void show() {
		Player player = user.getPlayer();

		for (TabListUser user : plugin.getUsers()) {
			Player pl = user.getPlayer();

			show(player, pl);
			show(pl, player);
		}
	}

	public void showEveryone() {
		Player player = user.getPlayer();

		for (TabListUser user : plugin.getUsers()) {
			Player pls = user.getPlayer();

			if (!pls.canSee(player)) {
				show(pls, player);
			}

			if (!player.canSee(pls)) {
				show(player, pls);
			}
		}
	}

	public void showForWorld() {
		Player player = user.getPlayer();

		for (TabListUser user : plugin.getUsers()) {
			Player pls = user.getPlayer();

			if (player.getWorld().getUID().equals(pls.getWorld().getUID())) {
				show(player, pls);
				show(pls, player);
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
