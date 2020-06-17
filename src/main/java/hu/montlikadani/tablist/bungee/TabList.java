package hu.montlikadani.tablist.bungee;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import hu.montlikadani.tablist.bungee.tablist.PlayerTab;
import hu.montlikadani.tablist.bungee.tablist.TabManager;
import hu.montlikadani.tablist.bungee.tablist.groups.Groups;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ProxyReloadEvent;
import net.md_5.bungee.api.event.ServerDisconnectEvent;
import net.md_5.bungee.api.event.ServerKickEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;

public class TabList extends Plugin implements Listener {

	private static TabList instance;

	private Configuration config;
	private TabManager tab;
	private Groups groups;

	private int cver = 7;

	@Override
	public void onEnable() {
		instance = this;

		tab = new TabManager(this);
		groups = new Groups(this);

		newCommand();
		reload();

		getProxy().getPluginManager().registerListener(this, this);
	}

	@Override
	public void onDisable() {
		if (instance == null)
			return;

		tab.cancel();
		groups.cancel();
		instance = null;
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

	public static TabList getInstance() {
		return instance;
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
		try {
			File folder = getDataFolder();
			if (!folder.exists()) {
				folder.mkdir();
			}

			File file = new File(folder, "bungeeconfig.yml");
			if (!file.exists()) {
				InputStream in = getResourceAsStream("bungeeconfig.yml");
				Files.copy(in, file.toPath());
				in.close();
			}

			config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
			if (!config.get("config-version").equals(cver)) {
				getLogger().log(Level.WARNING, "Found outdated configuration (bungeeconfig.yml)! (Your version: "
						+ config.getInt("config-version") + " | Newest version: " + cver + ")");
			}
		} catch (Exception e) {
			e.printStackTrace();
			getLogger().log(Level.WARNING,
					"There was an error. Please report it here:\nhttps://github.com/montlikadani/TabList/issues");
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

							boolean changed = false;
							for (ProxiedPlayer pl : getProxy().getPlayers()) {
								if (!tab.getTabToggle().contains(pl.getUniqueId())) {
									tab.getTabToggle().add(pl.getUniqueId());
									changed = true;
								} else {
									tab.getTabToggle().remove(pl.getUniqueId());
									changed = false;
								}
							}

							if (changed) {
								tab.cancel();
							} else {
								tab.start();
							}

							return;
						}

						ProxiedPlayer p = getProxy().getPlayer(args[1]);
						if (p == null) {
							Misc.sendMessage(s, config.getString("messages.toggle.no-player"));
							return;
						}

						boolean enabled = false;
						if (!tab.getTabToggle().contains(p.getUniqueId())) {
							tab.getTabToggle().add(p.getUniqueId());
							enabled = false;
						} else {
							tab.getTabToggle().remove(p.getUniqueId());
							enabled = true;
						}

						Misc.sendMessage(s, config.getString("messages.toggle." + (enabled ? "enabled" : "disabled")));
					} else if (s instanceof ProxiedPlayer) {
						ProxiedPlayer player = (ProxiedPlayer) s;

						boolean enabled = false;
						if (!tab.getTabToggle().contains(player.getUniqueId())) {
							tab.getTabToggle().add(player.getUniqueId());
							enabled = false;
						} else {
							tab.getTabToggle().remove(player.getUniqueId());
							enabled = true;
						}

						Misc.sendMessage(player,
								config.getString("messages.toggle." + (enabled ? "enabled" : "disabled")));
					}
				}
			}
		});
	}

	/**
	 * @param e
	 */
	@EventHandler
	public void onLogin(PostLoginEvent e) {
		if (tab.getTask() == null) {
			tab.start();
		}

		if (groups.getTask() == null) {
			groups.start();
		}

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
	public void onServerChange(net.md_5.bungee.api.event.ServerConnectedEvent event) {
		getProxy().getScheduler().schedule(this,
				() -> tab.getPlayerTab(event.getPlayer()).ifPresent(PlayerTab::loadTabList), 1, TimeUnit.SECONDS);
	}

	/**
	 * @param ev
	 */
	@EventHandler
	public void onProxyReload(ProxyReloadEvent ev) {
		reload();
	}

	public BaseComponent[] getComponentBuilder(String s) {
		return new ComponentBuilder(s).create();
	}
}