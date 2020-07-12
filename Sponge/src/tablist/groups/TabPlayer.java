package hu.montlikadani.tablist.sponge.tablist.groups;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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

		Set<TabGroup> groupsList = TabList.get().getGroupsList();
		List<TabGroup> playerNameGroups = groupsList.parallelStream()
				.filter(group -> group.getGroupName().equals(player.getName())).collect(Collectors.toList());
		if (!playerNameGroups.isEmpty()) {
			TabGroup group = playerNameGroups.get(0);
			if (this.group != group) {
				update = true;
				setGroup(group);
			}

			return update;
		}

		for (TabGroup group : groupsList) {
			if (!group.getPermission().isEmpty() && player.hasPermission(group.getPermission())) {
				if (this.group != group) {
					update = true;
					setGroup(group);
				}

				break;
			}
		}

		return update;
	}
}
