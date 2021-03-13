package hu.montlikadani.tablist.bukkit.tablist.entry.row.variable;

import com.google.common.collect.ImmutableSet;

import hu.montlikadani.tablist.bukkit.tablist.entry.row.RowPlayer;
import hu.montlikadani.tablist.bukkit.tablist.entry.row.variable.list.GroupPlayers;
import hu.montlikadani.tablist.bukkit.tablist.entry.row.variable.list.PlayersOnline;
import hu.montlikadani.tablist.bukkit.tablist.entry.row.variable.list.WorldPlayers;
import hu.montlikadani.tablist.bukkit.tablist.entry.row.variable.list.WorldsIndex;

public final class VariableReplacer {

	private String text = "";
	private boolean updateRequest = true;

	private final ImmutableSet<AbstractVariable> variables;

	public VariableReplacer(RowPlayer rowPlayer) {
		variables = ImmutableSet.<AbstractVariable>builder().add(new WorldsIndex(rowPlayer),
				new PlayersOnline(rowPlayer), new WorldPlayers(rowPlayer), new GroupPlayers(rowPlayer)).build();
	}

	public String setAndGetText(String text) {
		return this.text = text;
	}

	public boolean isUpdateRequested() {
		return updateRequest;
	}

	public void requestUpdate() {
		updateRequest = true;
	}

	public String replaceVariables(String text) {
		if (!updateRequest) {
			return this.text;
		}

		for (AbstractVariable variable : variables) {
			String replaced = variable.replace(text);
			if (replaced != null) {
				updateRequest = false;
				return this.text = replaced;
			}
		}

		return this.text = text;
	}
}
