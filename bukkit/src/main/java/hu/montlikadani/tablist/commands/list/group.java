package hu.montlikadani.tablist.commands.list;

import static hu.montlikadani.tablist.utils.Util.sendMsg;

import java.io.IOException;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import hu.montlikadani.tablist.Perm;
import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.commands.CommandProcessor;
import hu.montlikadani.tablist.commands.ICommand;
import hu.montlikadani.tablist.commands.Commands.ContextArguments;
import hu.montlikadani.tablist.config.ConfigMessages;
import hu.montlikadani.tablist.config.constantsLoader.ConfigValues;
import hu.montlikadani.tablist.tablist.TabText;
import hu.montlikadani.tablist.tablist.groups.TeamHandler;
import hu.montlikadani.tablist.utils.Util;

@CommandProcessor(name = "group",
		params = "<name> prefix/suffix/tabname <displayTag>",
		desc = "Sets the given group's prefix/suffix or tabname",
		permission = Perm.GROUP_META)
public final class group implements ICommand {

	@Override
	public boolean run(TabList plugin, CommandSender sender, Command cmd, String label, String[] args) {
		if (!ConfigValues.isPrefixSuffixEnabled()) {
			plugin.getConfig().set("change-prefix-suffix-in-tablist.enable", true);
			plugin.saveConfig();
		}

		if (args.length < 3) {
			sendMsg(sender, Util.colorText("&6/" + label + " group"
					+ "\n          &6prefix <prefix> -&7 Changes the prefix of an existing group."
					+ "\n          &6suffix <suffix> -&7 Changes the suffix of an existing group."
					+ "\n          &6tabname -&7 Changes the tabname of an existing group."));
			return false;
		}

		String target = args[1];

		ContextArguments argument = ContextArguments.PREFIX;
		switch (args[2].toLowerCase(java.util.Locale.ENGLISH)) {
		case "prefix":
			argument = ContextArguments.PREFIX;
			break;
		case "suffix":
			argument = ContextArguments.SUFFIX;
			break;
		case "tabname":
			argument = ContextArguments.TABNAME;
			break;
		case "priority":
			argument = ContextArguments.PRIORITY;
			break;
		case "remove":
			argument = ContextArguments.REMOVE;
			break;
		default:
			return false;
		}

		FileConfiguration config = plugin.getConf().getGroups();

		int priority = 0;

		switch (argument) {
		case PREFIX:
		case SUFFIX:
		case TABNAME:
			StringBuilder builder = new StringBuilder();

			for (int i = 3; i < args.length; i++) {
				builder.append(args[i] + (i + 1 < args.length ? " " : ""));
			}

			String result = builder.toString().replace("\"", "");
			if (result.trim().isEmpty()) {
				sendMsg(sender, ConfigMessages.get(ConfigMessages.MessageKeys.SET_GROUP_META_COULD_NOT_BE_EMPTY));
				return false;
			}

			config.set("groups." + target + "." + argument.loweredName, result);
			break;
		case PRIORITY:
			try {
				priority = Integer.parseInt(args[3]);
			} catch (NumberFormatException e) {
				sendMsg(sender, ConfigMessages.get(ConfigMessages.MessageKeys.SET_GROUP_PRIORITY_MUST_BE_NUMBER));
				return false;
			}

			config.set("groups." + target + ".sort-priority", priority);
			break;
		case REMOVE:
			if (config.contains("groups." + target)) {
				config.set("groups." + target, null);
			} else {
				sendMsg(sender, ConfigMessages.get(ConfigMessages.MessageKeys.SET_GROUP_NOT_FOUND, "%team%", target));
				return false;
			}

			break;
		default:
			return false;
		}

		try {
			config.save(plugin.getConf().getGroupsFile());
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (argument == ContextArguments.REMOVE) {
			plugin.getGroups().removeGroup(target);
			sendMsg(sender, ConfigMessages.get(ConfigMessages.MessageKeys.SET_GROUP_REMOVED, "%team%", target));
		} else {
			String prefix = config.getString("groups." + target + ".prefix", ""),
					suffix = config.getString("groups." + target + ".suffix", ""),
					tabName = config.getString("groups." + target + ".tabname", "");

			java.util.List<TeamHandler> teams = plugin.getGroups().getGroupsList();
			TeamHandler team = null;

			for (int i = 0; i < teams.size(); i++) {
				TeamHandler t = teams.get(i);

				if (t.team.equalsIgnoreCase(target)) {
					t.team = target;
					t.prefix = TabText.parseFromText(plugin.getPlaceholders().replaceMiscVariables(prefix));
					t.tabName = TabText.parseFromText(plugin.getPlaceholders().replaceMiscVariables(tabName));
					t.suffix = TabText.parseFromText(plugin.getPlaceholders().replaceMiscVariables(suffix));
					t.priority = priority;

					teams.set(i, t);
					team = t;
					break;
				}
			}

			if (team == null) {
				team = new TeamHandler();

				team.team = target;
				team.prefix = TabText.parseFromText(plugin.getPlaceholders().replaceMiscVariables(prefix));
				team.tabName = TabText.parseFromText(plugin.getPlaceholders().replaceMiscVariables(tabName));
				team.suffix = TabText.parseFromText(plugin.getPlaceholders().replaceMiscVariables(suffix));
				team.priority = priority;

				teams.add(team);
			}

			sendMsg(sender, ConfigMessages.get(ConfigMessages.MessageKeys.SET_GROUP_META_SET, "%team%", target, "%meta%",
					prefix + tabName + suffix));
		}

		return true;
	}
}
