package hu.montlikadani.tablist.bukkit.tablist.groups.impl;

import hu.montlikadani.tablist.bukkit.user.TabListUser;

public interface ITabScoreboard {

	TabListUser getTabListUser();

	void registerTeam(String teamName);

	void setTeam(String teamName);

	void unregisterTeam(String teamName);

}
