package hu.montlikadani.tablist.bukkit.commands;

import static hu.montlikadani.tablist.bukkit.utils.Util.colorMsg;
import static hu.montlikadani.tablist.bukkit.utils.Util.sendMsg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.google.common.reflect.TypeToken;

import hu.montlikadani.tablist.bukkit.Perm;
import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.config.constantsLoader.ConfigValues;
import hu.montlikadani.tablist.bukkit.tablist.fakeplayers.IFakePlayers;
import hu.montlikadani.tablist.bukkit.utils.ReflectionUtils.JavaAccessibilities;

public class Commands implements CommandExecutor, TabCompleter {

	private TabList plugin;

	private final Set<ICommand> cmds = new HashSet<>();

	public Commands(TabList plugin) {
		this.plugin = plugin;

		for (String s : Arrays.asList("reload", "fakeplayers", "player", "group", "toggle", "help")) {
			try {
				Class<?> c = null;
				try {
					c = TabList.class.getClassLoader().loadClass("hu.montlikadani.tablist.bukkit.commands.list." + s);
				} catch (ClassNotFoundException e) {
				}

				if (c == null) {
					continue;
				}

				if (JavaAccessibilities.getCurrentVersion() >= 9) {
					cmds.add((ICommand) c.getDeclaredConstructor().newInstance());
				} else {
					cmds.add((ICommand) c.newInstance());
				}
			} catch (ReflectiveOperationException e) {
				e.printStackTrace();
			}
		}
	}

	@SuppressWarnings("serial")
	@Override
	public boolean onCommand(final CommandSender sender, final Command cmd, final String label, final String[] args) {
		if (args.length == 0) {
			sendMsg(sender, colorMsg("&9&lTab&4&lList"));
			sendMsg(sender, colorMsg("&5Version:&a " + plugin.getDescription().getVersion()));
			sendMsg(sender, colorMsg("&5Author, created by:&a montlikadani"));
			sendMsg(sender, colorMsg("&5Commands:&8 /&7" + label + "&a help"));
			sendMsg(sender, colorMsg(
					"&4If you find a bug, make issue here:&e &nhttps://github.com/montlikadani/TabList/issues"));
			return true;
		}

		if (args[0].equalsIgnoreCase("help")) {
			if (sender instanceof Player && !sender.hasPermission(Perm.HELP.getPerm())) {
				sendMsg(sender, plugin.getMsg("no-permission", "%perm%", Perm.HELP.getPerm()));
				return true;
			}

			plugin.getMsg(new TypeToken<List<String>>() {}.getSubtype(List.class), "chat-messages", "%command%", label)
				.forEach(s -> sendMsg(sender, s));
			return true;
		}

		boolean found = false;
		for (ICommand command : cmds) {
			CommandProcessor proc = command.getClass().getAnnotation(CommandProcessor.class);
			if (proc != null && proc.name().equalsIgnoreCase(args[0])) {
				found = true;

				if (proc.playerOnly() && !(sender instanceof Player)) {
					sendMsg(sender, plugin.getMsg("no-console", "%command%", label + " " + args[0]));
					return false;
				}

				if (sender instanceof Player && !sender.hasPermission(proc.permission().getPerm())) {
					sendMsg(sender, plugin.getMsg("no-permission", "%perm%", proc.permission().getPerm()));
					return false;
				}

				command.run(plugin, sender, cmd, label, args);
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
		List<String> cmds = new ArrayList<>();

		switch (args.length) {
		case 1:
			cmds.addAll(getCmds(sender));
			break;
		case 2:
			if (ConfigValues.isFakePlayers() && args[0].equalsIgnoreCase("fakeplayers")) {
				cmds.addAll(Arrays.asList("add", "remove", "list", "setskin", "setping", "setdisplayname", "rename"));
			} else if (args[0].equalsIgnoreCase("toggle")) {
				cmds.add("all");
			}

			break;
		case 3:
			if (ConfigValues.isFakePlayers() && args[0].equalsIgnoreCase("fakeplayers")
					&& !args[1].equalsIgnoreCase("add") && !args[1].equalsIgnoreCase("list")) {
				for (IFakePlayers fp : plugin.getFakePlayerHandler().getFakePlayers()) {
					cmds.add(fp.getName());
				}
			} else if (args[0].equalsIgnoreCase("group") || args[0].equalsIgnoreCase("player")) {
				for (ContextArguments ca : ContextArguments.values()) {
					cmds.add(ca.toString().toLowerCase());
				}
			}

			break;
		default:
			break;
		}

		return cmds.isEmpty() ? null : cmds; // Suggest player names
	}

	private Set<String> getCmds(CommandSender sender) {
		// Try to avoid using stream for tab-complete
		Set<String> c = new HashSet<>();

		for (ICommand cmd : cmds) {
			if (cmd.getClass().isAnnotationPresent(CommandProcessor.class)) {
				CommandProcessor proc = cmd.getClass().getAnnotation(CommandProcessor.class);
				if (!(sender instanceof Player) || sender.hasPermission(proc.permission().getPerm())) {
					c.add(proc.name());
				}
			}
		}

		return c;
	}

	public enum ContextArguments {
		PREFIX, SUFFIX, PRIORITY, TABNAME, REMOVE;
	}
}