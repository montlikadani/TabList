package hu.montlikadani.tablist.bukkit.listeners;

import static hu.montlikadani.tablist.bukkit.utils.Util.colorMsg;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import hu.montlikadani.tablist.bukkit.ConfigValues;
import hu.montlikadani.tablist.bukkit.PlayerList;
import hu.montlikadani.tablist.bukkit.TabList;
import net.ess3.api.events.AfkStatusChangeEvent;

public class EssAfkStatus implements Listener {

	@EventHandler
	public void onAfkChange(AfkStatusChangeEvent event) {
		Player p = event.getAffected().getBase();

		String path = "placeholder-format.afk-status.";
		if (ConfigValues.isAfkStatusEnabled() && !ConfigValues.isAfkStatusShowPlayerGroup()) {
			boolean rightLeft = ConfigValues.isAfkStatusShowInRightLeftSide();

			path += "format-" + (event.getValue() ? "yes" : "no");

			org.bukkit.configuration.file.FileConfiguration conf = TabList.getInstance().getC();
			String result = "";
			if (conf.contains(path)) {
				result = colorMsg(rightLeft ? p.getName() + conf.getString(path) : conf.getString(path) + p.getName());
			}

			if (!result.isEmpty()) {
				p.setPlayerListName(result);
			}
		}

		if (ConfigValues.isHidePlayerFromTabAfk()) {
			if (event.getValue()) {
				PlayerList.hidePlayer(p);
			} else {
				PlayerList.showPlayer(p);
			}
		}
	}
}