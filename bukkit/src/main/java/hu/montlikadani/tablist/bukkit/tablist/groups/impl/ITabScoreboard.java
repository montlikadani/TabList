package hu.montlikadani.tablist.bukkit.tablist.groups.impl;

import hu.montlikadani.tablist.bukkit.tablist.groups.GroupPlayer;

public interface ITabScoreboard {

	void registerTeam(GroupPlayer groupPlayer);

	void setTeam(GroupPlayer groupPlayer);

	void unregisterTeam(GroupPlayer groupPlayer);

}
