package hu.montlikadani.tablist.bukkit.listeners;

import static hu.montlikadani.tablist.bukkit.utils.Util.colorMsg;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import hu.montlikadani.tablist.bukkit.ConfigValues;
import hu.montlikadani.tablist.bukkit.PlayerList;
import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.utils.NMS;
import net.ess3.api.events.AfkStatusChangeEvent;

public class EssAfkStatus implements Listener {

	private final Map<Player, String> afkPlayers = new HashMap<>();

	@EventHandler
	public void onAfkChange(AfkStatusChangeEvent event) {
		Player p = event.getAffected().getBase();
		boolean value = event.getValue();

		if (ConfigValues.isAfkStatusEnabled() && !ConfigValues.isAfkStatusShowPlayerGroup()) {
			boolean rightLeft = ConfigValues.isAfkStatusShowInRightLeftSide();

			org.bukkit.configuration.file.FileConfiguration conf = TabList.getInstance().getC();
			String path = "placeholder-format.afk-status.format-" + (value ? "yes" : "no");
			String result = "";

			if (conf.contains(path)) {
				result = colorMsg(rightLeft ? p.getName() + conf.getString(path) : conf.getString(path) + p.getName());
			}

			sortAfkPlayers(p, value);

			if (!result.isEmpty()) {
				p.setPlayerListName(result);
			}
		}

		if (ConfigValues.isHidePlayerFromTabAfk()) {
			if (value) {
				PlayerList.hidePlayer(p);
			} else {
				PlayerList.showPlayer(p);
			}
		}
	}

	private void sortAfkPlayers(Player target, boolean value) {
		if (!ConfigValues.isAfkSortLast()) {
			return;
		}

		if (!value && afkPlayers.containsKey(target)) {
			Scoreboard board = target.getScoreboard();
			Team team = NMS.getTeam(target, board, afkPlayers.get(target));
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
			Team team = NMS.getTeam(p, board, name);
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