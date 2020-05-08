package hu.montlikadani.tablist.bukkit.commands.list;

import static hu.montlikadani.tablist.bukkit.utils.Util.sendMsg;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import hu.montlikadani.tablist.bukkit.Perm;
import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.commands.ICommand;

public class reload implements ICommand {

	@Override
	public boolean run(TabList plugin, CommandSender sender, Command cmd, String label, String[] args) {
		if (sender instanceof Player && !sender.hasPermission(Perm.RELOAD.getPerm())) {
			sendMsg(sender, plugin.getMsg("no-permission", "%perm%", Perm.RELOAD.getPerm()));
			return false;
		}

		plugin.reload();
		sendMsg(sender, plugin.getMsg("reload-config"));
		return true;
	}
}
