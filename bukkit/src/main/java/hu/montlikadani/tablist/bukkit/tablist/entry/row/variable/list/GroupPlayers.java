package hu.montlikadani.tablist.bukkit.tablist.entry.row.variable.list;

import org.apache.commons.lang.StringUtils;

import hu.montlikadani.tablist.bukkit.config.constantsLoader.TabEntryValues;
import hu.montlikadani.tablist.bukkit.config.constantsLoader.TabEntryValues.PlaceholderSetting;
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

		PlaceholderSetting ps = TabEntryValues.VARIABLE_SETTINGS.get(PlaceholderSetting.SettingType.PLAYERS_IN_GROUP);
		if (ps == null) {
			return StringUtils.replace(text, replacement, "");
		}

		// Retrieve group name from placeholder
		String group = text.substring(index + replacement.length());
		group = group.substring(0, group.lastIndexOf('%'));

		java.util.List<String> playerNames = new java.util.ArrayList<>();

		for (String name : plugin.getVaultPerm().getGroups()) {
			if (group.equalsIgnoreCase(name)) {
				for (TabListUser user : plugin.getUsers()) {
					if (playerNames.size() >= ps.getMax()) {
						break;
					}

					org.bukkit.entity.Player player = user.getPlayer();

					if (plugin.getVaultPerm().playerInGroup(player, name) && canAddPlayer(player, ps)) {
						playerNames.add(player.getName());
					}
				}

				break;
			}
		}

		return setText(text, playerNames, replacement + group + "%", true);
	}
}
