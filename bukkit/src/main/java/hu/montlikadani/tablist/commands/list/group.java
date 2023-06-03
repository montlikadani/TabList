package hu.montlikadani.tablist.commands.list;

import java.io.IOException;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import hu.montlikadani.tablist.Perm;
import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.commands.CommandProcessor;
import hu.montlikadani.tablist.commands.ICommand;
import hu.montlikadani.tablist.config.ConfigMessages;
import hu.montlikadani.tablist.config.constantsLoader.ConfigValues;
import hu.montlikadani.tablist.tablist.TabText;
import hu.montlikadani.tablist.tablist.groups.TeamHandler;
import hu.montlikadani.tablist.utils.Util;

@CommandProcessor(name = "group",
	params = "<name> prefix/suffix/tabname/priority/remove",
	desc = "Sets a meta for the given group's",
	permission = Perm.GROUP_META)
public final class group implements ICommand {

	@Override
	public void run(TabList plugin, CommandSender sender, Command cmd, String label, String[] args) {
		if (!ConfigValues.isPrefixSuffixEnabled()) {
			plugin.getConfig().set("change-prefix-suffix-in-tablist.enable", true);
			plugin.saveConfig();
		}

		if (args.length < 3) {
			plugin.getComplement().sendMessage(sender, Util.applyMinimessageFormat("&6/" + label + " group\n"
					+ "          &6prefix <prefix> -&7 Changes the prefix of an existing group.\n"
					+ "          &6suffix <suffix> -&7 Changes the suffix of an existing group.\n"
					+ "          &6tabname -&7 Changes the tab name of an existing group.\n"
					+ "          &6priority -&7 Sets a priority for the given group.\n"
					+ "          &6 remove -&7 Removes the specified group meta"
			));

			return;
		}

		String target = args[1];
		String type = args[2].toLowerCase(java.util.Locale.ENGLISH);
		FileConfiguration config = plugin.getConf().getGroups();
		int priority = 0;

		switch (type) {
		case "prefix":
		case "suffix":
		case "tabname":
			StringBuilder builder = new StringBuilder();

			for (int i = 3; i < args.length; i++) {
				builder.append(args[i] + (i + 1 < args.length ? " " : ""));
			}

			String result = builder.toString().replace("\"", "");
			if (result.trim().isEmpty()) {
				plugin.getComplement().sendMessage(sender, ConfigMessages.get(ConfigMessages.MessageKeys.SET_GROUP_META_COULD_NOT_BE_EMPTY));
				return;
			}

			config.set("groups." + target + "." + type, result);
			break;
		case "priority":
			try {
				priority = Integer.parseInt(args[3]);
			} catch (NumberFormatException e) {
				plugin.getComplement().sendMessage(sender, ConfigMessages.get(ConfigMessages.MessageKeys.SET_GROUP_PRIORITY_MUST_BE_NUMBER));
				return;
			}

			config.set("groups." + target + ".sort-priority", priority);
			break;
		case "remove":
			if (config.contains("groups." + target)) {
				config.set("groups." + target, null);
				plugin.getGroups().removeTeam(target);

				plugin.getComplement().sendMessage(sender, ConfigMessages.get(ConfigMessages.MessageKeys.SET_GROUP_REMOVED, "%team%", target));

				try {
					config.save(plugin.getConf().getGroupsFile());
				} catch (IOException e) {
					e.printStackTrace();
				}

				return;
			}

			plugin.getComplement().sendMessage(sender, ConfigMessages.get(ConfigMessages.MessageKeys.SET_GROUP_NOT_FOUND, "%team%", target));
			return;
		default:
			return;
		}

		try {
			config.save(plugin.getConf().getGroupsFile());
		} catch (IOException e) {
			e.printStackTrace();
		}

		String prefix = config.getString("groups." + target + ".prefix", ""), suffix = config.getString("groups." + target + ".suffix", ""),
				tabName = config.getString("groups." + target + ".tabname", "");

		TeamHandler team = null;

		for (TeamHandler one : plugin.getGroups().getTeams()) {
			if (one.team.equalsIgnoreCase(target)) {
				team = one;
				team.team = target;
				team.prefix = TabText.parseFromText(plugin.getPlaceholders().replaceMiscVariables(prefix));
				team.tabName = TabText.parseFromText(plugin.getPlaceholders().replaceMiscVariables(tabName));
				team.suffix = TabText.parseFromText(plugin.getPlaceholders().replaceMiscVariables(suffix));
				team.priority = priority;
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

			plugin.getGroups().addTeam(team);
		}

		plugin.getComplement().sendMessage(sender, ConfigMessages.get(ConfigMessages.MessageKeys.SET_GROUP_META_SET, "%team%", target, "%meta%", prefix + tabName + suffix));
	}
}
