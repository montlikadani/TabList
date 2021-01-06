package hu.montlikadani.tablist.bukkit.tablist.groups;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import hu.montlikadani.tablist.bukkit.TabListPlayer;
import hu.montlikadani.tablist.bukkit.utils.Misc;

/**
 * This class is superfluous because it makes no sense to instantiate it as long
 * as there is no solution to fix multiple issues, as we use reflections to make
 * scoreboard teams work properly (like scoreboard conflict).
 */
public class BukkitHandled implements ITabScoreboard {

	private Scoreboard board;

	private TabListPlayer tabPlayer;

	public BukkitHandled(TabListPlayer tabPlayer) {
		this.tabPlayer = tabPlayer;
	}

	@Override
	public TabListPlayer getTabPlayer() {
		return tabPlayer;
	}

	@Override
	public void registerTeam(String teamName) {
		if (board == null) {
			return;
		}

		Team team = board.getTeam(teamName);
		if (team == null) {
			team = board.registerNewTeam(teamName);
		}

		Misc.addEntry(tabPlayer.getPlayer(), team);
	}

	@Override
	public void setTeam(String teamName) {
		tabPlayer.getPlayer().setPlayerListName(tabPlayer.getCustomTabName());

		if (board != null) {
			tabPlayer.getPlayer().setScoreboard(board);
		}
	}

	@Override
	public void unregisterTeam(String teamName) {
		Team team = board == null ? null : board.getTeam(teamName);
		if (team == null) {
			return;
		}

		Player player = tabPlayer.getPlayer();
		player.setPlayerListName(player.getName());

		Misc.removeEntry(player, board);
		team.unregister();
		player.setScoreboard(board);
	}

	@Override
	public Scoreboard getScoreboard() {
		return board;
	}

	@Override
	public void setScoreboard(Scoreboard board) {
		this.board = board;
	}

}
