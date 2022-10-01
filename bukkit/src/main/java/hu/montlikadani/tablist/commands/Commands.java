package hu.montlikadani.tablist.commands;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import hu.montlikadani.tablist.Perm;
import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.config.ConfigMessages;
import hu.montlikadani.tablist.config.constantsLoader.ConfigValues;
import hu.montlikadani.tablist.tablist.fakeplayers.IFakePlayer;

import static hu.montlikadani.tablist.utils.Util.colorText;
import static hu.montlikadani.tablist.utils.Util.sendMsg;

public final class Commands implements CommandExecutor, TabCompleter {

	private final TabList plugin;

	private final Set<ICommand> cmds = new HashSet<>(5);

	public Commands(TabList plugin) {
		this.plugin = plugin;

		for (String s : new String[] { "reload", "fakeplayers", "player", "group", "toggle" }) {
			try {
				Class<?> c = null;

				try {
					c = TabList.class.getClassLoader().loadClass("hu.montlikadani.tablist.commands.list." + s);
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}

				if (c != null) {
					cmds.add((ICommand) c.getDeclaredConstructor().newInstance());
				}
			} catch (ReflectiveOperationException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public boolean onCommand(final CommandSender sender, final Command cmd, final String label, final String[] args) {
		if (args.length == 0) {
			sendMsg(sender, colorText("&9&lTab&4&lList"));
			sendMsg(sender, colorText("&5Version:&a " + plugin.getDescription().getVersion()));
			sendMsg(sender, colorText("&5Author, created by:&a montlikadani"));
			sendMsg(sender, colorText("&5Commands:&7 /" + label + " help"));
			sendMsg(sender,
					colorText("&4If you find a bug, make issue here:&e &nhttps://github.com/montlikadani/TabList/issues"));
			return true;
		}

		boolean isPlayer = sender instanceof Player;
		String first = args[0];

		boolean isHelp;
		if ((isHelp = "help".equalsIgnoreCase(first)) && isPlayer && !sender.hasPermission(Perm.HELP.permission)) {
			sendMsg(sender, ConfigMessages.get(ConfigMessages.MessageKeys.NO_PERMISSION, "%perm%", Perm.HELP.permission));
			return true;
		}

		for (ICommand command : cmds) {
			CommandProcessor proc = command.getClass().getAnnotation(CommandProcessor.class);

			if (proc == null) {
				continue;
			}

			if (isHelp) {
				if (!isPlayer || sender.hasPermission(proc.permission().permission)) {
					String params = proc.params().isEmpty() ? "" : ' ' + proc.params();
					sendMsg(sender, colorText("&7/" + label + " " + proc.name() + params + " -&6 " + proc.desc()));
				}

				continue;
			}

			if (!proc.name().equalsIgnoreCase(first)) {
				continue;
			}

			if (proc.playerOnly() && !isPlayer) {
				sendMsg(sender, ConfigMessages.get(ConfigMessages.MessageKeys.NO_CONSOLE, "%command%", label + " " + first));
				return true;
			}

			if (isPlayer && !sender.hasPermission(proc.permission().permission)) {
				sendMsg(sender,
						ConfigMessages.get(ConfigMessages.MessageKeys.NO_PERMISSION, "%perm%", proc.permission().permission));
				return true;
			}

			command.run(plugin, sender, cmd, label, args);
			return true;
		}

		if (!isHelp) {
			sendMsg(sender, ConfigMessages.get(ConfigMessages.MessageKeys.UNKNOWN_SUB_COMMAND, "%subcmd%", first));
		}

		return true;
	}

	private static final ContextArguments[] VALUES = ContextArguments.values();

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		List<String> cmds = new ArrayList<>(this.cmds.size());

		switch (args.length) {
		case 1:
			cmds.addAll(getCmds(sender));
			break;
		case 2:
			if (ConfigValues.isFakePlayers() && args[0].equalsIgnoreCase("fakeplayers")) {
				for (String c : new String[] { "add", "remove", "list", "setskin", "setping", "setdisplayname", "rename" }) {
					cmds.add(c);
				}
			} else if (args[0].equalsIgnoreCase("toggle")) {
				cmds.add("all");
			}

			break;
		case 3:
			String first = args[0];

			if (ConfigValues.isFakePlayers() && first.equalsIgnoreCase("fakeplayers") && !args[1].equalsIgnoreCase("add")
					&& !args[1].equalsIgnoreCase("list")) {
				for (IFakePlayer fp : plugin.getFakePlayerHandler().getFakePlayers()) {
					cmds.add(fp.getName());
				}
			} else if (first.equalsIgnoreCase("group") || first.equalsIgnoreCase("player")) {
				for (ContextArguments ca : VALUES) {
					cmds.add(ca.loweredName);
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
		Set<String> c = new HashSet<>(cmds.size());

		boolean isPlayer = sender instanceof Player;

		for (ICommand cmd : cmds) {
			CommandProcessor proc = cmd.getClass().getAnnotation(CommandProcessor.class);

			if (proc != null && (!isPlayer || sender.hasPermission(proc.permission().permission))) {
				c.add(proc.name());
			}
		}

		return c;
	}

	public enum ContextArguments {
		PREFIX, SUFFIX, PRIORITY, TABNAME, REMOVE;

		public final String loweredName = name().toLowerCase(java.util.Locale.ENGLISH);
	}
}