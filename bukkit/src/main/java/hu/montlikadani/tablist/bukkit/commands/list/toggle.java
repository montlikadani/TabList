package hu.montlikadani.tablist.bukkit.commands.list;

import static hu.montlikadani.tablist.bukkit.utils.Util.sendMsg;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import hu.montlikadani.tablist.bukkit.Perm;
import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.commands.CommandProcessor;
import hu.montlikadani.tablist.bukkit.commands.ICommand;
import hu.montlikadani.tablist.bukkit.tablist.TabToggleBase;
import hu.montlikadani.tablist.bukkit.user.TabListUser;

@CommandProcessor(
	name = "toggle",
	params = "[player/all]",
	desc = "Toggles on/off the tab for player(s)",
	permission = Perm.TOGGLE)
public final class toggle implements ICommand {

	@Override
	public boolean run(TabList plugin, CommandSender sender, Command cmd, String label, String[] args) {
		if (args.length == 1) {
			if (!(sender instanceof Player)) {
				sendMsg(sender, plugin.getMsg("toggle.console-usage", "%command%", label));
				return false;
			}

			plugin.getUser((Player) sender).ifPresent(user -> sendMsg(user.getPlayer(),
					plugin.getMsg("toggle." + (toggleTab(user) ? "enabled" : "disabled"))));
			return true;
		}

		if (args.length == 2) {
			if (args[1].equalsIgnoreCase("all")) {
				if (sender instanceof Player && !sender.hasPermission(Perm.TOGGLEALL.getPerm())) {
					sendMsg(sender, plugin.getMsg("no-permission", "%perm%", Perm.TOGGLEALL.getPerm()));
					return false;
				}

				if (!(TabToggleBase.globallySwitched = !TabToggleBase.globallySwitched)) {
					for (TabListUser user : plugin.getUsers()) {
						user.getTabHandler().updateTab();
					}
				}

				return true;
			}

			Player player = plugin.getServer().getPlayer(args[1]);
			if (player == null) {
				sendMsg(sender, plugin.getMsg("toggle.player-not-found", "%player%", args[1]));
				return false;
			}

			plugin.getUser(player).ifPresent(
					user -> sendMsg(player, plugin.getMsg("toggle." + (toggleTab(user) ? "enabled" : "disabled"))));
		}

		return true;
	}

	private boolean toggleTab(TabListUser user) {
		if (!TabToggleBase.TAB_TOGGLE.remove(user.getUniqueId())) {
			TabToggleBase.TAB_TOGGLE.add(user.getUniqueId());
			user.getTabHandler().sendEmptyTab(user.getPlayer());
			return false;
		}

		user.getTabHandler().updateTab();
		return true;
	}
}
