package hu.montlikadani.tablist.bukkit.tablist.playerlist;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import hu.montlikadani.tablist.bukkit.API.TabListAPI;
import hu.montlikadani.tablist.bukkit.utils.ServerVersion;

public class PlayerList {

	public static void hideShow() {
		Bukkit.getOnlinePlayers().forEach(PlayerList::hideShow);
	}

	public static void hideShow(Player p) {
		hidePlayer(p);
		showPlayerForWorld(p);
	}

	public static void hidePlayer(Player p) {
		for (Player pls : Bukkit.getOnlinePlayers()) {
			hide(p, pls);
			hide(pls, p);
		}
	}

	public static void showPlayer(Player p) {
		for (Player pls : Bukkit.getOnlinePlayers()) {
			show(p, pls);
			show(pls, p);
		}
	}

	public static void showPlayerForWorld(Player p) {
		for (Player pls : Bukkit.getOnlinePlayers()) {
			if (p.getWorld().equals(pls.getWorld())) {
				show(p, pls);
				show(pls, p);
			}
		}
	}

	public static void showEveryone(Player p) {
		for (Player pls : Bukkit.getOnlinePlayers()) {
			if (!pls.canSee(p)) {
				show(pls, p);
			}

			if (!p.canSee(pls)) {
				show(p, pls);
			}
		}
	}

	@SuppressWarnings("deprecation")
	public static void hide(Player to, Player pls) {
		if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_12_R1)) {
			to.hidePlayer(TabListAPI.getPlugin(), pls);
		} else {
			to.hidePlayer(pls);
		}
	}

	@SuppressWarnings("deprecation")
	public static void show(Player to, Player pls) {
		if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_12_R1)) {
			to.showPlayer(TabListAPI.getPlugin(), pls);
		} else {
			to.showPlayer(pls);
		}
	}
}
