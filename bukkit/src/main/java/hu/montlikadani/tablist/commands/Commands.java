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

public final class Commands implements CommandExecutor, TabCompleter {

	private final TabList plugin;

	private final Set<ICommand> commands = new HashSet<>(5);

	public Commands(TabList plugin) {
		this.plugin = plugin;

		for (String s : new String[] { "reload", "fakeplayers", "player", "group", "toggle" }) {
			try {
				Class<?> clazz;

				try {
					clazz = TabList.class.getClassLoader().loadClass("hu.montlikadani.tablist.commands.list." + s);
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
					continue;
				}

				commands.add((ICommand) clazz.getDeclaredConstructor().newInstance());
			} catch (ReflectiveOperationException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public boolean onCommand(final CommandSender sender, final Command cmd, final String label, final String[] args) {
		if (args.length == 0) {
			plugin.getComplement().sendMessage(sender, colorText("&9&lTab&4&lList"));
			plugin.getComplement().sendMessage(sender, colorText("&5Version:&a " + plugin.getDescription().getVersion()));
			plugin.getComplement().sendMessage(sender, colorText("&5Author, created by:&a montlikadani"));
			plugin.getComplement().sendMessage(sender, colorText("&5List of commands:&7 /" + label + " help"));
			plugin.getComplement().sendMessage(sender, colorText("&4Report bugs/features here:&e &nhttps://github.com/montlikadani/TabList/issues"));
			return true;
		}

		boolean isPlayer = sender instanceof Player;
		String first = args[0];

		boolean isHelp;
		if ((isHelp = "help".equalsIgnoreCase(first)) && isPlayer && !sender.hasPermission(Perm.HELP.permission)) {
			plugin.getComplement().sendMessage(sender, ConfigMessages.get(ConfigMessages.MessageKeys.NO_PERMISSION, "%perm%", Perm.HELP.permission));
			return true;
		}

		for (ICommand command : commands) {
			CommandProcessor proc = command.getClass().getAnnotation(CommandProcessor.class);

			if (proc == null) {
				continue;
			}

			if (isHelp) {
				if (!isPlayer || sender.hasPermission(proc.permission().permission)) {
					String params = proc.params().isEmpty() ? "" : ' ' + proc.params();
					plugin.getComplement().sendMessage(sender, colorText("&7/" + label + " " + proc.name() + params + " -&6 " + proc.desc()));
				}

				continue;
			}

			if (!proc.name().equalsIgnoreCase(first)) {
				continue;
			}

			if (proc.playerOnly() && !isPlayer) {
				plugin.getComplement().sendMessage(sender, ConfigMessages.get(ConfigMessages.MessageKeys.NO_CONSOLE, "%command%", label + " " + first));
				return true;
			}

			if (isPlayer && !sender.hasPermission(proc.permission().permission)) {
				plugin.getComplement().sendMessage(sender, ConfigMessages.get(ConfigMessages.MessageKeys.NO_PERMISSION, "%perm%", proc.permission().permission));
				return true;
			}

			command.run(plugin, sender, cmd, label, args);
			return true;
		}

		if (!isHelp) {
			plugin.getComplement().sendMessage(sender, ConfigMessages.get(ConfigMessages.MessageKeys.UNKNOWN_SUB_COMMAND, "%subcmd%", first));
		}

		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		List<String> cmds = new ArrayList<>(this.commands.size());

		switch (args.length) {
		case 1:
			cmds.addAll(availableSubCommands(sender));
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

			if (ConfigValues.isFakePlayers() && first.equalsIgnoreCase("fakeplayers") && !args[1].equalsIgnoreCase("add") && !args[1].equalsIgnoreCase("list")) {
				for (IFakePlayer fp : plugin.getFakePlayerHandler().fakePlayers) {
					cmds.add(fp.getName());
				}
			} else if (first.equalsIgnoreCase("group") || first.equalsIgnoreCase("player")) {
				for (String ca : new String[] { "prefix", "suffix", "priority", "tabname", "remove" }) {
					cmds.add(ca);
				}
			}

			break;
		default:
			break;
		}

		return cmds.isEmpty() ? null : cmds; // Suggest player names
	}

	private Set<String> availableSubCommands(CommandSender sender) {
		Set<String> set = new HashSet<>(commands.size());
		boolean isPlayer = sender instanceof Player;

		for (ICommand cmd : commands) {
			CommandProcessor proc = cmd.getClass().getAnnotation(CommandProcessor.class);

			if (proc != null && (!isPlayer || sender.hasPermission(proc.permission().permission))) {
				set.add(proc.name());
			}
		}

		return set;
	}
}
