package hu.montlikadani.tablist.bukkit.listeners;

import static hu.montlikadani.tablist.bukkit.utils.Util.colorMsg;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import hu.montlikadani.tablist.bukkit.PlayerList;
import hu.montlikadani.tablist.bukkit.TabList;
import net.ess3.api.events.AfkStatusChangeEvent;

public class EssAfkStatus implements Listener {

	@EventHandler
	public void onAfkChange(AfkStatusChangeEvent event) {
		Player p = event.getAffected().getBase();
		org.bukkit.configuration.file.FileConfiguration conf = TabList.getInstance().getC();

		String path = "placeholder-format.afk-status.";
		if (conf.getBoolean(path + "enable") && !conf.getBoolean(path + "show-player-group")) {
			boolean rightLeft = conf.getBoolean(path + "show-in-right-or-left-side");

			path += "format-" + (event.getValue() ? "yes" : "no");

			String result = "";
			if (conf.contains(path)) {
				result = colorMsg(rightLeft ? p.getName() + conf.getString(path) : conf.getString(path) + p.getName());
			}

			if (!result.isEmpty()) {
				p.setPlayerListName(result);
			}
		}

		if (conf.getBoolean("hide-player-from-tab-when-afk")) {
			if (event.getValue()) {
				PlayerList.hidePlayer(p);
			} else {
				PlayerList.showPlayer(p);
			}
		}
	}
}