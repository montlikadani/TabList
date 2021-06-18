package hu.montlikadani.tablist.bungee;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Level;

import hu.montlikadani.tablist.bungee.config.ConfigConstants;
import hu.montlikadani.tablist.bungee.tablist.TabManager;
import hu.montlikadani.tablist.bungee.tablist.groups.Groups;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ProxyReloadEvent;
import net.md_5.bungee.api.event.ServerDisconnectEvent;
import net.md_5.bungee.api.event.ServerKickEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;

public final class TabList extends Plugin implements Listener {

	private Configuration config;
	private TabManager tab;
	private Groups groups;

	private int cver = 7;

	@Override
	public void onEnable() {
		tab = new TabManager(this);
		groups = new Groups(this);

		getProxy().getPluginManager().registerListener(this, this);
		newCommand();

		reload();
	}

	@Override
	public void onDisable() {
		getProxy().getPluginManager().unregisterCommands(this);
		getProxy().getPluginManager().unregisterListeners(this);

		tab.cancel();
		groups.cancel();
	}

	public Configuration getConf() {
		return config;
	}

	public TabManager getTab() {
		return tab;
	}

	public Groups getGroups() {
		return groups;
	}

	private void reload() {
		loadFile();

		tab.start();
		groups.start();

		getProxy().getPlayers().forEach(pl -> {
			tab.addPlayer(pl);
			groups.addPlayer(pl);
		});
	}

	private void loadFile() {
		File folder = getDataFolder();
		folder.mkdirs();

		File file = new File(folder, "bungeeconfig.yml");

		try {
			if (!file.exists()) {
				try (InputStream in = getResourceAsStream("bungeeconfig.yml")) {
					Files.copy(in, file.toPath());
				}
			}

			config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
			ConfigConstants.load(config);

			if (!config.get("config-version").equals(cver)) {
				getLogger().log(Level.WARNING, "Found outdated configuration (bungeeconfig.yml)! (Your version: "
						+ config.getInt("config-version") + " | Newest version: " + cver + ")");
			}
		} catch (java.io.IOException e) {
			e.printStackTrace();
		}
	}

	private void newCommand() {
		getProxy().getPluginManager().registerCommand(this, new Command("tablist", "tablist.help", "tl") {
			@Override
			public void execute(final CommandSender s, final String[] args) {
				if (args.length < 1) {
					if (s instanceof ProxiedPlayer && !hasPermission(s)) {
						Misc.sendMessage(s, config.getString("messages.no-permission"));
						return;
					}

					config.getStringList("messages.chat-messages").forEach(msg -> Misc.sendMessage(s, msg));
					return;
				}

				if (args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("rl")) {
					if (s instanceof ProxiedPlayer && !s.hasPermission("tablist.reload")) {
						Misc.sendMessage(s, config.getString("messages.no-permission"));
						return;
					}

					reload();
					Misc.sendMessage(s, config.getString("messages.reload-config"));
				} else if (args[0].equalsIgnoreCase("toggle")) {
					if (s instanceof ProxiedPlayer && !s.hasPermission("tablist.toggle")) {
						Misc.sendMessage(s, config.getString("messages.no-permission"));
						return;
					}

					if (args.length < 2 && !(s instanceof ProxiedPlayer)) {
						Misc.sendMessage(s, config.getString("messages.toggle.no-console"));
						return;
					}

					if (args.length > 1) {
						if (args[1].equalsIgnoreCase("all")) {
							if (getProxy().getPlayers().isEmpty()) {
								Misc.sendMessage(s, config.getString("messages.toggle.no-players-available"));
								return;
							}

							for (ProxiedPlayer pl : getProxy().getPlayers()) {
								if (!tab.getTabToggle().remove(pl.getUniqueId())) {
									tab.getTabToggle().add(pl.getUniqueId());
								}
							}

							return;
						}

						ProxiedPlayer target = getProxy().getPlayer(args[1]);
						if (target == null) {
							Misc.sendMessage(s, config.getString("messages.toggle.no-player"));
							return;
						}

						boolean enabled = false;

						if (!tab.getTabToggle().remove(target.getUniqueId())) {
							tab.getTabToggle().add(target.getUniqueId());
							enabled = false;
						} else {
							enabled = true;
						}

						Misc.sendMessage(s, config.getString("messages.toggle." + (enabled ? "enabled" : "disabled")));
					} else if (s instanceof ProxiedPlayer) {
						ProxiedPlayer player = (ProxiedPlayer) s;
						boolean enabled = false;

						if (!tab.getTabToggle().remove(player.getUniqueId())) {
							tab.getTabToggle().add(player.getUniqueId());
							enabled = false;
						} else {
							enabled = true;
						}

						Misc.sendMessage(player,
								config.getString("messages.toggle." + (enabled ? "enabled" : "disabled")));
					}
				}
			}
		});
	}

	@EventHandler
	public void onLogin(PostLoginEvent e) {
		tab.start();
		groups.start();

		tab.addPlayer(e.getPlayer());
		groups.addPlayer(e.getPlayer());
	}

	@EventHandler
	public void onLeave(ServerDisconnectEvent event) {
		tab.removePlayer(event.getPlayer());
		groups.removePlayer(event.getPlayer());
	}

	@EventHandler
	public void onKick(ServerKickEvent event) {
		tab.removePlayer(event.getPlayer());
		groups.removePlayer(event.getPlayer());
	}

	@EventHandler
	public void onServerSwitch(ServerSwitchEvent ev) {
		getProxy().getScheduler().schedule(this, () -> {
			tab.addPlayer(ev.getPlayer());
			groups.addPlayer(ev.getPlayer());

			tab.start();
			groups.start();
		}, 20L, java.util.concurrent.TimeUnit.MILLISECONDS);
	}

	@EventHandler
	public void onProxyReload(ProxyReloadEvent ev) {
		reload();
	}
}