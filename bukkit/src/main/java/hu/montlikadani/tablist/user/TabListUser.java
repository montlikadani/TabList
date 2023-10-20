package hu.montlikadani.tablist.user;

import hu.montlikadani.tablist.Objects;
import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.tablist.TabHandler;
import hu.montlikadani.tablist.tablist.groups.GroupPlayer;
import hu.montlikadani.tablist.tablist.playerlist.PlayerList;
import hu.montlikadani.tablist.utils.ServerVersion;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class TabListUser {

	private final TabList plugin;
	private final UUID uniqueId;

	private transient final GroupPlayer groupPlayer;
	private transient final TabHandler tabHandler;
	private transient PlayerScore playerScore;

	private transient PlayerList playerList;

	private boolean tabVisible = true;

	public TabListUser(TabList plugin, UUID uniqueId) {
		this.plugin = plugin;
		this.uniqueId = uniqueId;

		tabHandler = new TabHandler(plugin, this);
		groupPlayer = new GroupPlayer(plugin, this);

		String entry = plugin.getServer().getOfflinePlayer(uniqueId).getName();

		if (entry != null) {
			playerScore = new PlayerScore(entry);
		}
	}

	public Player getPlayer() {
		return plugin.getServer().getPlayer(uniqueId);
	}

	public UUID getUniqueId() {
		return uniqueId;
	}

	public GroupPlayer getGroupPlayer() {
		return groupPlayer;
	}

	public TabHandler getTabHandler() {
		return tabHandler;
	}

	public boolean isHidden() {
		return playerList != null;
	}

	public void setHidden(boolean hidden) {
		if (hidden) {
			if (playerList == null) {
				playerList = new PlayerList(plugin, this);
			}

			playerList.hide();
		} else if (playerList != null) {
			playerList.showEveryone();
			playerList = null;
		}
	}

	public PlayerList getPlayerList() {
		return playerList;
	}

	public PlayerScore getPlayerScore() {
		return playerScore;
	}

	public PlayerScore getPlayerScore(boolean initPlayerScore) {
		if (!initPlayerScore) {
			return playerScore;
		}

		if (hu.montlikadani.tablist.config.constantsLoader.ConfigValues.getObjectType() == Objects.ObjectTypes.NONE) {
			playerScore.setObjectiveCreated();
			playerScore = null;
			return null;
		}

		if (playerScore != null) {
			return playerScore;
		}

		Player player = getPlayer();

		playerScore = new PlayerScore(player == null ? "" : player.getName());
		return playerScore;
	}

	public boolean isTabVisible() {
		return tabVisible;
	}

	public void setTabVisibility(boolean visibility) {
		tabVisible = visibility;
	}

	@Override
	public boolean equals(Object o) {
		return o != null && o.getClass() == getClass() && uniqueId.equals(((TabListUser) o).uniqueId);
	}
}
