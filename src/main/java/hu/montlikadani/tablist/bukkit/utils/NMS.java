package hu.montlikadani.tablist.bukkit.utils;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import hu.montlikadani.tablist.bukkit.utils.ServerVersion.Version;

@SuppressWarnings("deprecation")
public abstract class NMS {

	public static void addEntry(Player player, Team team) {
		if (Version.isCurrentLower(Version.v1_9_R1)) {
			if (!team.hasPlayer(player)) {
				team.addPlayer(player);
			}
		} else if (!team.hasEntry(player.getName())) {
			team.addEntry(player.getName());
		}
	}

	public static void removeEntry(Player player, Scoreboard board) {
		if (Version.isCurrentLower(Version.v1_9_R1)) {
			Team team = board.getPlayerTeam(player);
			if (team != null)
				team.removePlayer(player);
		} else {
			Team team = board.getEntryTeam(player.getName());
			if (team != null)
				team.removeEntry(player.getName());
		}
	}
}
