package hu.montlikadani.tablist.bukkit.tablist.entry.row.variable.list;

import org.apache.commons.lang.StringUtils;
import org.bukkit.entity.Player;

import hu.montlikadani.tablist.bukkit.config.constantsLoader.TabEntryValues;
import hu.montlikadani.tablist.bukkit.config.constantsLoader.TabEntryValues.PlaceholderSetting;
import hu.montlikadani.tablist.bukkit.tablist.entry.row.RowPlayer;
import hu.montlikadani.tablist.bukkit.tablist.entry.row.variable.AbstractVariable;
import hu.montlikadani.tablist.bukkit.user.TabListUser;

public final class PlayersOnline extends AbstractVariable {

	public PlayersOnline(RowPlayer rowPlayer) {
		super(rowPlayer, "%players_online%");
	}

	@Override
	public String replace(String text) {
		if (!text.contains(replacement)) {
			return null;
		}

		PlaceholderSetting ps = TabEntryValues.VARIABLE_SETTINGS.get(PlaceholderSetting.SettingType.ONLINE_PLAYERS);
		if (ps == null) {
			return StringUtils.replace(text, "%players_online%", "");
		}

		java.util.List<String> collectedPlayerNames = new java.util.ArrayList<>();
		java.util.Set<TabListUser> players = plugin.getUsers();

		for (int i = 0; i < players.size(); i++) {
			if (i >= ps.getMax()) {
				break;
			}

			Player player = com.google.common.collect.Iterables.<TabListUser>get(players, i).getPlayer();
			if (canAddPlayer(player, ps)) {
				collectedPlayerNames.add(player.getName());
			}
		}

		return setText(text, collectedPlayerNames, "%players_online%", true);
	}
}
