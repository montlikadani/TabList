package hu.montlikadani.tablist.bukkit.commands.list;

import static hu.montlikadani.tablist.bukkit.utils.Util.sendMsg;

import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import hu.montlikadani.tablist.bukkit.Perm;
import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.commands.CommandProcessor;
import hu.montlikadani.tablist.bukkit.commands.ICommand;
import hu.montlikadani.tablist.bukkit.config.ConfigValues;
import hu.montlikadani.tablist.bukkit.utils.Util;

@CommandProcessor(name = "removegroup", permission = Perm.REMOVEGROUP)
public class removegroup implements ICommand {

	@Override
	public boolean run(TabList plugin, CommandSender sender, Command cmd, String label, String[] args) {
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

		String name = args[1];

		if (!plugin.getConf().getGroups().contains("groups." + name)) {
			sendMsg(sender, plugin.getMsg("set-prefix-suffix.remove-group.not-found-in-database", "%group%", name));
			return false;
		}

		plugin.getConf().getGroups().set("groups." + name, null);
		try {
			plugin.getConf().getGroups().save(plugin.getConf().getGroupsFile());
		} catch (IOException e) {
			e.printStackTrace();
		}

		Player target = Bukkit.getPlayer(name);
		if (target != null) {
			plugin.getGroups().removePlayerGroup(target);
		}

		plugin.getGroups().removeGroup(name);

		sendMsg(sender, plugin.getMsg("set-prefix-suffix.remove-group.successfully-removed", "%group%", name));
		return true;
	}
}
