package hu.montlikadani.tablist.bukkit.tablist.fakeplayers;

import org.bukkit.entity.Player;

public interface IFakePlayers {

	String getName();

	void createFakeplayer(Player p);

	void removeFakePlayer();
}
