package hu.montlikadani.tablist.bukkit;

import static hu.montlikadani.tablist.bukkit.utils.Util.colorMsg;
import static hu.montlikadani.tablist.bukkit.utils.Util.logConsole;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import hu.montlikadani.tablist.AnimCreator;
import hu.montlikadani.tablist.bukkit.commands.Commands;
import hu.montlikadani.tablist.bukkit.commands.TabNameCmd;
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
import hu.montlikadani.tablist.bukkit.utils.ServerVersion.Version;
import net.milkbowl.vault.permission.Permission;

public class TabList extends JavaPlugin {

	private static TabList instance;

	private static Permission perm = null;

	private Objects objects;
	private Variables variables;
	private Groups g;
	private ServerVersion mcVersion;
	private Configuration conf;
	private TabManager tabManager;
	private FakePlayerHandler fakePlayerHandler;
	private TabNameHandler tabNameHandler;

	private boolean isSpigot = false;
	private int tabRefreshTime = 0;

	private final Set<AnimCreator> animations = new HashSet<>();
	private final Map<Player, HidePlayers> hidePlayers = new HashMap<>();

	@Override
	public void onEnable() {
		long load = System.currentTimeMillis();

		instance = this;

		try {
			try { // Supports paper
				Class.forName("org.spigotmc.SpigotConfig");
				isSpigot = true;
			} catch (ClassNotFoundException c) {
				isSpigot = false;
			}

			mcVersion = new ServerVersion();

			if (Version.isCurrentLower(Version.v1_8_R1)) {
				logConsole(Level.SEVERE,
						"Your server version does not supported by this plugin! Please use 1.8+ or higher versions!",
						false);
				getServer().getPluginManager().disablePlugin(this);
				return;
			}

			init();

			conf.loadFiles();
			loadValues();

			if (ConfigValues.isPlaceholderAPI() && isPluginEnabled("PlaceholderAPI")) {
				logConsole("Hooked PlaceholderAPI version: "
						+ me.clip.placeholderapi.PlaceholderAPIPlugin.getInstance().getDescription().getVersion());
			}

			if (isPluginEnabled("Vault")) {
				initVaultPerm();
			}

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
				if (getTabC().getBoolean("enabled")) {
					metrics.addCustomChart(
							new Metrics.SimplePie("tab_interval", () -> getTabC().getString("interval")));
				}
				metrics.addCustomChart(new Metrics.SimplePie("enable_tablist", () -> getTabC().getString("enabled")));
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

			if (getC().getBoolean("logconsole")) {
				String msg = "&6&l[&5&lTab&c&lList&6&l]&7&l >&a The plugin successfully enabled&6 v"
						+ getDescription().getVersion() + "&a! (" + (System.currentTimeMillis() - load) + "ms)";
				Util.sendMsg(getServer().getConsoleSender(), colorMsg(msg));
			}
		} catch (Throwable e) {
			e.printStackTrace();
			logConsole(Level.WARNING,
					"There was an error. Please report it here:\nhttps://github.com/montlikadani/TabList/issues",
					false);
		}
	}

	@Override
	public void onDisable() {
		if (instance == null) return;

		try {
			g.cancelUpdate();

			objects.unregisterHealthObjective();
			objects.unregisterPingTab();
			objects.unregisterCustomValue();

			tabManager.saveToggledTabs();
			tabManager.removePlayer();

			addBackAllHiddenPlayers();

			if (fakePlayerHandler != null) {
				fakePlayerHandler.removeAllFakePlayer();
			}

			HandlerList.unregisterAll(this);

			getServer().getScheduler().cancelTasks(this);

			instance = null;
		} catch (Exception e) {
			e.printStackTrace();
			logConsole(Level.WARNING,
					"There was an error. Please report it here:\nhttps://github.com/montlikadani/TabList/issues",
					false);
		}
	}

	private void init() {
		conf = new Configuration(this);
		objects = new Objects();
		g = new Groups(this);
		variables = new Variables(this);
		tabManager = new TabManager(this);
		fakePlayerHandler = new FakePlayerHandler(this);
		tabNameHandler = new TabNameHandler(this);
	}

	private void registerCommands() {
		Commands cmds = new Commands(this);
		getCommand("tablist").setExecutor(cmds);
		getCommand("tablist").setTabCompleter(cmds);

		TabNameCmd tname = new TabNameCmd(this);
		getCommand("tabname").setExecutor(tname);
		getCommand("tabname").setTabCompleter(tname);
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
		tabManager.removePlayer();
		g.cancelUpdate();

		loadListeners();
		conf.loadFiles();
		loadAnimations();
		loadValues();

		g.load();

		getServer().getOnlinePlayers().forEach(pl -> updateAll(pl, true));
	}

	private void loadValues() {
		this.tabRefreshTime = getTabC().getInt("interval", 4);
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
		org.bukkit.plugin.RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager()
				.getRegistration(Permission.class);
		perm = rsp == null ? null : rsp.getProvider();
		return perm != null;
	}

	public String makeAnim(String name) {
		if (name == null) {
			return "";
		}

		for (AnimCreator ac : animations) {
			name = name.replace("%anim:" + ac.getAnimName() + "%",
					ac.getTime() > 0 ? ac.getRandomText() : ac.getFirstText());
		}

		return name;
	}

	public void updateAll(Player p) {
		updateAll(p, false);
	}

	void updateAll(Player p, boolean reload) {
		if (!ConfigValues.isTablistObjectiveEnabled()) {
			objects.unregisterPingTab();
			objects.unregisterCustomValue();
			objects.unregisterHealthObjective();
		} else if (!reload) {
			objects.unregisterPingTab(p);
			objects.unregisterCustomValue(p);

			switch (ConfigValues.getObjectType().toLowerCase()) {
			case "ping":
			case "custom":
				objects.unregisterHealthObjective();

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

		if (ConfigValues.isHidePlayersFromTab()) {
			HidePlayers h;
			if (!hidePlayers.containsKey(p)) {
				h = new HidePlayers(p);
				hidePlayers.put(p, h);
			} else {
				h = hidePlayers.get(p);
			}

			if (h != null) {
				h.removePlayerFromTab();
			}
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
			objects.unregisterHealthObjective();
			objects.unregisterPingTab();
			objects.unregisterCustomValue();
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

		if (!getMsgs().contains(key)) {
			getMsgs().set(key, "");
			try {
				getMsgs().save(getConf().getMessagesFile());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (getMsgs().getString(key).isEmpty()) {
			return msg;
		}

		msg = colorMsg(getMsgs().getString(key));

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

	public FileConfiguration getC() {
		return conf.getConfig();
	}

	public FileConfiguration getTabC() {
		return conf.getTablist();
	}

	public FileConfiguration getMsgs() {
		return conf.getMessages();
	}

	public FileConfiguration getGS() {
		return conf.getGroups();
	}

	public Permission getVaultPerm() {
		return perm;
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
