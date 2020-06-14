package hu.montlikadani.tablist.bukkit.tablist;

import static hu.montlikadani.tablist.bukkit.utils.Util.colorMsg;
import static hu.montlikadani.tablist.bukkit.utils.Util.logConsole;

import java.io.IOException;
import java.util.logging.Level;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;

import hu.montlikadani.tablist.Global;
import hu.montlikadani.tablist.bukkit.ConfigValues;
import hu.montlikadani.tablist.bukkit.TabList;

public class TabNameHandler {

	private TabList plugin;

	public TabNameHandler(TabList plugin) {
		this.plugin = plugin;
	}

	public void loadTabName(Player p) {
		if (!ConfigValues.isTabNameEnabled()) {
			return;
		}

		String result = "";
		if (ConfigValues.isTabNameUseEssentialsNickName()) {
			if (!plugin.isPluginEnabled("Essentials")) {
				logConsole(Level.WARNING, "The Essentials plugin not found. Without the nickname option not work.");
				return;
			}

			User user = JavaPlugin.getPlugin(Essentials.class).getUser(p);
			if (user.getNickname() != null) {
				result = colorMsg(user.getNickname() + "&r");
			}
		} else {
			String name = getTabName(p);
			if (!name.isEmpty()) {
				name = plugin.getPlaceholders().setPlaceholders(p, Global.setSymbols(name));

				if (ConfigValues.isDefaultColorEnabled()) {
					result = colorMsg(ConfigValues.getDefaultTabNameColor() + name + "&r");
				} else {
					result = ConfigValues.isTabNameColorCodeEnabled() ? colorMsg(name + "&r") : name + "\u00a7r";
				}
			} else {
				if (ConfigValues.isDefaultColorEnabled()) {
					result = colorMsg(ConfigValues.getDefaultTabNameColor() + p.getName());
				}
			}
		}

		if (!result.isEmpty()) {
			p.setPlayerListName(result);
		}
	}

	public String getTabName(Player p) {
		return getTabName(p.getName());
	}

	public String getTabName(String name) {
		return ConfigValues.isTabNameEnabled() && plugin.getConf().getNames().contains("players." + name)
				? plugin.getConf().getNames().getString("players." + name + ".tabname", "")
				: "";
	}

	public void setTabName(Player p, String name) {
		if (!ConfigValues.isTabNameEnabled()) {
			return;
		}

		String result = "", tName = "";

		if (ConfigValues.isTabNameUseEssentialsNickName()) {
			if (plugin.isPluginEnabled("Essentials")) {
				User user = JavaPlugin.getPlugin(Essentials.class).getUser(p);
				if (user.getNickname() != null) {
					result = colorMsg(user.getNickname());
					tName = user.getNickname();
				}
			} else {
				logConsole(Level.WARNING, "The Essentials plugin not found. Without the nickname option not work.");
				return;
			}
		} else {
			if (ConfigValues.isDefaultColorEnabled()) {
				result = colorMsg(ConfigValues.getDefaultTabNameColor()
						+ plugin.getPlaceholders().setPlaceholders(p, Global.setSymbols(name)) + "&r");
			} else {
				result = ConfigValues.isTabNameColorCodeEnabled()
						? colorMsg(plugin.getPlaceholders().setPlaceholders(p, Global.setSymbols(name)) + "&r")
						: name + "\u00a7r";
			}

			tName = name;
		}

		if (!result.isEmpty()) {
			p.setPlayerListName(result);
		}

		if (!tName.isEmpty()) {
			plugin.getConf().getNames().set("players." + p.getName() + ".tabname", tName);

			try {
				plugin.getConf().getNames().save(plugin.getConf().getNamesFile());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void unTabName(Player p) {
		if (!ConfigValues.isTabNameEnabled()) {
			return;
		}

		p.setPlayerListName(p.getName());

		plugin.getConf().getNames().set("players." + p.getName() + ".tabname", null);
		plugin.getConf().getNames().set("players." + p.getName(), null);

		try {
			plugin.getConf().getNames().save(plugin.getConf().getNamesFile());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
