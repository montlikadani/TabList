package hu.montlikadani.tablist.bukkit.commands.list;

import static hu.montlikadani.tablist.bukkit.utils.Util.sendMsg;

import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import hu.montlikadani.tablist.bukkit.ConfigValues;
import hu.montlikadani.tablist.bukkit.Groups;
import hu.montlikadani.tablist.bukkit.Perm;
import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.TabListPlayer;
import hu.montlikadani.tablist.bukkit.TeamHandler;
import hu.montlikadani.tablist.bukkit.commands.ICommand;
import hu.montlikadani.tablist.bukkit.utils.Util;

public class setsuffix implements ICommand {

	@Override
	public boolean run(TabList plugin, CommandSender sender, Command cmd, String label, String[] args) {
		if (sender instanceof Player && !sender.hasPermission(Perm.SETSUFFIX.getPerm())) {
			sendMsg(sender, plugin.getMsg("no-permission", "%perm%", Perm.SETSUFFIX.getPerm()));
			return false;
		}

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

		StringBuilder builder = new StringBuilder();
		for (int i = 2; i < args.length; i++) {
			builder.append(args[i] + " ");
		}

		String suffix = builder.toString();
		if (suffix.trim().isEmpty()) {
			sendMsg(sender, plugin.getMsg("set-prefix-suffix.suffix.could-not-be-empty"));
			return false;
		}

		String name = args[1];

		plugin.getGS().set("groups." + name + ".suffix", suffix);
		try {
			plugin.getGS().save(plugin.getConf().getGroupsFile());
		} catch (IOException e) {
			e.printStackTrace();
		}

		Groups groups = plugin.getGroups();
		String prefix = plugin.getGS().getString("groups." + name + ".prefix", "");
		int priority = plugin.getGS().getInt("groups." + name + ".sort-priority", 0);

		TeamHandler team = groups.getTeam(name);
		if (team == null) {
			team = new TeamHandler(name, prefix, suffix, priority);
		}

		Player target = Bukkit.getPlayer(name);
		if (target != null) {
			groups.removePlayerGroup(target);

			if (!prefix.isEmpty()) {
				prefix = plugin.getPlaceholders().replaceVariables(target, prefix);
			}
			suffix = plugin.getPlaceholders().replaceVariables(target, suffix);

			TabListPlayer tabPlayer = groups.addPlayer(target);
			tabPlayer.setCustomPrefix(prefix);
			tabPlayer.setCustomSuffix(suffix);
			tabPlayer.setCustomPriority(priority);
			groups.setPlayerTeam(tabPlayer, priority);
		}

		java.util.List<TeamHandler> teams = groups.getGroupsList();
		teams.add(team);

		groups.getGroupsList().clear();
		groups.getGroupsList().addAll(teams);

		sendMsg(sender, plugin.getMsg("set-prefix-suffix.suffix.successfully-set", "%group%", name, "%tag%",
				builder.toString()));
		return true;
	}
}
