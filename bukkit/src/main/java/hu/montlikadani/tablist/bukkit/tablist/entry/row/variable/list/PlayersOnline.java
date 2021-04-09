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
			return StringUtils.replace(text, replacement, "");
		}

		java.util.List<String> collectedPlayerNames = new java.util.ArrayList<>();

		for (TabListUser user : plugin.getUsers()) {
			if (collectedPlayerNames.size() >= ps.getMax()) {
				break;
			}

			Player player = user.getPlayer();
			if (canAddPlayer(player, ps)) {
				collectedPlayerNames.add(player.getName());
			}
		}

		return setText(text, collectedPlayerNames, replacement, true);
	}
}
