package hu.montlikadani.tablist.bukkit.commands;

import static hu.montlikadani.tablist.bukkit.utils.Util.sendMsg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import hu.montlikadani.tablist.bukkit.ConfigValues;
import hu.montlikadani.tablist.bukkit.Perm;
import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.utils.Util;

public class TabNameCmd implements CommandExecutor, TabCompleter {

	private TabList plugin;

	public TabNameCmd(TabList plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (sender instanceof Player && !sender.hasPermission(Perm.TABNAME.getPerm())) {
			sendMsg(sender, plugin.getMsg("no-permission", "%perm%", Perm.TABNAME.getPerm()));
			return true;
		}

		if (!ConfigValues.isTabNameEnabled()) {
			Util.logConsole(java.util.logging.Level.WARNING, "Tabname option is not enabled in configuration.");
			return true;
		}

		plugin.getConf().createNamesFile();

		final int maxLength = ConfigValues.getTabNameMaxLength();

		if (!(sender instanceof Player)) {
			if (args.length == 0) {
				sendMsg(sender, plugin.getMsg("tabname.console-usage", "%command%", label));
				return true;
			}

			if (args[0].equalsIgnoreCase("reset")) {
				if (args.length == 1) {
					sendMsg(sender, plugin.getMsg("tabname.reset.console-usage", "%command%", label));
					return true;
				}

				Player target = Bukkit.getPlayer(args[1]);
				if (target == null) {
					sendMsg(sender, plugin.getMsg("tabname.player-not-online", "%player%", args[1]));
					return true;
				}

				if (!plugin.getConf().getNames().contains("players." + target.getName() + ".tabname")) {
					sendMsg(sender, plugin.getMsg("tabname.reset.no-reset-target", "%player%", target.getName()));
					return true;
				}

				plugin.getTabNameHandler().unSetTabName(target);
				sendMsg(sender, plugin.getMsg("tabname.reset.target-name-reseted", "%target%", target.getName()));
				return true;
			}

			Player target = Bukkit.getPlayer(args[0]);
			if (target == null) {
				sendMsg(sender, plugin.getMsg("tabname.player-not-online", "%player%", args[0]));
				return true;
			}

			if (args.length == 1) {
				sendMsg(sender, plugin.getMsg("tabname.console-usage", "%command%", label));
				return true;
			}

			if (plugin.getC().getStringList("tabname.disabled-worlds").contains(target.getWorld().getName())) {
				sendMsg(sender, plugin.getMsg("tabname.world-disabled", "%world%", target.getWorld().getName()));
				return true;
			}

			StringBuilder builder = new StringBuilder();
			for (int i = 1; i < args.length; i++) {
				builder.append(args[i] + " ");
			}

			String msg = builder.toString();
			if (isNameDisabled(msg)) {
				sendMsg(sender, plugin.getMsg("tabname.name-disabled", "%name%", msg));
				return true;
			}

			if (msg.length() > maxLength) {
				sendMsg(sender, plugin.getMsg("tabname.name-too-long", "%max%", maxLength));
				return true;
			}

			plugin.getTabNameHandler().setTabName(target, msg);
			sendMsg(target,
					plugin.getMsg("tabname.target-changed-name-message", "%name%", msg, "%sender%", sender.getName()));
			sendMsg(sender,
					plugin.getMsg("tabname.target-name-change-success", "%target%", target.getName(), "%name%", msg));

			return true;
		}

		Player p = (Player) sender;
		if (args.length == 0) {
			if (sender instanceof Player) {
				((Player) sender).performCommand("tl help 2");
			} else {
				Bukkit.dispatchCommand(sender, "tl help");
			}

			return true;
		}

		if (args[0].equalsIgnoreCase("reset")) {
			if (!p.hasPermission(Perm.RESET.getPerm())) {
				sendMsg(p, plugin.getMsg("no-permission", "%perm%", Perm.RESET.getPerm()));
				return true;
			}

			if (args.length == 1) {
				if (!plugin.getConf().getNames().contains("players." + p.getName() + ".tabname")) {
					sendMsg(p, plugin.getMsg("tabname.reset.no-reset"));
					return true;
				}

				plugin.getTabNameHandler().unSetTabName(p);
				sendMsg(p, plugin.getMsg("tabname.reset.name-reseted"));
			} else {
				if (!p.hasPermission(Perm.RESETOTHERTAB.getPerm())) {
					sendMsg(p, plugin.getMsg("no-permission", "%perm%", Perm.RESETOTHERTAB.getPerm()));
					return true;
				}

				Player target = Bukkit.getPlayer(args[1]);
				if (target == null) {
					sendMsg(p, plugin.getMsg("tabname.player-not-online", "%player%", args[1]));
					return true;
				}

				if (!plugin.getConf().getNames().contains("players." + target.getName() + ".tabname")) {
					sendMsg(p, plugin.getMsg("tabname.reset.no-reset-target", "%player%", target.getName()));
					return true;
				}

				plugin.getTabNameHandler().unSetTabName(target);
				sendMsg(p, plugin.getMsg("tabname.reset.target-name-reseted", "%target%", target.getName()));
			}

			return true;
		}

		if (args.length == 1) {
			if (plugin.getC().getStringList("tabname.disabled-worlds").contains(p.getWorld().getName())) {
				sendMsg(p, plugin.getMsg("tabname.world-disabled", "%world%", p.getWorld().getName()));
				return true;
			}

			StringBuilder builder = new StringBuilder();
			for (int i = 0; i < args.length; i++) {
				builder.append(args[i] + " ");
			}

			String msg = builder.toString();
			if (isNameDisabled(msg)) {
				sendMsg(p, plugin.getMsg("tabname.name-disabled", "%name%", msg));
				return true;
			}

			if (msg.length() > maxLength) {
				sendMsg(p, plugin.getMsg("tabname.name-too-long", "%max%", maxLength));
				return true;
			}

			plugin.getTabNameHandler().setTabName(p, msg);
			sendMsg(p, plugin.getMsg("tabname.name-change-success", "%name%", msg));
		} else {
			if (!p.hasPermission(Perm.TABNAMEOTHER.getPerm())) {
				sendMsg(p, plugin.getMsg("no-permission", "%perm%", Perm.TABNAMEOTHER.getPerm()));
				return true;
			}

			Player target = Bukkit.getPlayer(args[0]);
			if (target == null) {
				sendMsg(p, plugin.getMsg("tabname.player-not-online", "%player%", args[0]));
				return true;
			}

			if (args.length == 1) {
				sendMsg(p, plugin.getMsg("tabname.usage", "%command%", label));
				return true;
			}

			if (plugin.getC().getStringList("tabname.disabled-worlds").contains(target.getWorld().getName())) {
				sendMsg(p, plugin.getMsg("tabname.world-disabled", "%world%", target.getWorld().getName()));
				return true;
			}

			StringBuilder builder = new StringBuilder();
			for (int i = 1; i < args.length; i++) {
				builder.append(args[i] + " ");
			}

			String msg = builder.toString();
			if (isNameDisabled(msg)) {
				sendMsg(p, plugin.getMsg("tabname.name-disabled", "%name%", msg));
				return true;
			}

			if (msg.length() > maxLength) {
				sendMsg(p, plugin.getMsg("tabname.name-too-long", "%max%", maxLength));
				return true;
			}

			plugin.getTabNameHandler().setTabName(target, msg);
			sendMsg(target,
					plugin.getMsg("tabname.target-changed-name-message", "%name%", msg, "%sender%", sender.getName()));
			sendMsg(sender,
					plugin.getMsg("tabname.target-name-change-success", "%target%", target.getName(), "%name%", msg));
		}

		return true;
	}

	private boolean isNameDisabled(String arg) {
		String text = Util.stripColor(arg.trim());
		List<String> restrictedNames = plugin.getC().getStringList("tabname.blacklist-names");
		if (restrictedNames.isEmpty()) {
			restrictedNames = plugin.getC().getStringList("tabname.restricted-names");
		}

		for (String b : restrictedNames) {
			if (b.equalsIgnoreCase(text)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		List<String> completionList = new ArrayList<>(), cmds = new ArrayList<>();
		String partOfCommand = "";

		if (sender.hasPermission(Perm.TABNAME.getPerm())) {
			if (args.length == 0) {
				cmds.add("tabname");
				cmds.add("tname");
				partOfCommand = args[0];

				StringUtil.copyPartialMatches(partOfCommand, cmds, completionList);
				Collections.sort(completionList);
				return completionList;
			}

			if (sender.hasPermission(Perm.RESET.getPerm()) && args.length == 1) {
				cmds.add("reset");
				partOfCommand = args[0];

				StringUtil.copyPartialMatches(partOfCommand, cmds, completionList);
				Collections.sort(completionList);
				return completionList;
			}
		}

		return null;
	}
}