package hu.montlikadani.tablist.bukkit.tablist.entry.row.variable.list;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.bukkit.World;

import hu.montlikadani.tablist.bukkit.tablist.entry.row.RowPlayer;
import hu.montlikadani.tablist.bukkit.tablist.entry.row.variable.AbstractVariable;

public final class WorldsIndex extends AbstractVariable {

	public WorldsIndex(RowPlayer rowPlayer) {
		super(rowPlayer, "%worlds_");
	}

	@Override
	public String replace(String text) {
		if (!text.contains(replacement)) {
			return null;
		}

		List<World> worlds = plugin.getServer().getWorlds();

		for (int i = 0; i < worlds.size(); i++) {
			if (text.indexOf(replacement + i + "%") >= 0) {
				return StringUtils.replace(text, replacement + i + "%", worlds.get(i).getName());
			}
		}

		return null;
	}
}
