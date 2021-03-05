package hu.montlikadani.tablist.bukkit.utils.stuff;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Scoreboard;

@SuppressWarnings("deprecation")
public final class Complement1 implements Complement {

	@Override
	public void setPlayerListName(Player player, String text) {
		player.setPlayerListName(text);
	}

	@Override
	public String getDisplayName(Player player) {
		return player.getDisplayName();
	}

	@Override
	public String getMotd() {
		return Bukkit.getServer().getMotd();
	}

	@Override
	public void setDisplayName(Objective objective, String dName) {
		objective.setDisplayName(dName);
	}

	@Override
	public Objective registerNewObjective(Scoreboard board, String name, String criteria, String displayName,
			RenderType renderType) {
		return board.registerNewObjective(name, criteria, displayName, renderType);
	}

	@Override
	public String getPlayerListName(Player player) {
		return player.getPlayerListName();
	}
}
