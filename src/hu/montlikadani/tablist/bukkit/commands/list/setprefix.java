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

public class setprefix implements ICommand {

	@Override
	public boolean run(TabList plugin, CommandSender sender, Command cmd, String label, String[] args) {
		if (sender instanceof Player && !sender.hasPermission(Perm.SETPREFIX.getPerm())) {
			sendMsg(sender, plugin.getMsg("no-permission", "%perm%", Perm.SETPREFIX.getPerm()));
			return false;
		}

		if (!plugin.getC().getBoolean("change-prefix-suffix-in-tablist.enable")) {
			Util.logConsole(
					"The prefix-suffix is not enabled in the TabList configuration. Without not work this function.");
			return false;
		}

		plugin.getConf().createGroupsFile();

		if (args.length < 3) {
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

		StringBuilder builder = new StringBuilder();
		for (int i = (args.length == 4 ? 3 : 2); i < args.length; i++) {
			builder.append(args[i]);
		}

		String prefix = builder.toString();
		if (prefix.trim().isEmpty()) {
			sendMsg(sender, plugin.getMsg("set-prefix-suffix.prefix.could-not-be-empty"));
			return false;
		}

		String name = args.length > 3 ? args[2] : target.getName();

		plugin.getGS().set("groups." + name + ".prefix", prefix);
		try {
			plugin.getGS().save(plugin.getConf().getGroupsFile());
		} catch (IOException e) {
			e.printStackTrace();
		}

		Groups groups = plugin.getGroups();
		groups.removePlayerGroup(target);

		String suffix = plugin.getGS().getString("groups." + name + ".suffix", "");

		TeamHandler team = groups.getTeam(name);
		if (team == null) {
			int priority = plugin.getGS().getInt("groups." + name + ".sort-priority", 0);
			team = new TeamHandler(name, prefix, suffix, priority);
		}

		prefix = plugin.getPlaceholders().replaceVariables(target, prefix);
		if (!suffix.isEmpty()) {
			suffix = plugin.getPlaceholders().replaceVariables(target, suffix);
		}

		groups.setPlayerTeam(target, prefix, suffix, team.getFullTeamName());

		java.util.List<TeamHandler> teams = groups.getGroupsList();
		teams.add(team);

		groups.getGroupsList().clear();
		groups.getGroupsList().addAll(teams);

		sendMsg(sender, plugin.getMsg("set-prefix-suffix.prefix.successfully-set", "%group%", name, "%tag%",
				builder.toString()));
		return true;
	}
}
