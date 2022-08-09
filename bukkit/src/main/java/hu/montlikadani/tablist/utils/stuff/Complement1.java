package hu.montlikadani.tablist.utils.stuff;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Scoreboard;

@SuppressWarnings("deprecation")
public final class Complement1 implements Complement {

	private boolean isCriteriaExists;

	public Complement1() {
		try {
			Class.forName("org.bukkit.scoreboard.Criteria");
			isCriteriaExists = true;
		} catch (ClassNotFoundException e) {
			isCriteriaExists = false;
		}
	}

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
	public Objective registerNewObjective(Scoreboard board, String name, String criteria /* Switch to enum? */, String displayName,
			RenderType renderType) {
		if (isCriteriaExists) {
			org.bukkit.scoreboard.Criteria crit;

			switch (criteria) {
			case "health":
				crit = org.bukkit.scoreboard.Criteria.HEALTH;
				break;
			case "dummy": // not used
				crit = org.bukkit.scoreboard.Criteria.DUMMY;
				break;
			default:
				return null; // prob not
			}

			return board.registerNewObjective(name, crit, displayName, renderType);
		}

		return board.registerNewObjective(name, criteria, displayName, renderType);
	}
}
