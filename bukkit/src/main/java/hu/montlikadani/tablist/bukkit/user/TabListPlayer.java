package hu.montlikadani.tablist.bukkit.user;

import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.tablist.TabHandler;
import hu.montlikadani.tablist.bukkit.tablist.groups.GroupPlayer;
import hu.montlikadani.tablist.bukkit.tablist.playerlist.HidePlayers;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class TabListPlayer implements TabListUser {

	private final UUID uuid;

	private final GroupPlayer groupPlayer;
	private final TabHandler tabHandler;

	private HidePlayers hidePlayers;
	private boolean hidden = false;

	public TabListPlayer(TabList plugin, UUID uuid) {
		this.uuid = uuid;

		tabHandler = new TabHandler(plugin, uuid);
		groupPlayer = new GroupPlayer(plugin, this);
	}

	@Override
	public Player getPlayer() {
		return Bukkit.getPlayer(uuid);
	}

	@Override
	public UUID getUniqueId() {
		return uuid;
	}

	@Override
	public GroupPlayer getGroupPlayer() {
		return groupPlayer;
	}

	@Override
	public TabHandler getTabHandler() {
		return tabHandler;
	}

	@Override
	public boolean isHidden() {
		return hidden;
	}

	@Override
	public void setHidden(boolean hidden) {
		if (this.hidden = hidden) {
			if (hidePlayers == null) {
				hidePlayers = new HidePlayers();
			}

			hidePlayers.removePlayerFromTab(getPlayer());
		} else if (hidePlayers != null) {
			hidePlayers.addPlayerToTab();
			hidePlayers = null;
		}
	}

	public HidePlayers getHidePlayers() {
		return hidePlayers;
	}
}
