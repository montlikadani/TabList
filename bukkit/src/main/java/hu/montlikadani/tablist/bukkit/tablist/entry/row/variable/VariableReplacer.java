package hu.montlikadani.tablist.bukkit.tablist.entry.row.variable;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import hu.montlikadani.tablist.bukkit.API.TabListAPI;
import hu.montlikadani.tablist.bukkit.config.constantsLoader.TabEntryValues;
import hu.montlikadani.tablist.bukkit.config.constantsLoader.TabEntryValues.PlaceholderSetting;
import hu.montlikadani.tablist.bukkit.tablist.entry.TabEntries.Entry;
import hu.montlikadani.tablist.bukkit.tablist.entry.row.IRowPlayer;
import hu.montlikadani.tablist.bukkit.tablist.entry.row.RowPlayer;
import hu.montlikadani.tablist.bukkit.user.TabListUser;
import hu.montlikadani.tablist.bukkit.utils.PluginUtils;

public final class VariableReplacer {

	private String text = "";
	private RowPlayer rowPlayer;
	private boolean updateRequest = true;

	public VariableReplacer(RowPlayer rowPlayer) {
		this.rowPlayer = rowPlayer;
	}

	public boolean isRequestUpdate() {
		return updateRequest;
	}

	public void requestUpdate() {
		updateRequest = true;
	}

	public String replaceVariables(String text) {
		if (!updateRequest) {
			return this.text;
		}

		// TODO maybe separate these into own abstract class for simplest usage, but not
		// for memory

		if (text.contains("%worlds_")) {
			List<World> worlds = Bukkit.getServer().getWorlds();

			for (int i = 0; i < worlds.size(); i++) {
				if (text.indexOf("%worlds_" + i + "%") >= 0) {
					updateRequest = false;
					return this.text = StringUtils.replace(text, "%worlds_" + i + "%", worlds.get(i).getName());
				}
			}
		}

		if (text.contains("%players_online%")) {
			PlaceholderSetting ps = TabEntryValues.VARIABLE_SETTINGS.get(PlaceholderSetting.SettingType.ONLINE_PLAYERS);
			if (ps == null) {
				return this.text = StringUtils.replace(text, "%players_online%", "");
			}

			updateRequest = false;

			List<String> collectedPlayerNames = new ArrayList<>();
			java.util.Set<TabListUser> players = TabListAPI.getPlugin().getUsers();

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

		if (text.contains("%world_players_")) {
			List<World> worlds = Bukkit.getServer().getWorlds();

			for (int w = 0; w < worlds.size(); w++) {
				World world = worlds.get(w);
				String replacement = "%world_players_" + world.getName() + "%";

				if (!StringUtils.contains(text, replacement)) {
					replacement = "%world_players_" + w + "%";
				}

				if (!StringUtils.contains(text, replacement)) {
					continue;
				}

				PlaceholderSetting ps = TabEntryValues.VARIABLE_SETTINGS
						.get(PlaceholderSetting.SettingType.WORLD_PLAYERS);
				if (ps == null) {
					return this.text = StringUtils.replace(text, replacement, "");
				}

				updateRequest = false;

				List<String> collectedPlayerNames = new ArrayList<>();

				for (int i = 0; i < world.getPlayers().size(); i++) {
					if (i >= ps.getMax()) {
						break;
					}

					Player player = world.getPlayers().get(i);
					if (canAddPlayer(player, ps)) {
						collectedPlayerNames.add(player.getName());
					}
				}

				return setText(text, collectedPlayerNames, replacement, true);
			}
		}

		return this.text = text;
	}

	private String setText(String text, List<String> collectedNames, String replacement, boolean isPlayers) {
		if (collectedNames.isEmpty()) {
			return this.text = StringUtils.replace(text, replacement, "");
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
						r.setPlayer(Bukkit.getPlayer(collectedNames.get(i)));
					}
				} else {
					if (i >= collectedNames.size()) {
						break;
					}

					for (TabListUser user : TabListAPI.getPlugin().getUsers()) {
						r.updateText(user.getPlayer(), ((RowPlayer) r).replacer.text = collectedNames.get(i));
					}

					r.setPing(rowPlayer.getPingLatency());
					r.setSkin(rowPlayer.getHeadId());
				}
			}

			i++;
		}

		return this.text = StringUtils.replace(text, replacement, isPlayers ? "" : collectedNames.get(0));
	}

	private boolean canAddPlayer(Player player, PlaceholderSetting ps) {
		return player != null && ((ps.isShowAfkPlayers() && PluginUtils.isAfk(player)) || !PluginUtils.isAfk(player))
				&& !PluginUtils.isVanished(player);
	}
}
