package hu.montlikadani.tablist.utils.stuff;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Scoreboard;

public interface Complement {

	void playerListName(Player player, String text);

	String displayName(Player player);

	String motd();

	void displayName(Objective objective, String dName);

	Objective registerNewObjective(Scoreboard board, String name, String criteria, String displayName, RenderType renderType);

}
