package hu.montlikadani.tablist;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

import hu.montlikadani.tablist.config.ConfigConstants;
import hu.montlikadani.tablist.tablist.TabManager;
import hu.montlikadani.tablist.tablist.groups.Groups;
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
import net.md_5.bungee.event.EventHandler;

public final class TabList extends Plugin implements Listener {

	private Configuration config;
	private TabManager tab;
	private Groups groups;

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

			config = ConfigurationProvider.getProvider(net.md_5.bungee.config.YamlConfiguration.class).load(file);
			ConfigConstants.load(config);

			int newConfigVersion = 8;
			int configVersion = config.getInt("config-version", 0);

			if (configVersion != newConfigVersion) {
				getLogger().log(java.util.logging.Level.WARNING,
						"Found outdated configuration (bungeeconfig.yml)! (Your version: " + configVersion + " | Newest version: "
								+ newConfigVersion + ")");
			}
		} catch (java.io.IOException e) {
			e.printStackTrace();
		}
	}

	private void newCommand() {
		getProxy().getPluginManager().registerCommand(this, new Command("tablist", "tablist.help", "tl") {
			@Override
			public void execute(final CommandSender sender, final String[] args) {
				if (args.length == 0) {
					if (sender instanceof ProxiedPlayer && !sender.hasPermission("tablist.help")) {
						Misc.sendMessage(sender, ConfigConstants.MessageKeys.NO_PERMISSION);
						return;
					}

					ConfigConstants.getMessageList(ConfigConstants.MessageKeys.CHAT_MESSAGES).forEach(sender::sendMessage);
					return;
				}

				String first = args[0];

				if (first.equalsIgnoreCase("reload") || first.equalsIgnoreCase("rl")) {
					if (sender instanceof ProxiedPlayer && !sender.hasPermission("tablist.reload")) {
						Misc.sendMessage(sender, ConfigConstants.MessageKeys.NO_PERMISSION);
						return;
					}

					reload();
					Misc.sendMessage(sender, ConfigConstants.MessageKeys.RELOAD_CONFIG);
					return;
				}

				if (first.equalsIgnoreCase("toggle")) {
					boolean isPlayer = sender instanceof ProxiedPlayer;

					if (isPlayer && !sender.hasPermission("tablist.toggle")) {
						Misc.sendMessage(sender, ConfigConstants.MessageKeys.NO_PERMISSION);
						return;
					}

					if (args.length < 2 && !isPlayer) {
						Misc.sendMessage(sender, ConfigConstants.MessageKeys.TOGGLE_NO_CONSOLE);
						return;
					}

					ProxiedPlayer target = null;

					if (args.length != 1) {
						String sec = args[1];

						if (sec.equalsIgnoreCase("all")) {
							if (getProxy().getOnlineCount() == 0) {
								Misc.sendMessage(sender, ConfigConstants.MessageKeys.TOGGLE_NO_PLAYERS_AVAILABLE);
								return;
							}

							for (ProxiedPlayer pl : getProxy().getPlayers()) {
								java.util.UUID playerId = pl.getUniqueId();

								if (!tab.tabEnableStatus.remove(playerId)) {
									tab.tabEnableStatus.add(playerId);
								}
							}

							return;
						}

						if ((target = getProxy().getPlayer(sec)) == null) {
							Misc.sendMessage(sender, ConfigConstants.MessageKeys.TOGGLE_NO_PLAYER);
							return;
						}
					}

					if (isPlayer && target == null) {
						target = (ProxiedPlayer) sender;
					}

					if (target == null) {
						return;
					}

					boolean enabled;
					java.util.UUID playerId = target.getUniqueId();

					if (!tab.tabEnableStatus.remove(playerId)) {
						enabled = !tab.tabEnableStatus.add(playerId);
					} else {
						enabled = true;
					}

					Misc.sendMessage(sender,
							enabled ? ConfigConstants.MessageKeys.TOGGLE_ENABLED : ConfigConstants.MessageKeys.TOGGLE_DISABLED);
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