package hu.montlikadani.tablist.commands;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import hu.montlikadani.tablist.utils.Util;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.config.ConfigMessages;
import hu.montlikadani.tablist.config.constantsLoader.ConfigValues;
import hu.montlikadani.tablist.tablist.fakeplayers.IFakePlayer;

public final class Commands implements CommandExecutor, TabCompleter {

	private final TabList plugin;

	private final ICommand[] commands = new ICommand[5];

	public Commands(TabList plugin) {
		this.plugin = plugin;

		int i = 0;

		for (String s : new String[] { "reload", "fakeplayers", "player", "group", "toggle" }) {
			try {
				Class<?> clazz;

				try {
					clazz = TabList.class.getClassLoader().loadClass("hu.montlikadani.tablist.commands.list." + s);
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
					continue;
				}

				commands[i] = (ICommand) clazz.getDeclaredConstructor().newInstance();
				i++;
			} catch (ReflectiveOperationException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (args.length == 0) {
			plugin.getComplement().sendMessage(sender, Util.applyTextFormat("&9&lTab&4&lList"));
			plugin.getComplement().sendMessage(sender, Util.applyTextFormat("&5Version:&a " + plugin.getDescription().getVersion()));
			plugin.getComplement().sendMessage(sender, Util.applyTextFormat("&5Author, created by:&a montlikadani"));
			plugin.getComplement().sendMessage(sender, Util.applyTextFormat("&5List of commands:&7 /" + label + " help"));
			plugin.getComplement().sendMessage(sender, Util.applyTextFormat("&4Report bugs/features here:&e &nhttps://github.com/montlikadani/TabList/issues"));
			return true;
		}

		boolean isPlayer = sender instanceof Player;
		String first = args[0];

		for (ICommand command : commands) {
			CommandProcessor processor = command.getClass().getAnnotation(CommandProcessor.class);

			if (processor == null || !processor.name().equalsIgnoreCase(first)) {
				continue;
			}

			if (isPlayer) {
				if (!sender.hasPermission(processor.permission().value)) {
					plugin.getComplement().sendMessage(sender, ConfigMessages.get(ConfigMessages.MessageKeys.NO_PERMISSION, "%perm%",
							processor.permission().value));
					return true;
				}
			} else if (processor.playerOnly()) {
				plugin.getComplement().sendMessage(sender, ConfigMessages.get(ConfigMessages.MessageKeys.NO_CONSOLE,
						"%command%", label + " " + first));
				return true;
			}

			command.run(plugin, sender, cmd, label, args);
			return true;
		}

		for (ICommand command : commands) {
			CommandProcessor processor = command.getClass().getAnnotation(CommandProcessor.class);

			if (processor == null) {
				continue;
			}

			if (!isPlayer || sender.hasPermission(processor.permission().value)) {
				String params = processor.params().isEmpty() ? "" : ' ' + processor.params();

				plugin.getComplement().sendMessage(sender, Util.applyTextFormat("&7/" + label + " "
						+ processor.name() + params + " -&6 " + processor.desc()));
			}
		}

		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		List<String> list = new ArrayList<>(this.commands.length);

		switch (args.length) {
		case 1:
			list.addAll(availableSubCommands(sender));
			break;
		case 2:
			if (ConfigValues.isFakePlayers() && args[0].equalsIgnoreCase("fakeplayers")) {
				for (String c : new String[] { "add", "remove", "list", "setskin", "setping", "setdisplayname", "rename" }) {
					list.add(c);
				}
			} else if (args[0].equalsIgnoreCase("toggle")) {
				list.add("all");
			}

			break;
		case 3:
			String first = args[0];

			if (ConfigValues.isFakePlayers() && first.equalsIgnoreCase("fakeplayers")
					&& !args[1].equalsIgnoreCase("add") && !args[1].equalsIgnoreCase("list")) {
				for (IFakePlayer fp : plugin.getFakePlayerHandler().fakePlayers) {
					list.add(fp.getName());
				}
			} else if (first.equalsIgnoreCase("group") || first.equalsIgnoreCase("player")) {
				for (String ca : new String[] { "prefix", "suffix", "priority", "tabname", "remove" }) {
					list.add(ca);
				}
			}

			break;
		default:
			break;
		}

		return list.isEmpty() ? null : list; // Suggest player names
	}

	private Set<String> availableSubCommands(CommandSender sender) {
		Set<String> set = new HashSet<>(commands.length);
		boolean isPlayer = sender instanceof Player;

		for (ICommand cmd : commands) {
			CommandProcessor proc = cmd.getClass().getAnnotation(CommandProcessor.class);

			if (proc != null && (!isPlayer || sender.hasPermission(proc.permission().value))) {
				set.add(proc.name());
			}
		}

		return set;
	}
}
