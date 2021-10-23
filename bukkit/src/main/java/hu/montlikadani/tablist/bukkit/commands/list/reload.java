package hu.montlikadani.tablist.bukkit.commands.list;

import static hu.montlikadani.tablist.bukkit.utils.Util.sendMsg;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import hu.montlikadani.tablist.bukkit.Perm;
import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.commands.CommandProcessor;
import hu.montlikadani.tablist.bukkit.commands.ICommand;
import hu.montlikadani.tablist.bukkit.config.ConfigMessages;

@CommandProcessor(name = "reload", desc = "Reloads the entire plugin", permission = Perm.RELOAD)
public final class reload implements ICommand {

	@Override
	public boolean run(TabList plugin, CommandSender sender, Command cmd, String label, String[] args) {
		plugin.reload();
		sendMsg(sender, ConfigMessages.get(ConfigMessages.MessageKeys.RELOAD_CONFIG));
		return true;
	}
}
