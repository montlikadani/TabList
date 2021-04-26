package hu.montlikadani.tablist.bukkit.user;

import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.tablist.TabHandler;
import hu.montlikadani.tablist.bukkit.tablist.groups.GroupPlayer;
import hu.montlikadani.tablist.bukkit.tablist.playerlist.HidePlayers;
import hu.montlikadani.tablist.bukkit.tablist.playerlist.PlayerList;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class TabListPlayer implements TabListUser {

	private final TabList plugin;
	private final UUID uuid;

	private final GroupPlayer groupPlayer;
	private final TabHandler tabHandler;

	private HidePlayers hidePlayers;
	private PlayerList playerList;
	private boolean hidden = false;

	public TabListPlayer(TabList plugin, UUID uuid) {
		this.plugin = plugin;
		this.uuid = uuid;

		tabHandler = new TabHandler(plugin, uuid);
		groupPlayer = new GroupPlayer(plugin, this, plugin.getGroups());
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
			if (playerList == null) {
				playerList = new PlayerList(plugin, this);
			}

			playerList.hide();
		} else if (playerList != null) {
			playerList.show();
			playerList = null;
		}
	}

	@Override
	public boolean isRemovedFromPlayerList() {
		return hidePlayers != null;
	}

	@Override
	public void removeFromPlayerList() {
		if (hidePlayers == null) {
			hidePlayers = new HidePlayers(getPlayer());
			hidePlayers.removePlayerFromTab();
		}
	}

	@Override
	public void addToPlayerList() {
		if (hidePlayers != null) {
			hidePlayers.addPlayerToTab();
			hidePlayers = null;
		}
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof TabListPlayer && uuid.equals(((TabListPlayer) o).uuid);
	}

	public final void remove() {
		playerList = null;
	}

	public HidePlayers getHidePlayers() {
		return hidePlayers;
	}

	public PlayerList getPlayerList() {
		return playerList;
	}
}
