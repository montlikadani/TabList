package hu.montlikadani.tablist.bukkit.commands.list;

import static hu.montlikadani.tablist.bukkit.utils.Util.sendMsg;

import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import hu.montlikadani.tablist.bukkit.Groups;
import hu.montlikadani.tablist.bukkit.Perm;
import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.TabListPlayer;
import hu.montlikadani.tablist.bukkit.TeamHandler;
import hu.montlikadani.tablist.bukkit.commands.CommandProcessor;
import hu.montlikadani.tablist.bukkit.commands.ICommand;
import hu.montlikadani.tablist.bukkit.config.ConfigValues;
import hu.montlikadani.tablist.bukkit.utils.Util;

@CommandProcessor(name = "setpriority", permission = Perm.SETPRIORITY)
public class setpriority implements ICommand {

	@Override
	public boolean run(TabList plugin, CommandSender sender, Command cmd, String label, String[] args) {
		if (!ConfigValues.isPrefixSuffixEnabled()) {
			Util.logConsole(
					"The prefix-suffix is not enabled in the TabList configuration. Without not work this function.");
			return false;
		}

		plugin.getConf().createGroupsFile();

		if (args.length < 3) {
			if (sender instanceof Player) {
				((Player) sender).performCommand("tl help 3");
			} else {
				Bukkit.dispatchCommand(sender, "tl help");
			}

			return false;
		}

		String match = args[2];
		if (!match.matches("[0-9]+")) {
			sendMsg(sender, plugin.getMsg("set-prefix-suffix.set-priority.priority-must-be-number"));
			return false;
		}

		String name = args[1];
		int priority = Integer.parseInt(match);

		plugin.getConf().getGroups().set("groups." + name + ".sort-priority", priority);
		try {
			plugin.getConf().getGroups().save(plugin.getConf().getGroupsFile());
		} catch (IOException e) {
			e.printStackTrace();
		}

		String prefix = plugin.getConf().getGroups().getString("groups." + name + ".prefix", "");
		String suffix = plugin.getConf().getGroups().getString("groups." + name + ".suffix", "");

		Groups groups = plugin.getGroups();
		TeamHandler team = groups.getTeam(name).orElse(new TeamHandler(name, prefix, suffix));

		team.setPriority(priority);

		Player target = Bukkit.getPlayer(name);
		if (target != null) {
			if (!prefix.isEmpty()) {
				prefix = plugin.getPlaceholders().replaceVariables(target, prefix);
			}
			if (!suffix.isEmpty()) {
				suffix = plugin.getPlaceholders().replaceVariables(target, suffix);
			}

			TabListPlayer tabPlayer = groups.addPlayer(target);
			tabPlayer.setCustomPrefix(prefix);
			tabPlayer.setCustomSuffix(suffix);
			tabPlayer.setCustomPriority(priority);
			groups.setPlayerTeam(tabPlayer, priority);
		}

		groups.getGroupsList().remove(team);
		groups.getGroupsList().add(team);

		sendMsg(sender,
				plugin.getMsg("set-prefix-suffix.set-priority.successfully-set", "%group%", name, "%number%", match));
		return true;
	}
}
