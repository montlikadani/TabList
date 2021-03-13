package hu.montlikadani.tablist.bukkit.tablist.entry.row.variable;

import org.apache.commons.lang.StringUtils;
import org.bukkit.entity.Player;

import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.API.TabListAPI;
import hu.montlikadani.tablist.bukkit.config.constantsLoader.TabEntryValues.PlaceholderSetting;
import hu.montlikadani.tablist.bukkit.tablist.entry.TabEntries.Entry;
import hu.montlikadani.tablist.bukkit.tablist.entry.row.IRowPlayer;
import hu.montlikadani.tablist.bukkit.tablist.entry.row.RowPlayer;
import hu.montlikadani.tablist.bukkit.user.TabListUser;
import hu.montlikadani.tablist.bukkit.utils.PluginUtils;

public abstract class AbstractVariable {

	protected final TabList plugin = TabListAPI.getPlugin();

	protected final RowPlayer rowPlayer;
	protected final String replacement;

	public AbstractVariable(RowPlayer rowPlayer, String replacement) {
		this.rowPlayer = rowPlayer;
		this.replacement = replacement;
	}

	public String getReplacement() {
		return replacement;
	}

	protected boolean canAddPlayer(Player player, PlaceholderSetting ps) {
		return player != null && ((ps.isShowAfkPlayers() && PluginUtils.isAfk(player)) || !PluginUtils.isAfk(player))
				&& !PluginUtils.isVanished(player);
	}

	protected String setText(String text, java.util.List<String> collectedNames, String replacement,
			boolean isPlayers) {
		if (collectedNames.isEmpty()) {
			return StringUtils.replace(text, replacement, "");
		}

		Entry[][] entries = rowPlayer.root.getEntries();

		boolean firstEqual = false;
		int i = 0;
		for (int rowIndex = 0; rowIndex < 20; rowIndex++) {
			Entry entry = entries[rowPlayer.columnIndex][rowIndex];
			if (entry != null) {
				IRowPlayer r = entry.getRow();

				if (isPlayers) {
					if (!firstEqual && !(firstEqual = entry.getRow().getText().equals(rowPlayer.getText()))) {
						continue; // Dirty solution, to check the last row text is equal to the current row
					}

					if (i < collectedNames.size()) {
						r.setPlayer(org.bukkit.Bukkit.getPlayer(collectedNames.get(i)));
					}
				} else {
					if (i >= collectedNames.size()) {
						break;
					}

					for (TabListUser user : plugin.getUsers()) {
						r.updateText(user.getPlayer(), ((RowPlayer) r).replacer.setAndGetText(collectedNames.get(i)));
					}

					r.setPing(rowPlayer.getPingLatency());
					r.setSkin(rowPlayer.getHeadId());
				}
			}

			i++;
		}

		return StringUtils.replace(text, replacement, isPlayers ? "" : collectedNames.get(0));
	}

	public abstract String replace(String text);

}
