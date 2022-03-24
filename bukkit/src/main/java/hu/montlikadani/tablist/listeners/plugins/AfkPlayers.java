package hu.montlikadani.tablist.listeners.plugins;

import org.bukkit.entity.Player;

import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.api.TabListAPI;
import hu.montlikadani.tablist.config.constantsLoader.ConfigValues;
import hu.montlikadani.tablist.utils.Util;

abstract class AfkPlayers {

	private final TabList plugin = TabListAPI.getPlugin();

	protected void goAfk(Player player, boolean value) {
		if (ConfigValues.isHidePlayerFromTabAfk()) {
			plugin.getUser(player).ifPresent(user -> user.setHidden(value));
		} else if (ConfigValues.isAfkStatusEnabled() && !ConfigValues.isPrefixSuffixEnabled()) {
			String prop = value ? ConfigValues.getAfkFormatYes() : ConfigValues.getAfkFormatNo();

			plugin.getComplement().setPlayerListName(player, Util.colorText(
					ConfigValues.isAfkStatusShowInRightLeftSide() ? player.getName() + prop : prop + player.getName()));
		}
	}
}
