package hu.montlikadani.tablist.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import hu.montlikadani.tablist.bukkit.utils.ServerVersion.Version;

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
	static void hide(Player to, Player pls) {
		if (Version.isCurrentEqualOrHigher(Version.v1_12_R1)) {
			to.hidePlayer(TabList.getInstance(), pls);
		} else {
			to.hidePlayer(pls);
		}
	}

	@SuppressWarnings("deprecation")
	static void show(Player to, Player pls) {
		if (Version.isCurrentEqualOrHigher(Version.v1_12_R1)) {
			to.showPlayer(TabList.getInstance(), pls);
		} else {
			to.showPlayer(pls);
		}
	}
}
