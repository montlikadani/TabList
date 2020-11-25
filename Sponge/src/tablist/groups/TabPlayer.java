package hu.montlikadani.tablist.sponge.tablist.groups;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;

import hu.montlikadani.tablist.sponge.TabList;

public class TabPlayer implements Comparable<TabPlayer> {

	private UUID playerUUID;
	private TabGroup group;

	public TabPlayer(UUID playerUUID) {
		this.playerUUID = playerUUID;
	}

	public UUID getPlayerUUID() {
		return playerUUID;
	}

	public void setGroup(TabGroup group) {
		this.group = group;
	}

	public Optional<TabGroup> getGroup() {
		return Optional.ofNullable(group);
	}

	public boolean update() {
		boolean update = false;

		Optional<Player> p = Sponge.getServer().getPlayer(playerUUID);
		if (!p.isPresent()) {
			return update;
		}

		Set<TabGroup> groupsList = TabList.get().getGroupsList();
		List<TabGroup> playerNameGroups = groupsList.stream()
				.filter(group -> group.getGroupName().equalsIgnoreCase(p.get().getName())).collect(Collectors.toList());
		if (!playerNameGroups.isEmpty()) {
			TabGroup group = playerNameGroups.get(0);
			if (this.group != group) {
				update = true;
				setGroup(group);
			}

			return update;
		}

		for (TabGroup group : groupsList) {
			if (!group.getPermission().isEmpty() && p.get().hasPermission(group.getPermission())) {
				if (this.group != group) {
					update = true;
					setGroup(group);
				}

				break;
			}
		}

		return update;
	}

	@Override
	public int compareTo(TabPlayer tabPlayer) {
		int ownPriority = group != null ? group.getPriority() : 0;
		int tlpPriority = tabPlayer.getGroup().isPresent() ? tabPlayer.getGroup().get().getPriority() : 0;

		Optional<Player> p = Sponge.getServer().getPlayer(playerUUID);
		Optional<Player> p2 = Sponge.getServer().getPlayer(tabPlayer.getPlayerUUID());
		if (ownPriority == tlpPriority && p.isPresent() && p2.isPresent())
			return p.get().getName().compareTo(p2.get().getName());

		return ownPriority - tlpPriority;
	}
}
