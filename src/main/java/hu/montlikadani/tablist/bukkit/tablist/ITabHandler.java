package hu.montlikadani.tablist.bukkit.tablist;

import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public interface ITabHandler {

	Player getPlayer();

	List<String> getHeader();

	void setHeader(List<String> header);

	List<String> getFooter();

	void setFooter(List<String> footer);

	BukkitTask getTask();
}
