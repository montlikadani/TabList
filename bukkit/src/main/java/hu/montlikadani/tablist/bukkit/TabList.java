package hu.montlikadani.tablist.bukkit;

import static hu.montlikadani.tablist.bukkit.utils.Util.colorMsg;
import static hu.montlikadani.tablist.bukkit.utils.Util.logConsole;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import hu.montlikadani.tablist.AnimCreator;
import hu.montlikadani.tablist.bukkit.Objects.ObjectTypes;
import hu.montlikadani.tablist.bukkit.commands.Commands;
import hu.montlikadani.tablist.bukkit.commands.TabNameCmd;
import hu.montlikadani.tablist.bukkit.config.ConfigValues;
import hu.montlikadani.tablist.bukkit.config.Configuration;
import hu.montlikadani.tablist.bukkit.listeners.Listeners;
import hu.montlikadani.tablist.bukkit.listeners.plugins.CMIAfkStatus;
import hu.montlikadani.tablist.bukkit.listeners.plugins.EssAfkStatus;
import hu.montlikadani.tablist.bukkit.tablist.TabManager;
import hu.montlikadani.tablist.bukkit.tablist.TabNameHandler;
import hu.montlikadani.tablist.bukkit.tablist.fakeplayers.FakePlayerHandler;
import hu.montlikadani.tablist.bukkit.utils.Metrics;
import hu.montlikadani.tablist.bukkit.utils.ServerVersion;
import hu.montlikadani.tablist.bukkit.utils.UpdateDownloader;
import hu.montlikadani.tablist.bukkit.utils.Util;
import hu.montlikadani.tablist.bukkit.utils.Variables;
import hu.montlikadani.tablist.bukkit.utils.plugin.VaultPermission;
import hu.montlikadani.tablist.bukkit.utils.ServerVersion.Version;

public class TabList extends JavaPlugin {

	private static TabList instance;

	private VaultPermission vaultPermission;
	private Objects objects;
	private Variables variables;
	private Groups g;
	private ServerVersion mcVersion;
	private Configuration conf;
	private TabManager tabManager;
	private FakePlayerHandler fakePlayerHandler;
	private TabNameHandler tabNameHandler;

	private boolean isSpigot = false;
	private boolean hasVault = false;
	private int tabRefreshTime = 0;

	private final Set<AnimCreator> animations = Collections.synchronizedSet(new HashSet<AnimCreator>());
	private final Map<Player, HidePlayers> hidePlayers = new HashMap<>();

	@Override
	public void onEnable() {
		long load = System.currentTimeMillis();

		instance = this;

		try {
			Class.forName("org.spigotmc.SpigotConfig");
			isSpigot = true;
		} catch (ClassNotFoundException c) {
			isSpigot = false;
		}

		try {
			mcVersion = new ServerVersion();

			if (mcVersion.getVersion().isLower(Version.v1_8_R1)) {
				logConsole(Level.SEVERE,
						"Your server version does not supported by this plugin! Please use 1.8+ or higher versions!",
						false);
				getServer().getPluginManager().disablePlugin(this);
				return;
			}

			conf = new Configuration(this);
			objects = new Objects();
			g = new Groups(this);
			variables = new Variables(this);
			tabManager = new TabManager(this);
			fakePlayerHandler = new FakePlayerHandler(this);
			tabNameHandler = new TabNameHandler(this);

			conf.loadFiles();
			loadValues();

			if (ConfigValues.isPlaceholderAPI() && isPluginEnabled("PlaceholderAPI")) {
				logConsole("Hooked PlaceholderAPI version: "
						+ me.clip.placeholderapi.PlaceholderAPIPlugin.getInstance().getDescription().getVersion());
			}

			hasVault = initVaultPerm();

			fakePlayerHandler.load();
			loadAnimations();
			loadListeners();
			registerCommands();
			tabManager.loadToggledTabs();
			g.load();

			getServer().getOnlinePlayers().forEach(this::updateAll);

			UpdateDownloader.checkFromGithub(getServer().getConsoleSender());

			Metrics metrics = new Metrics(this, 1479);
			if (metrics.isEnabled()) {
				metrics.addCustomChart(new Metrics.SimplePie("using_placeholderapi",
						() -> String.valueOf(ConfigValues.isPlaceholderAPI())));
				if (conf.getTablist().getBoolean("enabled")) {
					metrics.addCustomChart(
							new Metrics.SimplePie("tab_interval", () -> conf.getTablist().getString("interval")));
				}
				metrics.addCustomChart(
						new Metrics.SimplePie("enable_tablist", () -> conf.getTablist().getString("enabled")));
				metrics.addCustomChart(
						new Metrics.SimplePie("enable_tabname", () -> String.valueOf(ConfigValues.isTabNameEnabled())));
				if (ConfigValues.isTablistObjectiveEnabled()) {
					metrics.addCustomChart(new Metrics.SimplePie("object_type", () -> {
						switch (ConfigValues.getObjectType()) {
						case "ping":
							return "ping";
						case "health":
							return "health";
						case "custom":
							return "custom";
						default:
							return "";
						}
					}));
				}
				metrics.addCustomChart(new Metrics.SimplePie("enable_fake_players",
						() -> String.valueOf(ConfigValues.isFakePlayers())));
				metrics.addCustomChart(new Metrics.SimplePie("enable_groups",
						() -> String.valueOf(ConfigValues.isPrefixSuffixEnabled())));
			}

			if (conf.getConfig().getBoolean("logconsole")) {
				String msg = "&6&l[&5&lTab&c&lList&6&l]&7&l >&a The plugin successfully enabled&6 v"
						+ getDescription().getVersion() + "&a! (" + (System.currentTimeMillis() - load) + "ms)";
				Util.sendMsg(getServer().getConsoleSender(), colorMsg(msg));
			}
		} catch (Throwable t) {
			t.printStackTrace();
			logConsole(Level.WARNING,
					"There was an error. Please report it here:\nhttps://github.com/montlikadani/TabList/issues",
					false);
		}
	}

	@Override
	public void onDisable() {
		if (instance == null) return;

		g.cancelUpdate();

		objects.unregisterObjectiveForEveryone(ObjectTypes.HEALTH);
		objects.unregisterObjectiveForEveryone(ObjectTypes.PING);
		objects.unregisterObjectiveForEveryone(ObjectTypes.CUSTOM);

		tabManager.saveToggledTabs();
		tabManager.removeAll();

		if (fakePlayerHandler != null) {
			fakePlayerHandler.removeAllFakePlayer(false);
		}

		addBackAllHiddenPlayers();

		HandlerList.unregisterAll(this);
		getServer().getScheduler().cancelTasks(this);
		instance = null;
	}

	private void registerCommands() {
		Optional.ofNullable(getCommand("tablist")).ifPresent(tl -> {
			Commands cmds = new Commands(this);
			tl.setExecutor(cmds);
			tl.setTabCompleter(cmds);
		});

		Optional.ofNullable(getCommand("tabname")).ifPresent(tName -> {
			TabNameCmd cmd = new TabNameCmd(this);
			tName.setExecutor(cmd);
			tName.setTabCompleter(cmd);
		});
	}

	void loadListeners() {
		HandlerList.unregisterAll(this);

		getServer().getPluginManager().registerEvents(new Listeners(this), this);

		if (isPluginEnabled("Essentials")) {
			getServer().getPluginManager().registerEvents(new EssAfkStatus(), this);
		}

		if (isPluginEnabled("CMI")) {
			getServer().getPluginManager().registerEvents(new CMIAfkStatus(), this);
		}

		if (isPluginEnabled("ProtocolLib")) {
			ProtocolPackets.onSpectatorChange();
		}
	}

	public void reload() {
		tabManager.removeAll();
		g.cancelUpdate();

		loadListeners();
		conf.loadFiles();
		loadAnimations();
		loadValues();

		g.load();

		getServer().getOnlinePlayers().forEach(pl -> updateAll(pl, true));
	}

	private void loadValues() {
		tabRefreshTime = conf.getTablist().getInt("interval", 4);

		variables.loadExpressions();
		tabNameHandler.loadRestrictedNames();
	}

	private void loadAnimations() {
		animations.clear();

		FileConfiguration c = conf.getAnimCreator();
		if (!c.contains("animations")) {
			return;
		}

		for (String ac : c.getConfigurationSection("animations").getKeys(false)) {
			String path = "animations." + ac + ".";
			List<String> t = c.getStringList(path + "texts");
			if (!t.isEmpty()) {
				if (c.getInt(path + "interval", 200) < 0) {
					animations.add(new AnimCreator(ac, new ArrayList<>(t), c.getBoolean(path + "random")));
				} else {
					animations.add(new AnimCreator(ac, new ArrayList<>(t), c.getInt(path + "interval"),
							c.getBoolean(path + "random")));
				}
			}
		}
	}

	private boolean initVaultPerm() {
		return isPluginEnabled("Vault") && (vaultPermission = new VaultPermission()).getPermission() != null;
	}

	public String makeAnim(String name) {
		if (name == null) {
			return "";
		}

		while (name.contains("%anim:")) { // when using multiple animations
			synchronized (animations) {
				for (AnimCreator ac : animations) {
					name = name.replace("%anim:" + ac.getAnimName() + "%",
							ac.getTime() > 0 ? ac.getRandomText() : ac.getFirstText());
				}
			}
		}

		return name;
	}

	public void updateAll(Player p) {
		updateAll(p, false);
	}

	void updateAll(Player p, boolean reload) {
		if (!ConfigValues.isTablistObjectiveEnabled()) {
			for (ObjectTypes t : ObjectTypes.values()) {
				objects.unregisterObjectiveForEveryone(t);
			}
		} else if (!reload) {
			objects.unregisterObjective(objects.getObject(p, ObjectTypes.PING));
			objects.unregisterObjective(objects.getObject(p, ObjectTypes.CUSTOM));

			switch (ConfigValues.getObjectType().toLowerCase()) {
			case "ping":
			case "custom":
				objects.unregisterObjectiveForEveryone(ObjectTypes.HEALTH);

				if (objects.isCancelled()) {
					objects.startTask();
				}

				break;
			case "health":
				objects.registerHealthTab(p);
				break;
			default:
				break;
			}
		}

		if (reload) {
			fakePlayerHandler.removeAllFakePlayer(false);
			fakePlayerHandler.load();
		}

		if (ConfigValues.isHidePlayersFromTab()) {
			HidePlayers h = hidePlayers.getOrDefault(p, new HidePlayers());
			if (!hidePlayers.containsKey(p)) {
				hidePlayers.put(p, h);
			}

			h.removePlayerFromTab(p);
		} else {
			if (hidePlayers.containsKey(p)) {
				hidePlayers.get(p).addPlayerToTab();
				hidePlayers.remove(p);
			}

			if (ConfigValues.isPerWorldPlayerList()) {
				PlayerList.hideShow(p);
				PlayerList.hideShow();
			} else {
				PlayerList.showEveryone(p);
			}
		}

		tabNameHandler.loadTabName(p);
		tabManager.addPlayer(p);
	}

	public void onPlayerQuit(Player p) {
		if (ConfigValues.isTabNameEnabled() && ConfigValues.isClearTabNameOnQuit()
				&& conf.getNames().contains("players." + p.getName() + ".tabname")) {
			tabNameHandler.unSetTabName(p);
		}

		if (!ConfigValues.isTablistObjectiveEnabled()) {
			for (ObjectTypes t : ObjectTypes.values()) {
				objects.unregisterObjectiveForEveryone(t);
			}
		}

		if (hidePlayers.containsKey(p)) {
			hidePlayers.remove(p);
		}

		tabManager.removePlayer(p);
		g.removePlayerGroup(p);
	}

	void addBackAllHiddenPlayers() {
		hidePlayers.values().forEach(HidePlayers::addPlayerToTab);
		hidePlayers.clear();
	}

	public String getMsg(String key, Object... placeholders) {
		if (!conf.getMessagesFile().exists())
			return "FILENF";

		String msg = "";

		if (!conf.getMessages().contains(key)) {
			conf.getMessages().set(key, "");
			try {
				conf.getMessages().save(getConf().getMessagesFile());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (conf.getMessages().getString(key).isEmpty()) {
			return msg;
		}

		msg = colorMsg(conf.getMessages().getString(key));

		for (int i = 0; i < placeholders.length; i++) {
			if (placeholders.length >= i + 2) {
				msg = msg.replace(String.valueOf(placeholders[i]), String.valueOf(placeholders[i + 1]));
			}

			i++;
		}

		return msg;
	}

	public Map<Player, HidePlayers> getHidePlayers() {
		return hidePlayers;
	}

	public boolean isSpigot() {
		return isSpigot;
	}

	public boolean hasVault() {
		return hasVault;
	}

	public Variables getPlaceholders() {
		return variables;
	}

	public Set<AnimCreator> getAnimations() {
		return animations;
	}

	public FakePlayerHandler getFakePlayerHandler() {
		return fakePlayerHandler;
	}

	public TabManager getTabManager() {
		return tabManager;
	}

	public TabNameHandler getTabNameHandler() {
		return tabNameHandler;
	}

	public Groups getGroups() {
		return g;
	}

	public Objects getObjects() {
		return objects;
	}

	public Configuration getConf() {
		return conf;
	}

	public VaultPermission getVaultPerm() {
		return vaultPermission;
	}

	ServerVersion getMCVersion() {
		return mcVersion;
	}

	public int getTabRefreshTime() {
		return tabRefreshTime;
	}

	/**
	 * Gets this class instance
	 * 
	 * @return {@link TabList}
	 */
	public static TabList getInstance() {
		return instance;
	}

	public boolean isPluginEnabled(String name) {
		return getServer().getPluginManager().getPlugin(name) != null
				&& getServer().getPluginManager().isPluginEnabled(name);
	}

	public File getFolder() {
		File dataFolder = getDataFolder();
		if (!dataFolder.exists()) {
			dataFolder.mkdir();
		}

		return dataFolder;
	}
}
