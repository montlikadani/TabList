package hu.montlikadani.tablist.bukkit.utils.stuff;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Scoreboard;

public interface Complement {

	void setPlayerListName(Player player, String text);

	String getPlayerListName(Player player);

	String getDisplayName(Player player);

	String getMotd();

	void setDisplayName(Objective objective, String dName);

	Objective registerNewObjective(Scoreboard board, String name, String criteria, String displayName, RenderType renderType);

}
