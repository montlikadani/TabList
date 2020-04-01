package hu.montlikadani.tablist.bukkit.commands.list;

import static hu.montlikadani.tablist.bukkit.utils.Util.sendMsg;

import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import hu.montlikadani.tablist.Global;
import hu.montlikadani.tablist.bukkit.Perm;
import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.commands.ICommand;
import hu.montlikadani.tablist.bukkit.utils.Util;
import hu.montlikadani.tablist.bukkit.utils.ServerVersion.Version;

public class setsuffix implements ICommand {

	@SuppressWarnings("deprecation")
	@Override
	public boolean run(TabList plugin, CommandSender sender, Command cmd, String label, String[] args) {
		if (sender instanceof Player && !sender.hasPermission(Perm.SETSUFFIX.getPerm())) {
			sendMsg(sender, plugin.getMsg("no-permission", "%perm%", Perm.SETSUFFIX.getPerm()));
			return false;
		}

		if (!plugin.getC().getBoolean("change-prefix-suffix-in-tablist.enable")) {
			Util.logConsole(
					"The prefix-suffix is not enabled in the TabList configuration. Without not work this function.");
			return false;
		}

		if (args.length < 2) {
			sendMsg(sender, plugin.getMsg("set-prefix-suffix.suffix.usage", "%command%", label));
			return false;
		}

		plugin.getConf().createGroupsFile();

		Player target = Bukkit.getPlayer(args[1]);
		if (target == null) {
			sendMsg(sender, plugin.getMsg("set-prefix-suffix.suffix.player-not-found", "%target%", args[1]));
			return false;
		}

		StringBuilder build = new StringBuilder();
		for (int i = 2; i < args.length; i++) {
			build.append(args[i] + " ");
		}

		String suf = build.toString();
		if (suf.isEmpty()) {
			sendMsg(sender, plugin.getMsg("set-prefix-suffix.suffix.could-not-be-empty"));
			return false;
		}

		plugin.getGS().set("players." + target.getName() + ".suffix", suf);
		try {
			plugin.getGS().save(plugin.getConf().getGroupsFile());
		} catch (IOException e) {
			e.printStackTrace();
		}

		suf = plugin.getPlaceholders().setPlaceholders(target, suf);
		suf = Global.setSymbols(suf);
		suf = Util.colorMsg(suf);

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

			if (t == null) {
				t = b.registerNewTeam(name);
			}

			if (Version.isCurrentLower(Version.v1_9_R1)) {
				if (!t.hasPlayer(target)) {
					t.addPlayer(target);
				}
			} else if (!t.hasEntry(target.getName())) {
				t.addEntry(target.getName());
			}

			suf = Util.splitStringByVersion(suf);

			if (plugin.getGS().contains("players." + target.getName() + ".prefix")) {
				String prefix = plugin.getGS().getString("players." + target.getName() + ".prefix");
				prefix = plugin.getPlaceholders().setPlaceholders(target, prefix);
				prefix = Global.setSymbols(prefix);
				prefix = Util.colorMsg(prefix);

				t.setPrefix(prefix);
			}

			t.setSuffix(suf);
			target.setScoreboard(b);
		} else if (plugin.getChangeType().equals("namer")) {
			target.setPlayerListName(Util.colorMsg(target.getName() + suf));
		}

		sendMsg(sender,
				plugin.getMsg("set-prefix-suffix.suffix.successfully-set", "%tag%", suf, "%target%", target.getName()));
		return true;
	}
}
