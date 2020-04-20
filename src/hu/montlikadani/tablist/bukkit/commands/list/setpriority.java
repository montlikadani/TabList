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
import hu.montlikadani.tablist.bukkit.TeamHandler;
import hu.montlikadani.tablist.bukkit.commands.ICommand;
import hu.montlikadani.tablist.bukkit.utils.Util;

public class setpriority implements ICommand {

	@Override
	public boolean run(TabList plugin, CommandSender sender, Command cmd, String label, String[] args) {
		if (sender instanceof Player && !sender.hasPermission(Perm.SETPRIORITY.getPerm())) {
			sendMsg(sender, plugin.getMsg("no-permission", "%perm%", Perm.SETPRIORITY.getPerm()));
			return false;
		}

		if (!plugin.getC().getBoolean("change-prefix-suffix-in-tablist.enable")) {
			Util.logConsole(
					"The prefix-suffix is not enabled in the TabList configuration. Without not work this function.");
			return false;
		}

		plugin.getConf().createGroupsFile();

		if (args.length < 3) {
			sendMsg(sender, plugin.getMsg("set-prefix-suffix.set-priority.usage", "%command%", label));
			return false;
		}

		Player target = Bukkit.getPlayer(args[1]);
		if (target == null) {
			sendMsg(sender, plugin.getMsg("set-prefix-suffix.player-not-found", "%target%", args[1]));
			return false;
		}

		String match = args[args.length == 2 ? 3 : 2];
		if (!match.matches("[0-9]+")) {
			sendMsg(sender, plugin.getMsg("set-prefix-suffix.set-priority.priority-must-be-number"));
			return false;
		}

		String name = args.length > 2 ? args[2] : target.getName();
		int priority = Integer.parseInt(match);

		plugin.getGS().set("groups." + name + ".sort-priority", priority);
		try {
			plugin.getGS().save(plugin.getConf().getGroupsFile());
		} catch (IOException e) {
			e.printStackTrace();
		}

		String prefix = plugin.getGS().getString("groups." + name + ".prefix", "");
		String suffix = plugin.getGS().getString("groups." + name + ".suffix", "");

		Groups groups = plugin.getGroups();

		TeamHandler team = groups.getTeam(name);
		if (team == null) {
			team = new TeamHandler(name, prefix, suffix);
		}

		team.setPriority(priority);

		if (!prefix.isEmpty()) {
			prefix = plugin.getPlaceholders().replaceVariables(target, prefix);
		}
		if (!suffix.isEmpty()) {
			suffix = plugin.getPlaceholders().replaceVariables(target, suffix);
		}

		groups.setPlayerTeam(target, prefix, suffix, team.getFullTeamName());

		if (groups.getGroupsList().contains(team)) {
			groups.getGroupsList().remove(team);
		}

		groups.getGroupsList().add(team);

		sendMsg(sender,
				plugin.getMsg("set-prefix-suffix.set-priority.successfully-set", "%group%", name, "%number%", args[2]));
		return true;
	}
}
