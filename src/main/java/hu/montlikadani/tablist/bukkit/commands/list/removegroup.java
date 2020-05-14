package hu.montlikadani.tablist.bukkit.commands.list;

import static hu.montlikadani.tablist.bukkit.utils.Util.sendMsg;

import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import hu.montlikadani.tablist.bukkit.ConfigValues;
import hu.montlikadani.tablist.bukkit.Perm;
import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.commands.ICommand;
import hu.montlikadani.tablist.bukkit.utils.Util;

public class removegroup implements ICommand {

	@Override
	public boolean run(TabList plugin, CommandSender sender, Command cmd, String label, String[] args) {
		if (sender instanceof Player && !sender.hasPermission(Perm.REMOVEGROUP.getPerm())) {
			sendMsg(sender, plugin.getMsg("no-permission", "%perm%", Perm.REMOVEGROUP.getPerm()));
			return false;
		}

		if (!ConfigValues.isPrefixSuffixEnabled()) {
			Util.logConsole(
					"The prefix-suffix is not enabled in the TabList configuration. Without not work this function.");
			return false;
		}

		plugin.getConf().createGroupsFile();

		if (args.length < 2) {
			if (sender instanceof Player) {
				((Player) sender).performCommand("tl help 2");
			} else {
				Bukkit.dispatchCommand(sender, "tl help");
			}

			return false;
		}

		Player target = Bukkit.getPlayer(args[1]);
		if (target == null) {
			sendMsg(sender, plugin.getMsg("set-prefix-suffix.player-not-found", "%target%", args[1]));
			return false;
		}

		String name = args.length > 2 ? args[2] : target.getName();

		if (!plugin.getGS().contains("groups." + name)) {
			sendMsg(sender, plugin.getMsg("set-prefix-suffix.remove-group.not-found-in-database", "%group%", name));
			return false;
		}

		plugin.getGS().set("groups." + name, null);
		try {
			plugin.getGS().save(plugin.getConf().getGroupsFile());
		} catch (IOException e) {
			e.printStackTrace();
		}

		plugin.getGroups().removePlayerGroup(target);
		plugin.getGroups().removeGroup(name);

		sendMsg(sender, plugin.getMsg("set-prefix-suffix.remove-group.successfully-removed", "%group%", name));
		return true;
	}
}
