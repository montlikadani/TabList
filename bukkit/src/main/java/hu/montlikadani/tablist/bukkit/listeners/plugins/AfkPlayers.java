package hu.montlikadani.tablist.bukkit.listeners.plugins;

import static hu.montlikadani.tablist.bukkit.utils.Util.colorMsg;

import org.bukkit.entity.Player;

import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.API.TabListAPI;
import hu.montlikadani.tablist.bukkit.config.constantsLoader.ConfigValues;

public abstract class AfkPlayers {

	private final TabList plugin = TabListAPI.getPlugin();

	protected void goAfk(Player player, boolean value) {
		if (ConfigValues.isAfkStatusEnabled() && !ConfigValues.isAfkStatusShowPlayerGroup()) {
			String path = "placeholder-format.afk-status.format-" + (value ? "yes" : "no");
			String result = "";

			if (plugin.getConfig().contains(path)) {
				result = colorMsg(ConfigValues.isAfkStatusShowInRightLeftSide()
						? player.getName() + plugin.getConfig().getString(path)
						: plugin.getConfig().getString(path) + player.getName());
			}

			if (!result.isEmpty()) {
				plugin.getComplement().setPlayerListName(player, result);
			}
		}

		if (!ConfigValues.isHidePlayerFromTabAfk()) {
			return;
		}

		plugin.getUser(player).ifPresent(user -> user.setHidden(value));
	}
}
