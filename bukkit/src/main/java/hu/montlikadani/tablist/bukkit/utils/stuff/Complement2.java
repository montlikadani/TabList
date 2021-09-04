package hu.montlikadani.tablist.bukkit.utils.stuff;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Scoreboard;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class Complement2 implements Complement {

	private Component deserialize(String t) {
		// legacySection is used to deserialize hex colors from plain text too
		return LegacyComponentSerializer.legacySection().deserialize(t);
	}

	private String serialize(Component component) {
		return component != null ? LegacyComponentSerializer.legacyAmpersand().serialize(component) : "";
	}

	@Override
	public void setPlayerListName(Player player, String text) {
		player.playerListName(deserialize(text));
	}

	@Override
	public String getDisplayName(Player player) {
		return serialize(player.displayName());
	}

	@Override
	public String getMotd() {
		return serialize(Bukkit.getServer().motd());
	}

	@Override
	public void setDisplayName(Objective objective, String dName) {
		objective.displayName(deserialize(dName));
	}

	@Override
	public Objective registerNewObjective(Scoreboard board, String name, String criteria, String displayName,
			RenderType renderType) {
		return board.registerNewObjective(name, criteria, deserialize(displayName), renderType);
	}

	@Override
	public String getPlayerListName(Player player) {
		return serialize(player.playerListName());
	}
}
