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
		boolean update = true;

		for (TabGroup group : TabList.get().getGroupsList()) {
			if (!group.getPermission().isEmpty()
					&& player.hasPermission(player.getActiveContexts(), group.getPermission())) {
				if (this.group != group) {
					setGroup(group);
				}

				break;
			}
		}

		if (group == null) {
			update = false;
		}

		return update;
	}
}
