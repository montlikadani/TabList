package hu.montlikadani.tablist.sponge.tablist.groups;

import java.util.Optional;

import org.spongepowered.api.entity.living.player.Player;

import hu.montlikadani.tablist.sponge.TabList;

public class TabPlayer {

	private Player player;
	private TabGroup group;

	public TabPlayer(Player player) {
		this.player = player;
	}

	public Player getPlayer() {
		return player;
	}

	public void setGroup(TabGroup group) {
		this.group = group;
	}

	public Optional<TabGroup> getGroup() {
		return Optional.ofNullable(group);
	}

	public boolean update() {
		boolean update = false;
		// player.getSubjectData().getPermissions(player.getActiveContexts()).containsKey(permission)

		for (TabGroup group : TabList.get().getGroupsList()) {
			if (player.hasPermission(player.getActiveContexts(), group.getPermission())) {
				update = true;

				if (this.group == group) {
					break;
				}

				setGroup(group);
			}
		}

		return update;
	}
}
