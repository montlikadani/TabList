package hu.montlikadani.tablist.bukkit.listeners.plugins;

import static hu.montlikadani.tablist.bukkit.utils.Util.colorMsg;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import hu.montlikadani.tablist.bukkit.API.TabListAPI;
import hu.montlikadani.tablist.bukkit.config.CommentedConfig;
import hu.montlikadani.tablist.bukkit.config.constantsLoader.ConfigValues;
import hu.montlikadani.tablist.bukkit.tablist.playerlist.PlayerList;

public abstract class AfkPlayers {

	protected final Map<Player, String> afkPlayers = new HashMap<>();

	protected void goAfk(Player player, boolean value) {
		if (ConfigValues.isAfkStatusEnabled() && !ConfigValues.isAfkStatusShowPlayerGroup()) {
			CommentedConfig conf = TabListAPI.getPlugin().getConfig();
			String path = "placeholder-format.afk-status.format-" + (value ? "yes" : "no");
			String result = "";

			if (conf.contains(path)) {
				result = colorMsg(
						ConfigValues.isAfkStatusShowInRightLeftSide() ? player.getName() + conf.getString(path)
								: conf.getString(path) + player.getName());
			}

			sortAfkPlayers(player, value);

			if (!result.isEmpty()) {
				TabListAPI.getPlugin().getComplement().setPlayerListName(player, result);
			}
		}

		if (ConfigValues.isHidePlayerFromTabAfk()) {
			if (value) {
				PlayerList.hidePlayer(player);
			} else {
				PlayerList.showPlayer(player);
			}
		}
	}

	// TODO Improve
	protected void sortAfkPlayers(Player target, boolean value) {
		if (!ConfigValues.isAfkSortLast()) {
			return;
		}

		if (!value && afkPlayers.containsKey(target)) {
			Scoreboard board = target.getScoreboard();
			Team team = board.getTeam(afkPlayers.get(target));
			if (team != null) {
				team.unregister();
				target.setScoreboard(board);
			}

			afkPlayers.remove(target);
			return;
		}

		final List<String> sortedPlayers = Bukkit.getOnlinePlayers().stream().map(Player::getName).sorted()
				.collect(Collectors.toList());

		int i = 0;
		while (i < sortedPlayers.size()) {
			Player p = Bukkit.getPlayer(sortedPlayers.get(i));
			int position = i;

			if (target == p) {
				position += sortedPlayers.size();
			}

			String name = 100000 + position + p.getName();
			if (name.length() > 16) {
				name = name.substring(0, 16);
			}

			Scoreboard board = p.getScoreboard();
			Team team = board.getTeam(name);
			if (team == null) {
				team = board.registerNewTeam(name);
			}

			if (target == p) {
				afkPlayers.put(p, name);
			}

			p.setScoreboard(board);
			i++;
		}

		sortedPlayers.clear();
	}
}
