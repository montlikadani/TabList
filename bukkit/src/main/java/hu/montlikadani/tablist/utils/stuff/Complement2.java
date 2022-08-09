package hu.montlikadani.tablist.utils.stuff;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Scoreboard;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class Complement2 implements Complement {

	private boolean isCriteriaExists;

	public Complement2() {
		try {
			Class.forName("org.bukkit.scoreboard.Criteria");
			isCriteriaExists = true;
		} catch (ClassNotFoundException e) {
			isCriteriaExists = false;
		}
	}

	private Component deserialize(String t) {
		// legacySection is used to deserialize hex colors from plain text too
		return LegacyComponentSerializer.legacySection().deserialize(t);
	}

	private String serialize(Component component) {
		return component != null ? LegacyComponentSerializer.legacyAmpersand().serialize(component) : "";
	}

	@Override
	public void playerListName(Player player, String text) {
		player.playerListName(deserialize(text));
	}

	@Override
	public String displayName(Player player) {
		return serialize(player.displayName());
	}

	@Override
	public String motd() {
		return serialize(Bukkit.getServer().motd());
	}

	@Override
	public void displayName(Objective objective, String dName) {
		objective.displayName(deserialize(dName));
	}

	@SuppressWarnings("deprecation")
	@Override
	public Objective registerNewObjective(Scoreboard board, String name, String criteria, String displayName,
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

			return board.registerNewObjective(name, crit, deserialize(displayName), renderType);
		}

		return board.registerNewObjective(name, criteria, deserialize(displayName), renderType);
	}
}
