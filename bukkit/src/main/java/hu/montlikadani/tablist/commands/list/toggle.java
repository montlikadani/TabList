package hu.montlikadani.tablist.commands.list;

import static hu.montlikadani.tablist.utils.Util.sendMsg;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import hu.montlikadani.tablist.Perm;
import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.commands.CommandProcessor;
import hu.montlikadani.tablist.commands.ICommand;
import hu.montlikadani.tablist.config.ConfigMessages;
import hu.montlikadani.tablist.config.constantsLoader.TabConfigValues;
import hu.montlikadani.tablist.tablist.TabToggleBase;
import hu.montlikadani.tablist.user.TabListUser;

@CommandProcessor(
	name = "toggle",
	params = "[player/all]",
	desc = "Toggles the tab visibility for player(s)",
	permission = Perm.TOGGLE)
public final class toggle implements ICommand {

	@Override
	public boolean run(TabList plugin, CommandSender sender, Command cmd, String label, String[] args) {
		if (args.length == 1) {
			if (!(sender instanceof Player)) {
				sendMsg(sender, ConfigMessages.get(ConfigMessages.MessageKeys.TOGGLE_CONSOLE_USAGE, "%command%", label));
				return false;
			}

			Player player = (Player) sender;

			plugin.getUser(player).ifPresent(
					user -> sendMsg(player, ConfigMessages.get(toggleTab(user, player) ? ConfigMessages.MessageKeys.TOGGLE_ENABLED
							: ConfigMessages.MessageKeys.TOGGLE_DISABLED)));
			return true;
		}

		if (args.length == 2) {
			String first = args[1];

			if (first.equalsIgnoreCase("all")) {
				if (sender instanceof Player && !sender.hasPermission(Perm.TOGGLEALL.permission)) {
					sendMsg(sender,
							ConfigMessages.get(ConfigMessages.MessageKeys.NO_PERMISSION, "%perm%", Perm.TOGGLEALL.permission));
					return false;
				}

				if (!(TabToggleBase.globallySwitched = !TabToggleBase.globallySwitched)) {
					for (TabListUser user : plugin.getUsers()) {
						if (TabConfigValues.isEnabled()) {
							user.getTabHandler().loadTabComponents();
						} else {
							user.getTabHandler().sendEmptyTab(user.getPlayer());
						}
					}
				}

				return true;
			}

			Player player = plugin.getServer().getPlayer(first);
			if (player == null) {
				sendMsg(sender, ConfigMessages.get(ConfigMessages.MessageKeys.TOGGLE_PLAYER_NOT_FOUND, "%player%", first));
				return false;
			}

			plugin.getUser(player).ifPresent(
					user -> sendMsg(player, ConfigMessages.get(toggleTab(user, player) ? ConfigMessages.MessageKeys.TOGGLE_ENABLED
							: ConfigMessages.MessageKeys.TOGGLE_DISABLED)));
		}

		return true;
	}

	private boolean toggleTab(TabListUser user, Player player) {
		if (!TabToggleBase.TAB_TOGGLE.remove(user.getUniqueId())) {
			TabToggleBase.TAB_TOGGLE.add(user.getUniqueId());
			user.getTabHandler().sendEmptyTab(player);
			return false;
		}

		if (TabConfigValues.isEnabled()) {
			user.getTabHandler().loadTabComponents();
		}

		return true;
	}
}
