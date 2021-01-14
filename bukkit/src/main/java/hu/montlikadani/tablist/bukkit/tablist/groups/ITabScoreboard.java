package hu.montlikadani.tablist.bukkit.tablist.groups;

import hu.montlikadani.tablist.bukkit.TabListPlayer;

public interface ITabScoreboard {

	TabListPlayer getTabPlayer();

	void registerTeam(String teamName);

	void setTeam(String teamName);

	void unregisterTeam(String teamName);

}
