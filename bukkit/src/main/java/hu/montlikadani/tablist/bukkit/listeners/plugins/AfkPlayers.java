package hu.montlikadani.tablist.bukkit.listeners.plugins;

import org.bukkit.entity.Player;

import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.api.TabListAPI;
import hu.montlikadani.tablist.bukkit.config.constantsLoader.ConfigValues;
import hu.montlikadani.tablist.bukkit.utils.Util;

abstract class AfkPlayers {

	private final TabList plugin = TabListAPI.getPlugin();

	protected void goAfk(Player player, boolean value) {
		if (ConfigValues.isAfkStatusEnabled() && !ConfigValues.isAfkStatusShowPlayerGroup()) {
			String prop = value ? ConfigValues.getAfkFormatYes() : ConfigValues.getAfkFormatNo();

			if (!prop.isEmpty()) {
				plugin.getComplement().setPlayerListName(player,
						Util.colorMsg(ConfigValues.isAfkStatusShowInRightLeftSide() ? player.getName() + prop
								: prop + player.getName()));
			}
		}

		if (ConfigValues.isHidePlayerFromTabAfk()) {
			plugin.getUser(player).ifPresent(user -> user.setHidden(value));
		}
	}
}
