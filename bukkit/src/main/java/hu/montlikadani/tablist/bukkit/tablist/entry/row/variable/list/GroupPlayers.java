package hu.montlikadani.tablist.bukkit.tablist.entry.row.variable.list;

import hu.montlikadani.tablist.bukkit.tablist.entry.row.RowPlayer;
import hu.montlikadani.tablist.bukkit.tablist.entry.row.variable.AbstractVariable;
import hu.montlikadani.tablist.bukkit.user.TabListUser;

public final class GroupPlayers extends AbstractVariable {

	public GroupPlayers(RowPlayer rowPlayer) {
		super(rowPlayer, "%players_in_group_");
	}

	@Override
	public String replace(String text) {
		if (!plugin.hasVault()) {
			return null;
		}

		int index = text.indexOf(replacement);
		if (index < 0) {
			return null;
		}

		// Retrieve group name from placeholder
		String group = text.substring(index + replacement.length());
		group = group.substring(0, group.lastIndexOf('%'));

		java.util.List<String> playerNames = new java.util.ArrayList<>();

		for (String name : plugin.getVaultPerm().getGroups()) {
			if (group.equalsIgnoreCase(name)) {
				for (TabListUser user : plugin.getUsers()) {
					org.bukkit.entity.Player player = user.getPlayer();

					if (plugin.getVaultPerm().playerInGroup(player, name)) {
						playerNames.add(player.getName());
					}
				}

				break;
			}
		}

		return setText(text, playerNames, replacement + group + "%", true);
	}
}
