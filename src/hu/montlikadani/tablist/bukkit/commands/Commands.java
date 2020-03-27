package hu.montlikadani.tablist.bukkit.commands;

import static hu.montlikadani.tablist.bukkit.utils.Util.colorMsg;
import static hu.montlikadani.tablist.bukkit.utils.Util.sendMsg;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import com.google.common.collect.ImmutableList;

import hu.montlikadani.tablist.bukkit.Perm;
import hu.montlikadani.tablist.bukkit.TabList;

public class Commands implements CommandExecutor, TabCompleter {

	private TabList plugin;

	private final Map<String, String> arg = new HashMap<>();

	public Commands(TabList plugin) {
		this.plugin = plugin;

		String path = "hu.montlikadani.tablist.bukkit.commands.list";
		ImmutableList<Class<?>> classes = hu.montlikadani.tablist.bukkit.utils.Util.getClasses(path);
		for (Class<?> cl : classes) {
			if (cl != null) {
				String className = cl.getName().toLowerCase();
				arg.put(className.replace(path + ".", ""), className);
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
		} else if (args[0].equalsIgnoreCase("help")) {
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

		boolean unknown = false;
		for (Entry<String, String> a : arg.entrySet()) {
			if (args[0].equalsIgnoreCase(a.getKey())) {
				try {
					Object run = Class.forName(a.getValue()).newInstance();
					Class<?>[] paramTypes = { TabList.class, CommandSender.class, Command.class, String.class,
							String[].class };
					Method printMethod = run.getClass().getDeclaredMethod("run", paramTypes);

					Object[] arguments = { plugin, sender, cmd, label, args };
					printMethod.invoke(run, arguments);
				} catch (Exception e) {
					e.printStackTrace();
				}

				unknown = false;
				break;
			}

			unknown = true;
		}

		if (unknown) {
			sendMsg(sender, plugin.getMsg("unknown-sub-command", "%subcmd%", args[0]));
		}

		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		List<String> completionList = new ArrayList<>();
		List<String> cmds = new ArrayList<>();
		String partOfCommand = "";

		if (args.length == 1) {
			getCmds(sender).forEach(cmds::add);
			partOfCommand = args[0];

			StringUtil.copyPartialMatches(partOfCommand, cmds, completionList);
			Collections.sort(completionList);

			return completionList;
		}

		if (args.length == 2) {
			if (plugin.getC().getBoolean("enable-fake-players") && args[0].equalsIgnoreCase("fakeplayers")) {
				Arrays.asList("add", "remove", "list").forEach(cmds::add);
				partOfCommand = args[1];

				StringUtil.copyPartialMatches(partOfCommand, cmds, completionList);
				Collections.sort(completionList);

				return completionList;
			}
		}

		if (args.length == 3) {
			if (plugin.getC().getBoolean("enable-fake-players") && args[0].equalsIgnoreCase("fakeplayers")) {
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
		for (String cmds : arg.keySet()) {
			if (sender instanceof Player && !sender.hasPermission("tablist." + cmds)) {
				continue;
			}

			c.add(cmds);
		}

		return c;
	}
}