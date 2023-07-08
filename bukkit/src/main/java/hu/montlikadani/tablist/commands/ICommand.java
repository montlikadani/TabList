package hu.montlikadani.tablist.commands;

public interface ICommand {

	void run(hu.montlikadani.tablist.TabList plugin, org.bukkit.command.CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args);

}
