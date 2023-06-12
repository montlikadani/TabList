package hu.montlikadani.tablist.listeners.resources;

import org.bukkit.entity.Player;

import hu.montlikadani.tablist.config.constantsLoader.ConfigValues;
import hu.montlikadani.tablist.utils.Util;

public abstract class AfkPlayers {

	protected void goAfk(hu.montlikadani.tablist.TabList tl, Player player, boolean value) {
		if (ConfigValues.isHidePlayerFromTabAfk()) {
			tl.getUser(player).ifPresent(user -> user.setHidden(value));
			return;
		}

		if (ConfigValues.isAfkStatusEnabled() && !ConfigValues.isPrefixSuffixEnabled()) {
			String prop = value ? ConfigValues.getAfkFormatYes() : ConfigValues.getAfkFormatNo();

			tl.getComplement().playerListName(player, Util
					.applyMinimessageFormat(tl.getPlaceholders().replaceVariables(player, ConfigValues.isAfkStatusShowInRightLeftSide() ? player.getName() + prop : prop + player.getName())));
		}
	}
}
