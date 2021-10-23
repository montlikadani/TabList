package hu.montlikadani.tablist.bukkit.commands.list;

import static hu.montlikadani.tablist.bukkit.utils.Util.sendMsg;

import java.io.IOException;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import hu.montlikadani.tablist.bukkit.Perm;
import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.commands.CommandProcessor;
import hu.montlikadani.tablist.bukkit.commands.ICommand;
import hu.montlikadani.tablist.bukkit.commands.Commands.ContextArguments;
import hu.montlikadani.tablist.bukkit.config.ConfigMessages;
import hu.montlikadani.tablist.bukkit.config.constantsLoader.ConfigValues;
import hu.montlikadani.tablist.bukkit.tablist.groups.GroupPlayer;
import hu.montlikadani.tablist.bukkit.tablist.groups.TeamHandler;

@CommandProcessor(
	name = "player",
	params = "<name> prefix/suffix/tabname <displayTag>",
	desc = "Sets the given player's prefix/suffix or tabname",
	permission = Perm.PLAYER_META)
public final class player implements ICommand {

	@Override
	public boolean run(TabList plugin, CommandSender sender, Command cmd, String label, String[] args) {
		if (!ConfigValues.isPrefixSuffixEnabled()) {
			plugin.getConfig().set("change-prefix-suffix-in-tablist.enable", true);
			plugin.saveConfig();
		}

		if (args.length < 3) {
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
			if (!args[3].matches("[0-9]+")) {
				sendMsg(sender, ConfigMessages.get(ConfigMessages.MessageKeys.SET_GROUP_PRIORITY_MUST_BE_NUMBER));
				return false;
			}

			config.set("groups." + target + ".sort-priority", priority = Integer.parseInt(args[3]));
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
			plugin.getUser(plugin.getServer().getPlayer(target)).ifPresent(plugin.getGroups()::removePlayerGroup);
			plugin.getGroups().removeGroup(target);

			sendMsg(sender, ConfigMessages.get(ConfigMessages.MessageKeys.SET_GROUP_REMOVED, "%team%", target));
		} else {
			String prefix = config.getString("groups." + target + ".prefix", ""),
					suffix = config.getString("groups." + target + ".suffix", ""),
					tabName = config.getString("groups." + target + ".tabname", "");

			TeamHandler team = plugin.getGroups().getTeam(target).orElse(new TeamHandler());

			team.setTeam(target);
			team.setPrefix(prefix);
			team.setTabName(tabName);
			team.setSuffix(suffix);
			team.setPriority(priority);

			Player playerTarget = plugin.getServer().getPlayer(target);
			GroupPlayer groupPlayer = plugin.getGroups().addPlayer(playerTarget);

			if (groupPlayer != null) {
				if (!prefix.isEmpty()) {
					prefix = plugin.getPlaceholders().replaceVariables(playerTarget, prefix);
					groupPlayer.setCustomPrefix(prefix);
				}

				if (!suffix.isEmpty()) {
					suffix = plugin.getPlaceholders().replaceVariables(playerTarget, suffix);
					groupPlayer.setCustomSuffix(suffix);
				}

				groupPlayer.setCustomPriority(priority);
				plugin.getGroups().setPlayerTeam(groupPlayer, priority);
			}

			int index = plugin.getGroups().getGroupsList().indexOf(team);
			if (index > -1) {
				plugin.getGroups().getGroupsList().set(index, team);
			} else {
				plugin.getGroups().getGroupsList().add(team);
			}

			sendMsg(sender, ConfigMessages.get(ConfigMessages.MessageKeys.SET_GROUP_META_SET, "%team%", target,
					"%meta%", prefix + tabName + suffix));
		}

		return false;
	}
}
