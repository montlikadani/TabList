package hu.montlikadani.tablist.listeners.resources;

import hu.montlikadani.tablist.config.constantsLoader.ConfigValues;

abstract class AfkPlayers {

	protected void goAfk(hu.montlikadani.tablist.TabList tl, org.bukkit.entity.Player player, boolean value) {
		if (ConfigValues.isHidePlayerFromTabAfk()) {
			tl.getUser(player).ifPresent(user -> user.setHidden(value));
			return;
		}

		if (!ConfigValues.isAfkStatusEnabled() || ConfigValues.isPrefixSuffixEnabled()) {
			return;
		}

		String prop = value ? ConfigValues.getAfkFormatYes() : ConfigValues.getAfkFormatNo();

		tl.getComplement().playerListName(player, hu.montlikadani.tablist.utils.Util
				.applyTextFormat(tl.getPlaceholders().replaceVariables(player, ConfigValues.isAfkStatusShowInRightLeftSide()
						? player.getName() + prop : prop + player.getName())));
	}
}
