package hu.montlikadani.tablist.tablist.groups;

import java.util.Optional;

import org.spongepowered.api.entity.living.player.Player;

import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.user.TabListUser;

public class TabGroupPlayer {

	private final TabListUser user;
	private final TabList tl;

	private TabGroup group;

	public TabGroupPlayer(TabList tl, TabListUser user) {
		this.tl = tl;
		this.user = user;
	}

	public TabListUser getUser() {
		return user;
	}

	public void setGroup(TabGroup group) {
		this.group = group;
	}

	public Optional<TabGroup> getGroup() {
		return Optional.ofNullable(group);
	}

	public boolean update() {
		Optional<Player> opt = user.getPlayer();

		if (!opt.isPresent()) {
			return false;
		}

		Player player = opt.get();
		boolean update = false;

		for (TabGroup tabGroup : tl.getGroupsList()) {
			if (tabGroup.getGroupName().equalsIgnoreCase(player.getName())) {
				if (tabGroup != group) {
					update = true;
					setGroup(tabGroup);
				}

				return update;
			}

			if (tabGroup != group && !tabGroup.getPermission().isEmpty() && player.hasPermission(tabGroup.getPermission())) {
				update = true;
				setGroup(tabGroup);
				break;
			}
		}

		return update;
	}
}
