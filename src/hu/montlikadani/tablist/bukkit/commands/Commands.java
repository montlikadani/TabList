package hu.montlikadani.tablist.bukkit.commands;

import static hu.montlikadani.tablist.bukkit.utils.Util.colorMsg;
import static hu.montlikadani.tablist.bukkit.utils.Util.logConsole;
import static hu.montlikadani.tablist.bukkit.utils.Util.sendMsg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.StringUtil;

import hu.montlikadani.tablist.Global;
import hu.montlikadani.tablist.bukkit.Perm;
import hu.montlikadani.tablist.bukkit.TabHandler;
import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.utils.Util;
import hu.montlikadani.tablist.bukkit.utils.ServerVersion.Version;

public class Commands implements CommandExecutor, TabCompleter {

	private TabList plugin;

	public Commands(TabList plugin) {
		this.plugin = plugin;
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean onCommand(final CommandSender sender, final Command cmd, final String label, final String[] args) {
		if (args.length == 0) {
			sendMsg(sender, colorMsg("&e&l[&9&lTab&4&lList&b&l Info&e&l]"));
			sendMsg(sender, colorMsg("&5Version:&a " + plugin.getDescription().getVersion()));
			sendMsg(sender, colorMsg("&5Author, created by:&a montlikadani"));
			sendMsg(sender, colorMsg("&5Commands:&8 /&7" + label + "&a help"));
			sendMsg(sender, colorMsg(
					"&4If you find a bug, send issue here:&e &nhttps://github.com/montlikadani/TabList/issues"));
		} else if (args[0].equalsIgnoreCase("help")) {
			if (sender instanceof Player && !sender.hasPermission(Perm.HELP.getPerm())) {
				sendMsg(sender, plugin.getMsg("no-permission", "%perm%", Perm.HELP.getPerm()));
				return true;
			}

			if (sender instanceof Player) {
				if (args.length == 1) {
					plugin.getMsgs().getStringList("chat-messages.1")
							.forEach(msg -> sendMsg(sender, colorMsg(msg.replace("%command%", label))));
				} else if (args.length == 2) {
					if (args[1].equals("2")) {
						plugin.getMsgs().getStringList("chat-messages.2")
								.forEach(msg -> sendMsg(sender, colorMsg(msg.replace("%command%", label))));
					} else if (args[1].equals("3")) {
						plugin.getMsgs().getStringList("chat-messages.3")
								.forEach(msg -> sendMsg(sender, colorMsg(msg.replace("%command%", label))));
					}
				}
			} else {
				plugin.getMsgs().getStringList("chat-messages.1")
						.forEach(msg -> sendMsg(sender, colorMsg(msg.replace("%command%", label))));

				plugin.getMsgs().getStringList("chat-messages.2")
						.forEach(msg -> sendMsg(sender, colorMsg(msg.replace("%command%", label))));

				plugin.getMsgs().getStringList("chat-messages.3")
						.forEach(msg -> sendMsg(sender, colorMsg(msg.replace("%command%", label))));
			}
		} else if (args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("rl")) {
			if (sender instanceof Player && !sender.hasPermission(Perm.RELOAD.getPerm())) {
				sendMsg(sender, plugin.getMsg("no-permission", "%perm%", Perm.RELOAD.getPerm()));
				return true;
			}

			plugin.reload();
			sendMsg(sender, plugin.getMsg("reload-config"));
		} else if (args[0].equalsIgnoreCase("get")) {
			if (sender instanceof Player && !sender.hasPermission(Perm.GET.getPerm())) {
				sendMsg(sender, plugin.getMsg("no-permission", "%perm%", Perm.GET.getPerm()));
				return true;
			}

			if (!plugin.getC().getBoolean("tabname.enable")) {
				sendMsg(sender, colorMsg("Tabname option is disabled in configuration!"));
				return true;
			}

			plugin.getConf().createNamesFile();

			if (!(sender instanceof Player)) {
				if (args.length == 1) {
					sendMsg(sender,
							plugin.getMsg("tabname.get-name.no-console", "%command%", label, "%args%", args[0]));
				} else if (args.length == 2) {
					Player targ = Bukkit.getPlayer(args[1]);
					if (targ == null) {
						sendMsg(sender, plugin.getMsg("tabname.player-not-online", "%player%", args[1]));
						return true;
					}

					String nam = targ.getName();
					if (!plugin.getConf().getNames().contains("players." + nam + ".tabname")) {
						sendMsg(sender, plugin.getMsg("tabname.no-tab-name", "%player%", nam));
						return true;
					}

					sendMsg(sender, plugin.getMsg("tabname.get-name.result", "%target%", nam, "%name%",
							plugin.getConf().getNames().getString("players." + nam + ".tabname")));
				}

				return true;
			}

			Player p = (Player) sender;
			if (args.length == 1) {
				if (!plugin.getConf().getNames().contains("players." + p.getName() + ".tabname")) {
					sendMsg(p, plugin.getMsg("tabname.no-tab-name", "%player%", p.getName()));
					return true;
				}

				sendMsg(p, plugin.getMsg("tabname.get-name.result", "%name%",
						plugin.getConf().getNames().getString("players." + p.getName() + ".tabname")));
			} else if (args.length == 2) {
				if (!p.hasPermission(Perm.GETO.getPerm())) {
					sendMsg(p, plugin.getMsg("no-permission", "%perm%", "tablist.get.other"));
					return true;
				}

				Player targ = Bukkit.getPlayer(args[1]);
				if (targ == null) {
					sendMsg(p, plugin.getMsg("tabname.player-not-online", "%player%", args[1]));
					return true;
				}

				String nam = targ.getName();
				if (!plugin.getConf().getNames().contains("players." + nam + ".tabname")) {
					sendMsg(p, plugin.getMsg("tabname.no-tab-name", "%player%", nam));
					return true;
				}

				sendMsg(p, plugin.getMsg("tabname.get-name.target-result", "%target%", nam, "%name%",
						plugin.getConf().getNames().getString("players." + nam + ".tabname")));
			}
		} else if (args[0].equalsIgnoreCase("toggle")) {
			if (sender instanceof Player && !sender.hasPermission(Perm.TOGGLE.getPerm())) {
				sendMsg(sender, plugin.getMsg("no-permission", "%perm%", Perm.TOGGLE.getPerm()));
				return true;
			}

			if (args.length == 1) {
				if (!(sender instanceof Player)) {
					sendMsg(sender, plugin.getMsg("toggle.console-usage", "%command%", label));
					return true;
				}

				Player p = (Player) sender;
				UUID uuid = p.getUniqueId();

				if (TabHandler.tabEnabled.containsKey(uuid)) {
					if (!TabHandler.tabEnabled.get(uuid)) {
						TabHandler.tabEnabled.put(uuid, true);
						sendMsg(p, plugin.getMsg("toggle.disabled"));
					} else {
						TabHandler.tabEnabled.put(uuid, false);
						sendMsg(p, plugin.getMsg("toggle.enabled"));
					}
				} else {
					TabHandler.tabEnabled.put(uuid, true);
					sendMsg(p, plugin.getMsg("toggle.disabled"));
				}
			} else if (args.length == 2) {
				if (args[1].equalsIgnoreCase("all")) {
					if (sender instanceof Player && !sender.hasPermission(Perm.TOGGLEALL.getPerm())) {
						sendMsg(sender, plugin.getMsg("no-permission", "%perm%", Perm.TOGGLEALL.getPerm()));
						return true;
					}

					if (Bukkit.getOnlinePlayers().isEmpty()) {
						sendMsg(sender, plugin.getMsg("toggle.no-player"));
						return true;
					}

					boolean changed = false;
					for (Player pl : Bukkit.getOnlinePlayers()) {
						UUID uuid = pl.getUniqueId();
						if (TabHandler.tabEnabled.containsKey(uuid)) {
							if (!TabHandler.tabEnabled.get(uuid)) {
								TabHandler.tabEnabled.put(uuid, true);
								plugin.getTabHandler().cancelTabForPlayer(pl, true);
								changed = true;
							} else {
								TabHandler.tabEnabled.put(uuid, false);
								plugin.getTabHandler().updateTab(pl);
								changed = false;
							}
						} else {
							TabHandler.tabEnabled.put(uuid, true);
							plugin.getTabHandler().cancelTabForPlayer(pl, true);
							changed = true;
						}
					}

					sendMsg(sender, plugin.getMsg("toggle." + (changed ? "disabled" : "enabled")));
					return true;
				}

				Player pl = Bukkit.getPlayer(args[1]);
				if (pl == null) {
					sendMsg(sender, plugin.getMsg("toggle.player-not-found", "%player%", args[1]));
					return true;
				}

				UUID uuid = pl.getUniqueId();
				if (TabHandler.tabEnabled.containsKey(uuid)) {
					if (!TabHandler.tabEnabled.get(uuid)) {
						TabHandler.tabEnabled.put(uuid, true);
						sendMsg(sender, plugin.getMsg("toggle.disabled"));
					} else {
						TabHandler.tabEnabled.put(uuid, false);
						sendMsg(sender, plugin.getMsg("toggle.enabled"));
					}
				} else {
					TabHandler.tabEnabled.put(uuid, true);
					sendMsg(sender, plugin.getMsg("toggle.disabled"));
				}
			}
		} else if (args[0].equalsIgnoreCase("fakeplayers") || args[0].equalsIgnoreCase("fp")) {
			if (!(sender instanceof Player)) {
				sendMsg(sender, plugin.getMsg("no-console", "%command%", label + " " + args[0]));
				return true;
			}

			Player p = (Player) sender;
			if (!p.hasPermission(Perm.FAKEPLAYERS.getPerm())) {
				sendMsg(p, plugin.getMsg("no-permission", "%perm%", Perm.FAKEPLAYERS.getPerm()));
				return true;
			}

			if (!plugin.getC().getBoolean("enable-fake-players")) {
				sendMsg(p, plugin.getMsg("fake-player.disabled"));
				return true;
			}

			plugin.getConf().createFakePlayersFile();

			if (args.length < 2) {
				sendMsg(p, plugin.getMsg("fake-player.usage", "%command%", label, "%args%", args[0]));
				return true;
			}

			if (args[1].equalsIgnoreCase("add")) {
				if (!p.hasPermission(Perm.ADDFAKEPLAYER.getPerm())) {
					sendMsg(p, plugin.getMsg("no-permission", "%perm%", "tablist.fakeplayers.add"));
					return true;
				}

				if (args.length < 3) {
					sendMsg(p, plugin.getMsg("fake-player.usage", "%command%", label, "%args%", args[0]));
					return true;
				}

				String name = args[2];
				if (plugin.getConf().getFakeplayers().getStringList("fakeplayers").contains(name)) {
					sendMsg(p, plugin.getMsg("fake-player.already-added", "%name%", name));
					return true;
				}

				if (plugin.createPlayer(p, name)) {
					sendMsg(p, plugin.getMsg("fake-player.added", "%name%", name));
				}
			} else if (args[1].equalsIgnoreCase("remove") || args[1].equalsIgnoreCase("rem")) {
				if (!p.hasPermission(Perm.REMOVEFAKEPLAYER.getPerm())) {
					sendMsg(p, plugin.getMsg("no-permission", "%perm%", Perm.REMOVEFAKEPLAYER.getPerm()));
					return true;
				}

				if (args.length < 3) {
					sendMsg(p, plugin.getMsg("fake-player.usage", "%command%", label, "%args%", args[0]));
					return true;
				}

				String name = args[2];
				if (!plugin.getConf().getFakeplayers().getStringList("fakeplayers").contains(name)) {
					sendMsg(p, plugin.getMsg("fake-player.already-removed", "%name%", name));
					return true;
				}

				if (plugin.removePlayer(name)) {
					sendMsg(p, plugin.getMsg("fake-player.removed", "%name%", name));
				}
			} else if (args[1].equalsIgnoreCase("list")) {
				if (!p.hasPermission(Perm.LISTFAKEPLAYERS.getPerm())) {
					sendMsg(p, plugin.getMsg("no-permission", "%perm%", Perm.LISTFAKEPLAYERS.getPerm()));
					return true;
				}

				List<String> fakepls = plugin.getConf().getFakeplayers().getStringList("fakeplayers");
				if (fakepls == null || fakepls.isEmpty()) {
					sendMsg(p, plugin.getMsg("fake-player.no-fake-player"));
					return true;
				}

				Collections.sort(fakepls);

				String msg = "";
				for (String fpl : fakepls) {
					if (!msg.isEmpty()) {
						msg += "&r, ";
					}

					msg += fpl;
				}

				for (String lpl : plugin.getMsgs().getStringList("fake-player.list")) {
					sendMsg(p, colorMsg(lpl.replace("%amount%", fakepls.size() + "").replace("%fake-players%", msg)));
				}
			}

			return true;
		} else if (args[0].equalsIgnoreCase("setprefix")) {
			if (sender instanceof Player && !sender.hasPermission(Perm.SETPREFIX.getPerm())) {
				sendMsg(sender, plugin.getMsg("no-permission", "%perm%", Perm.SETPREFIX.getPerm()));
				return true;
			}

			if (!plugin.getC().getBoolean("change-prefix-suffix-in-tablist.enable")) {
				logConsole(
						"The prefix-suffix is not enabled in the TabList configuration. Without not work this function.");
				return true;
			}

			if (args.length < 2) {
				sendMsg(sender, plugin.getMsg("set-prefix-suffix.prefix.usage", "%command%", label));
				return true;
			}

			plugin.getConf().createGroupsFile();

			Player target = Bukkit.getPlayer(args[1]);
			if (target == null) {
				sendMsg(sender, plugin.getMsg("set-prefix-suffix.prefix.player-not-found", "%target%", args[1]));
				return true;
			}

			StringBuilder build = new StringBuilder();
			for (int i = 2; i < args.length; i++) {
				build.append(args[i] + " ");
			}

			String pref = build.toString();
			if (pref.isEmpty()) {
				sendMsg(sender, plugin.getMsg("set-prefix-suffix.prefix.could-not-be-empty"));
				return true;
			}

			plugin.getGS().set("players." + target.getName() + ".prefix", pref);
			try {
				plugin.getGS().save(plugin.getConf().getGroupsFile());
			} catch (IOException e) {
				e.printStackTrace();
			}

			pref = plugin.getPlaceholders().setPlaceholders(target, pref);
			pref = Global.setSymbols(pref);
			pref = Util.colorMsg(pref);

			if (plugin.getChangeType().equals("scoreboard")) {
				Scoreboard b = Bukkit.getScoreboardManager().getNewScoreboard();
				Team t = null;
				if (plugin.getGS().contains("players." + target.getName() + ".sort-priority")) {
					t = getTeam(b,
							Integer.toString(plugin.getGS().getInt("players." + target.getName() + ".sort-priority")));
				} else {
					t = getTeam(b, target.getName());
				}

				if (Version.isCurrentLower(Version.v1_9_R1)) {
					if (!t.hasPlayer(target)) {
						t.addPlayer(target);
					}
				} else if (!t.hasEntry(target.getName())) {
					t.addEntry(target.getName());
				}

				pref = Util.splitStringByVersion(pref);

				t.setPrefix(pref);

				if (Version.isCurrentEqualOrHigher(Version.v1_13_R1)) {
					t.setColor(plugin.getGroups().fromPrefix(pref));
				}

				if (plugin.getGS().contains("players." + target.getName() + ".suffix")) {
					String suffix = plugin.getGS().getString("players." + target.getName() + ".suffix");
					suffix = plugin.getPlaceholders().setPlaceholders(target, suffix);
					suffix = Global.setSymbols(suffix);
					suffix = Util.colorMsg(suffix);

					t.setSuffix(suffix);
				}

				target.setScoreboard(b);
			} else if (plugin.getChangeType().equals("namer")) {
				target.setPlayerListName(colorMsg(pref + target.getName()));
			}

			sendMsg(sender, plugin.getMsg("set-prefix-suffix.prefix.successfully-set", "%tag%", pref, "%target%",
					target.getName()));
		} else if (args[0].equalsIgnoreCase("setsuffix")) {
			if (sender instanceof Player && !sender.hasPermission(Perm.SETSUFFIX.getPerm())) {
				sendMsg(sender, plugin.getMsg("no-permission", "%perm%", Perm.SETSUFFIX.getPerm()));
				return true;
			}

			if (!plugin.getC().getBoolean("change-prefix-suffix-in-tablist.enable")) {
				logConsole(
						"The prefix-suffix is not enabled in the TabList configuration. Without not work this function.");
				return true;
			}

			if (args.length < 2) {
				sendMsg(sender, plugin.getMsg("set-prefix-suffix.suffix.usage", "%command%", label));
				return true;
			}

			plugin.getConf().createGroupsFile();

			Player target = Bukkit.getPlayer(args[1]);
			if (target == null) {
				sendMsg(sender, plugin.getMsg("set-prefix-suffix.suffix.player-not-found", "%target%", args[1]));
				return true;
			}

			StringBuilder build = new StringBuilder();
			for (int i = 2; i < args.length; i++) {
				build.append(args[i] + " ");
			}

			String suf = build.toString();
			if (suf.isEmpty()) {
				sendMsg(sender, plugin.getMsg("set-prefix-suffix.suffix.could-not-be-empty"));
				return true;
			}

			plugin.getGS().set("players." + target.getName() + ".suffix", suf);
			try {
				plugin.getGS().save(plugin.getConf().getGroupsFile());
			} catch (IOException e) {
				e.printStackTrace();
			}

			suf = plugin.getPlaceholders().setPlaceholders(target, suf);
			suf = Global.setSymbols(suf);
			suf = Util.colorMsg(suf);

			if (plugin.getChangeType().equals("scoreboard")) {
				Scoreboard b = Bukkit.getScoreboardManager().getNewScoreboard();
				Team t = null;
				if (plugin.getGS().contains("players." + target.getName() + ".sort-priority")) {
					t = getTeam(b,
							Integer.toString(plugin.getGS().getInt("players." + target.getName() + ".sort-priority")));
				} else {
					t = getTeam(b, target.getName());
				}

				if (Version.isCurrentLower(Version.v1_9_R1)) {
					if (!t.hasPlayer(target)) {
						t.addPlayer(target);
					}
				} else if (!t.hasEntry(target.getName())) {
					t.addEntry(target.getName());
				}

				suf = Util.splitStringByVersion(suf);

				if (plugin.getGS().contains("players." + target.getName() + ".prefix")) {
					String prefix = plugin.getGS().getString("players." + target.getName() + ".prefix");
					prefix = plugin.getPlaceholders().setPlaceholders(target, prefix);
					prefix = Global.setSymbols(prefix);
					prefix = Util.colorMsg(prefix);

					t.setPrefix(prefix);
				}

				t.setSuffix(suf);
				target.setScoreboard(b);
			} else if (plugin.getChangeType().equals("namer")) {
				target.setPlayerListName(colorMsg(target.getName() + suf));
			}

			sendMsg(sender, plugin.getMsg("set-prefix-suffix.suffix.successfully-set", "%tag%", suf, "%target%",
					target.getName()));
		} else if (args[0].equalsIgnoreCase("setpriority")) {
			if (sender instanceof Player && !sender.hasPermission(Perm.SETPRIORITY.getPerm())) {
				sendMsg(sender, plugin.getMsg("no-permission", "%perm%", Perm.SETPRIORITY.getPerm()));
				return true;
			}

			if (!plugin.getC().getBoolean("change-prefix-suffix-in-tablist.enable")) {
				logConsole(
						"The prefix-suffix is not enabled in the TabList configuration. Without not work this function.");
				return true;
			}

			if (args.length < 3) {
				sendMsg(sender, plugin.getMsg("set-prefix-suffix.set-priority.usage", "%command%", label));
				return true;
			}

			Player target = Bukkit.getPlayer(args[1]);
			if (target == null) {
				sendMsg(sender, plugin.getMsg("set-prefix-suffix.set-priority.player-not-found", "%target%", args[1]));
				return true;
			}

			if (!args[2].matches("[0-9]+")) {
				sendMsg(sender, plugin.getMsg("set-prefix-suffix.set-priority.priority-must-be-number"));
				return true;
			}

			if (plugin.getChangeType().equals("scoreboard")) {
				Scoreboard b = Bukkit.getScoreboardManager().getNewScoreboard();
				Team t = getTeam(b, target.getName());
				t.unregister();
				target.setScoreboard(b);

				t = getTeam(b, args[2]);
				target.setScoreboard(b);
			} else if (plugin.getChangeType().equals("namer")) {
				target.setPlayerListName(target.getName());
			}

			plugin.getGS().set("players." + target.getName() + ".sort-priority", Integer.valueOf(args[2]));
			try {
				plugin.getGS().save(plugin.getConf().getGroupsFile());
			} catch (IOException e) {
				e.printStackTrace();
			}

			sendMsg(sender, plugin.getMsg("set-prefix-suffix.set-priority.successfully-set", "%number%", args[2],
					"%target%", target.getName()));
		} else if (args[0].equalsIgnoreCase("removeplayer")) {
			if (sender instanceof Player && !sender.hasPermission(Perm.REMOVEPLAYER.getPerm())) {
				sendMsg(sender, plugin.getMsg("no-permission", "%perm%", Perm.REMOVEPLAYER.getPerm()));
				return true;
			}

			if (!plugin.getC().getBoolean("change-prefix-suffix-in-tablist.enable")) {
				logConsole(
						"The prefix-suffix is not enabled in the TabList configuration. Without not work this function.");
				return true;
			}

			if (args.length < 2) {
				sendMsg(sender, plugin.getMsg("set-prefix-suffix.remove-player.usage", "%command%", label));
				return true;
			}

			Player target = Bukkit.getPlayer(args[1]);
			if (target == null) {
				sendMsg(sender, plugin.getMsg("set-prefix-suffix.remove-player.player-not-found", "%target%", args[1]));
				return true;
			}

			if (!plugin.getGS().contains("players." + target.getName())) {
				sendMsg(sender, plugin.getMsg("set-prefix-suffix.remove-player.player-not-found-in-database",
						"%target%", target.getName()));
				return true;
			}

			if (plugin.getChangeType().equals("scoreboard")) {
				Scoreboard b = Bukkit.getScoreboardManager().getNewScoreboard();
				Team t = null;

				if (plugin.getGS().contains("players." + target.getName() + ".sort-priority")) {
					t = getTeam(b,
							Integer.toString(plugin.getGS().getInt("players." + target.getName() + ".sort-priority")));
				} else {
					t = getTeam(b, target.getName());
				}

				t.unregister();
				target.setScoreboard(b);
			} else if (plugin.getChangeType().equals("namer")) {
				target.setPlayerListName(target.getName());
			}

			plugin.getGS().set("players." + target.getName(), null);
			try {
				plugin.getGS().save(plugin.getConf().getGroupsFile());
			} catch (IOException e) {
				e.printStackTrace();
			}

			sendMsg(sender, plugin.getMsg("set-prefix-suffix.remove-player.successfully-removed", "%target%",
					target.getName()));
		} else {
			sendMsg(sender, plugin.getMsg("unknown-sub-command", "%subcmd%", args[0]));
		}

		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		List<String> completionList = new ArrayList<>();
		List<String> cmds = new ArrayList<>();
		String partOfCommand = "";

		if (args.length == 1) {
			getCmds(sender).forEach(cmds::add);
			partOfCommand = args[0];

			StringUtil.copyPartialMatches(partOfCommand, cmds, completionList);
			Collections.sort(completionList);

			return completionList;
		}

		if (args.length == 2) {
			if (plugin.getC().getBoolean("enable-fake-players") && args[0].equalsIgnoreCase("fakeplayers")
					|| args[0].equalsIgnoreCase("fp")) {
				Arrays.asList("add", "remove", "rem", "list").forEach(cmds::add);
				partOfCommand = args[1];

				StringUtil.copyPartialMatches(partOfCommand, cmds, completionList);
				Collections.sort(completionList);

				return completionList;
			}
		}

		if (args.length == 3) {
			if (plugin.getC().getBoolean("enable-fake-players") && args[0].equalsIgnoreCase("fakeplayers")
					|| args[0].equalsIgnoreCase("fp")) {
				if (args[1].equalsIgnoreCase("remove") || args[1].equalsIgnoreCase("rem")) {
					plugin.getConf().getFakeplayers().getStringList("fakeplayers").forEach(cmds::add);
					partOfCommand = args[2];
				}

				StringUtil.copyPartialMatches(partOfCommand, cmds, completionList);
				Collections.sort(completionList);

				return completionList;
			}
		}

		return null;
	}

	private List<String> getCmds(CommandSender sender) {
		List<String> c = new ArrayList<>();
		for (String cmds : Arrays.asList("help", "reload", "get", "toggle", "fakeplayers", "setprefix", "setsuffix",
				"removeplayer")) {
			if (sender instanceof Player && !sender.hasPermission("tablist." + cmds)) {
				continue;
			}

			c.add(cmds);
		}
		return c;
	}

	private Team getTeam(Scoreboard board, String name) {
		return board.getTeam(name) == null ? board.registerNewTeam(name) : board.getTeam(name);
	}
}