package hu.montlikadani.tablist.bukkit.commands;

import static hu.montlikadani.tablist.bukkit.utils.Util.colorMsg;
import static hu.montlikadani.tablist.bukkit.utils.Util.sendMsg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import hu.montlikadani.tablist.bukkit.ConfigValues;
import hu.montlikadani.tablist.bukkit.Perm;
import hu.montlikadani.tablist.bukkit.TabList;

public class Commands implements CommandExecutor, TabCompleter {

	private TabList plugin;

	public Commands(TabList plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(final CommandSender sender, final Command cmd, final String label, final String[] args) {
		if (args.length == 0) {
			sendMsg(sender, colorMsg("&e&l[&9&lTab&4&lList&b&l Info&e&l]"));
			sendMsg(sender, colorMsg("&5Version:&a " + plugin.getDescription().getVersion()));
			sendMsg(sender, colorMsg("&5Author, created by:&a montlikadani"));
			sendMsg(sender, colorMsg("&5Commands:&8 /&7" + label + "&a help"));
			sendMsg(sender, colorMsg(
					"&4If you find a bug, send issue here:&e &nhttps://github.com/montlikadani/TabList/issues"));
			return true;
		}

		if (args[0].equalsIgnoreCase("help")) {
			if (sender instanceof Player && !sender.hasPermission(Perm.HELP.getPerm())) {
				sendMsg(sender, plugin.getMsg("no-permission", "%perm%", Perm.HELP.getPerm()));
				return true;
			}

			if (sender instanceof Player) {
				if (args.length == 1) {
					plugin.getMsgs().getStringList("chat-messages.1")
							.forEach(msg -> sender.sendMessage(colorMsg(msg.replace("%command%", label))));
				} else if (args.length == 2) {
					if (args[1].equals("2")) {
						plugin.getMsgs().getStringList("chat-messages.2")
								.forEach(msg -> sender.sendMessage(colorMsg(msg.replace("%command%", label))));
					} else if (args[1].equals("3")) {
						plugin.getMsgs().getStringList("chat-messages.3")
								.forEach(msg -> sender.sendMessage(colorMsg(msg.replace("%command%", label))));
					}
				}
			} else {
				plugin.getMsgs().getStringList("chat-messages.1")
						.forEach(msg -> sender.sendMessage(colorMsg(msg.replace("%command%", label))));

				plugin.getMsgs().getStringList("chat-messages.2")
						.forEach(msg -> sender.sendMessage(colorMsg(msg.replace("%command%", label))));

				plugin.getMsgs().getStringList("chat-messages.3")
						.forEach(msg -> sender.sendMessage(colorMsg(msg.replace("%command%", label))));
			}

			return true;
		}

		String path = "hu.montlikadani.tablist.bukkit.commands.list";
		ICommand command = null;
		try {
			command = (ICommand) TabList.class.getClassLoader().loadClass(path + "." + args[0].toLowerCase())
					.newInstance();
		} catch (ClassNotFoundException e) {
			sendMsg(sender, plugin.getMsg("unknown-sub-command", "%subcmd%", args[0]));
		} catch (IllegalAccessException | InstantiationException e) {
			e.printStackTrace();
		}

		if (command != null) {
			command.run(plugin, sender, cmd, label, args);
		}

		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		List<String> completionList = new ArrayList<>(), cmds = new ArrayList<>();
		String partOfCommand = "";

		if (args.length == 1) {
			getCmds(sender).forEach(cmds::add);
			partOfCommand = args[0];

			StringUtil.copyPartialMatches(partOfCommand, cmds, completionList);
			Collections.sort(completionList);
			return completionList;
		}

		if (args.length == 2) {
			if (ConfigValues.isFakePlayers() && args[0].equalsIgnoreCase("fakeplayers")) {
				Arrays.asList("add", "remove", "list").forEach(cmds::add);
				partOfCommand = args[1];

				StringUtil.copyPartialMatches(partOfCommand, cmds, completionList);
				Collections.sort(completionList);
				return completionList;
			}
		}

		if (args.length == 3) {
			if (ConfigValues.isFakePlayers() && args[0].equalsIgnoreCase("fakeplayers")) {
				if (args[1].equalsIgnoreCase("remove")) {
					plugin.getConf().getFakeplayers().getStringList("fakeplayers").forEach(cmds::add);
					partOfCommand = args[2];
				}

				StringUtil.copyPartialMatches(partOfCommand, cmds, completionList);
				Collections.sort(completionList);
				return completionList;
			}
		}

		return null;
	}

	private List<String> getCmds(CommandSender sender) {
		List<String> c = new ArrayList<>();
		for (String cmds : Arrays.asList("reload", "fakeplayers", "get", "removegroup", "setprefix", "setsuffix",
				"setpriority", "toggle")) {
			if (sender instanceof Player && !sender.hasPermission("tablist." + cmds)) {
				continue;
			}

			c.add(cmds);
		}

		return c;
	}
}