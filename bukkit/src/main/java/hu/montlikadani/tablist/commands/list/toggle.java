package hu.montlikadani.tablist.commands.list;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import hu.montlikadani.tablist.Perm;
import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.commands.CommandProcessor;
import hu.montlikadani.tablist.commands.ICommand;
import hu.montlikadani.tablist.config.ConfigMessages;
import hu.montlikadani.tablist.tablist.TabToggleBase;
import hu.montlikadani.tablist.user.TabListUser;

@CommandProcessor(name = "toggle", params = "[player/all]", desc = "Toggles the tab visibility for player(s)", permission = Perm.TOGGLE)
public final class toggle implements ICommand {

	@Override
	public boolean run(TabList plugin, CommandSender sender, Command cmd, String label, String[] args) {
		if (args.length == 1) {
			if (!(sender instanceof Player)) {
				plugin.getComplement().sendMessage(sender, ConfigMessages.get(ConfigMessages.MessageKeys.TOGGLE_CONSOLE_USAGE, "%command%", label));
				return false;
			}

			Player player = (Player) sender;

			plugin.getUser(player).ifPresent(
					user -> plugin.getComplement().sendMessage(player, ConfigMessages.get(toggleTab(user, player) ? ConfigMessages.MessageKeys.TOGGLE_ENABLED
							: ConfigMessages.MessageKeys.TOGGLE_DISABLED)));
			return true;
		}

		if (args.length == 2) {
			String first = args[1];

			if (first.equalsIgnoreCase("all")) {
				if (sender instanceof Player && !sender.hasPermission(Perm.TOGGLEALL.permission)) {
					plugin.getComplement().sendMessage(sender, ConfigMessages.get(ConfigMessages.MessageKeys.NO_PERMISSION, "%perm%", Perm.TOGGLEALL.permission));
					return false;
				}

				if (!(TabToggleBase.globallySwitched = !TabToggleBase.globallySwitched)) {
					for (TabListUser user : plugin.getUsers()) {
						user.getTabHandler().loadTabComponents();
					}
				} else {
					TabToggleBase.TEMPORAL_PLAYER_CACHE.clear();
				}

				return true;
			}

			Player player = plugin.getServer().getPlayer(first);
			if (player == null) {
				plugin.getComplement().sendMessage(sender, ConfigMessages.get(ConfigMessages.MessageKeys.TOGGLE_PLAYER_NOT_FOUND, "%player%", first));
				return false;
			}

			plugin.getUser(player).ifPresent(
					user -> plugin.getComplement().sendMessage(player, ConfigMessages.get(toggleTab(user, player) ? ConfigMessages.MessageKeys.TOGGLE_ENABLED
							: ConfigMessages.MessageKeys.TOGGLE_DISABLED)));
		}

		return true;
	}

	private boolean toggleTab(TabListUser user, Player player) {
		if (user.isTabVisible()) {
			user.setTabVisibility(false);
			TabToggleBase.TEMPORAL_PLAYER_CACHE.add(user.getUniqueId());
			user.getTabHandler().sendEmptyTab(player);
			return false;
		}

		user.setTabVisibility(true);
		TabToggleBase.TEMPORAL_PLAYER_CACHE.remove(user.getUniqueId());
		user.getTabHandler().loadTabComponents();
		return true;
	}
}
