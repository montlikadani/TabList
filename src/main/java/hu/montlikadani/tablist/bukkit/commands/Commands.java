package hu.montlikadani.tablist.bukkit.commands;

import static hu.montlikadani.tablist.bukkit.utils.Util.colorMsg;
import static hu.montlikadani.tablist.bukkit.utils.Util.sendMsg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import hu.montlikadani.tablist.bukkit.Perm;
import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.config.ConfigValues;
import hu.montlikadani.tablist.bukkit.utils.ReflectionUtils.ClassMethods;

public class Commands implements CommandExecutor, TabCompleter {

	private TabList plugin;

	@SuppressWarnings("serial")
	private final Set<String> subCmds = new HashSet<String>() {
		{
			add("reload");
			add("fakeplayers");
			add("get");
			add("removegroup");
			add("setprefix");
			add("setsuffix");
			add("setpriority");
			add("toggle");
			add("help");
		}
	};

	private final Set<ICommand> cmds = new HashSet<>();

	@SuppressWarnings("deprecation")
	public Commands(TabList plugin) {
		this.plugin = plugin;

		for (String s : subCmds) {
			try {
				Class<?> c = null;
				try {
					c = TabList.class.getClassLoader().loadClass("hu.montlikadani.tablist.bukkit.commands.list." + s);
				} catch (ClassNotFoundException e) {
				}

				if (c == null) {
					continue;
				}

				if (ClassMethods.getCurrentVersion() >= 9) {
					cmds.add((ICommand) c.getDeclaredConstructor().newInstance());
				} else {
					cmds.add((ICommand) c.newInstance());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
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
					if (args[1].equals("2") || args[1].equals("3")) {
						plugin.getMsgs().getStringList("chat-messages." + args[1])
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

		boolean found = false;
		for (ICommand command : cmds) {
			if (command.getClass().getSimpleName().equalsIgnoreCase(args[0])) {
				command.run(plugin, sender, cmd, label, args);
				found = true;
				break;
			}
		}

		if (!found) {
			sendMsg(sender, plugin.getMsg("unknown-sub-command", "%subcmd%", args[0]));
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
				Arrays.asList("add", "remove", "list", "setskin", "setping").forEach(cmds::add);
				partOfCommand = args[1];

				StringUtil.copyPartialMatches(partOfCommand, cmds, completionList);
				Collections.sort(completionList);
				return completionList;
			} else if (args[0].equalsIgnoreCase("toggle")) {
				cmds.add("all");
				partOfCommand = args[1];

				StringUtil.copyPartialMatches(partOfCommand, cmds, completionList);
				Collections.sort(completionList);
				return completionList;
			}
		}

		if (args.length == 3 && ConfigValues.isFakePlayers() && args[0].equalsIgnoreCase("fakeplayers")) {
			if (args[1].equalsIgnoreCase("remove") || args[1].equalsIgnoreCase("setskin")
					|| args[1].equalsIgnoreCase("setping")) {
				plugin.getFakePlayerHandler().getFakePlayersFromConfig().stream()
						.map(fp -> fp.contains(";") ? fp.split(";")[0] : fp).forEach(cmds::add);
				partOfCommand = args[2];
			}

			StringUtil.copyPartialMatches(partOfCommand, cmds, completionList);
			Collections.sort(completionList);
			return completionList;
		}

		return null;
	}

	private Set<String> getCmds(CommandSender sender) {
		if (!(sender instanceof Player)) {
			return subCmds;
		}

		// Don't use stream for tab-complete
		Set<String> c = new HashSet<>();
		for (String name : subCmds) {
			if (sender.hasPermission("tablist." + name)) {
				c.add(name);
			}
		}

		return c;
	}
}