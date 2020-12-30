package hu.montlikadani.tablist.tablist.groups;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.player.TabPlayer;

public class TabUser extends TabPlayer implements Comparable<TabUser> {

	private TabGroup group;

	public TabUser(UUID playerUUID) {
		super(playerUUID);
	}

	@Override
	public void setGroup(TabGroup group) {
		this.group = group;
	}

	@Override
	public Optional<TabGroup> getGroup() {
		return Optional.ofNullable(group);
	}

	@Override
	public boolean updateGroup() {
		boolean update = false;

		Optional<ServerPlayer> player = asServerPlayer();
		if (!player.isPresent()) {
			return update;
		}

		Set<TabGroup> groupsList = TabList.get().getGroupsList();
		List<TabGroup> playerNameGroups = groupsList.stream()
				.filter(group -> group.getGroupName().equalsIgnoreCase(player.get().getName()))
				.collect(Collectors.toList());
		if (!playerNameGroups.isEmpty()) {
			TabGroup group = playerNameGroups.get(0);
			if (this.group != group) {
				update = true;
				setGroup(group);
			}

			return update;
		}

		for (TabGroup group : groupsList) {
			if (!group.getPermission().isEmpty() && player.get().hasPermission(group.getPermission())) {
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
	public int compareTo(TabUser tabUser) {
		int ownPriority = group != null ? group.getPriority() : 0;
		int tlpPriority = tabUser.getGroup().isPresent() ? tabUser.getGroup().get().getPriority() : 0;

		Optional<ServerPlayer> player = Sponge.getServer().getPlayer(getPlayerUUID());
		Optional<ServerPlayer> player2 = Sponge.getServer().getPlayer(tabUser.getPlayerUUID());
		if (ownPriority == tlpPriority && player.isPresent() && player2.isPresent()) {
			return player.get().getName().compareTo(player2.get().getName());
		}

		return ownPriority - tlpPriority;
	}
}
