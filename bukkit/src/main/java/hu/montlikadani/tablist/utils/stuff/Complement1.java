package hu.montlikadani.tablist.utils.stuff;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Scoreboard;

@SuppressWarnings("deprecation")
public final class Complement1 implements Complement {

	@Override
	public void playerListName(Player player, String text) {
		player.setPlayerListName(text);
	}

	@Override
	public String displayName(Player player) {
		return player.getDisplayName();
	}

	@Override
	public String motd() {
		return org.bukkit.Bukkit.getServer().getMotd();
	}

	@Override
	public void displayName(Objective objective, String dName) {
		objective.setDisplayName(dName);
	}

	@Override
	public Objective registerNewObjective(Scoreboard board, String name, String criteria, String displayName,
			RenderType renderType) {
		return board.registerNewObjective(name, criteria, displayName, renderType);
	}
}
