package hu.montlikadani.tablist.bukkit.commands.list;

import static hu.montlikadani.tablist.bukkit.utils.Util.sendMsg;

import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import hu.montlikadani.tablist.bukkit.Perm;
import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.commands.ICommand;
import hu.montlikadani.tablist.bukkit.utils.Util;

public class removeplayer implements ICommand {

	@Override
	public boolean run(TabList plugin, CommandSender sender, Command cmd, String label, String[] args) {
		if (sender instanceof Player && !sender.hasPermission(Perm.REMOVEPLAYER.getPerm())) {
			sendMsg(sender, plugin.getMsg("no-permission", "%perm%", Perm.REMOVEPLAYER.getPerm()));
			return false;
		}

		if (!plugin.getC().getBoolean("change-prefix-suffix-in-tablist.enable")) {
			Util.logConsole(
					"The prefix-suffix is not enabled in the TabList configuration. Without not work this function.");
			return false;
		}

		if (args.length < 2) {
			sendMsg(sender, plugin.getMsg("set-prefix-suffix.remove-player.usage", "%command%", label));
			return false;
		}

		Player target = Bukkit.getPlayer(args[1]);
		if (target == null) {
			sendMsg(sender, plugin.getMsg("set-prefix-suffix.remove-player.player-not-found", "%target%", args[1]));
			return false;
		}

		if (!plugin.getGS().contains("players." + target.getName())) {
			sendMsg(sender, plugin.getMsg("set-prefix-suffix.remove-player.player-not-found-in-database", "%target%",
					target.getName()));
			return false;
		}

		if (plugin.getChangeType().equals("scoreboard")) {
			Scoreboard b = Bukkit.getScoreboardManager().getNewScoreboard();
			String name = target.getName();

			Team t = null;
			if (plugin.getGS().contains("players." + target.getName() + ".sort-priority")) {
				name = Integer.toString(plugin.getGS().getInt("players." + target.getName() + ".sort-priority"));
				t = b.getTeam(name);
			} else {
				t = b.getTeam(name);
			}

			if (t != null) {
				t.unregister();
				target.setScoreboard(b);
			}
		} else if (plugin.getChangeType().equals("namer")) {
			target.setPlayerListName(target.getName());
		}

		plugin.getGS().set("players." + target.getName(), null);
		try {
			plugin.getGS().save(plugin.getConf().getGroupsFile());
		} catch (IOException e) {
			e.printStackTrace();
		}

		sendMsg(sender,
				plugin.getMsg("set-prefix-suffix.remove-player.successfully-removed", "%target%", target.getName()));
		return true;
	}
}
