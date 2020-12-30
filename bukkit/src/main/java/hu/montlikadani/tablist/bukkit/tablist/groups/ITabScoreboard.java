package hu.montlikadani.tablist.bukkit.tablist.groups;

import org.bukkit.scoreboard.Scoreboard;

import hu.montlikadani.tablist.bukkit.TabListPlayer;

public interface ITabScoreboard {

	TabListPlayer getTabPlayer();

	void registerTeam(String teamName);

	void setTeam(String teamName);

	void unregisterTeam(String teamName);

	Scoreboard getScoreboard();

	void setScoreboard(Scoreboard board);

}
