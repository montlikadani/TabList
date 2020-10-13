package hu.montlikadani.tablist.bukkit.tablist.fakeplayers;

import org.bukkit.entity.Player;

public interface IFakePlayers {

	String getName();

	int getPingLatency();

	void createFakePlayer(Player p);

	void createFakePlayer(Player p, int pingLatency);

	void createFakePlayer(Player p, String headUUID, int pingLatency);

	void setPing(int pingAmount);

	void setSkin(String skinUUID);

	void removeFakePlayer();
}
