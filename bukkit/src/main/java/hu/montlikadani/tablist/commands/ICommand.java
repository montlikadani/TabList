package hu.montlikadani.tablist.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import hu.montlikadani.tablist.TabList;

public interface ICommand {

	boolean run(final TabList plugin, final CommandSender sender, final Command cmd, final String label, final String[] args);
}
