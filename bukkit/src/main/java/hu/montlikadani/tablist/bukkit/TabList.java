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
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;

import org.bstats.bukkit.Metrics;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.reflect.TypeToken;

import hu.montlikadani.tablist.AnimCreator;
import hu.montlikadani.tablist.bukkit.Objects.ObjectTypes;
import hu.montlikadani.tablist.bukkit.commands.Commands;
import hu.montlikadani.tablist.bukkit.config.CommentedConfig;
import hu.montlikadani.tablist.bukkit.config.ConfigValues;
import hu.montlikadani.tablist.bukkit.config.Configuration;
import hu.montlikadani.tablist.bukkit.listeners.Listeners;
import hu.montlikadani.tablist.bukkit.listeners.plugins.CMIAfkStatus;
import hu.montlikadani.tablist.bukkit.listeners.plugins.EssAfkStatus;
import hu.montlikadani.tablist.bukkit.tablist.TabManager;
import hu.montlikadani.tablist.bukkit.tablist.fakeplayers.FakePlayerHandler;
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

	private boolean isSpigot = false;
	private boolean hasVault = false;
	private int tabRefreshTime = 0;

	private final Set<AnimCreator> animations = new HashSet<>();
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

			beginDataCollection();

			if (getConfig().get("logconsole", false)) {
				Util.sendMsg(getServer().getConsoleSender(), colorMsg("&6&l[&5&lTab&c&lList&6&l]&7&l >&a Enabled&6 v"
						+ getDescription().getVersion() + "&a! (" + (System.currentTimeMillis() - load) + "ms)"));
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

		for (ObjectTypes ot : ObjectTypes.values()) {
			objects.unregisterObjectiveForEveryone(ot);
		}

		tabManager.saveToggledTabs();
		tabManager.removeAll();

		if (fakePlayerHandler != null) {
			fakePlayerHandler.removeAllFakePlayer(false);
		}

		addBackAllHiddenPlayers();

		HandlerList.unregisterAll(this);
		getServer().getScheduler().cancelTasks(this);

		// Async tasks can't be cancelled sometimes, with this we forcedly interrupts
		// the active ones
		for (org.bukkit.scheduler.BukkitWorker worker : getServer().getScheduler().getActiveWorkers()) {
			if (worker.getOwner() == this && !worker.getThread().isInterrupted()) {
				worker.getThread().interrupt();
			}
		}

		instance = null;
	}

	@Override
	public CommentedConfig getConfig() {
		return conf.getConfig();
	}

	private void beginDataCollection() {
		Metrics metrics = new Metrics(this, 1479);

		metrics.addCustomChart(new org.bstats.charts.SimplePie("using_placeholderapi",
				() -> String.valueOf(ConfigValues.isPlaceholderAPI())));

		if (conf.getTablist().getBoolean("enabled")) {
			metrics.addCustomChart(
					new org.bstats.charts.SimplePie("tab_interval", () -> conf.getTablist().getString("interval")));
		}

		metrics.addCustomChart(
				new org.bstats.charts.SimplePie("enable_tablist", () -> conf.getTablist().getString("enabled")));

		if (ConfigValues.isTablistObjectiveEnabled()) {
			metrics.addCustomChart(new org.bstats.charts.SimplePie("object_type",
					objects.getCurrentObjectType().toString()::toLowerCase));
		}

		metrics.addCustomChart(new org.bstats.charts.SimplePie("enable_fake_players",
				() -> String.valueOf(ConfigValues.isFakePlayers())));

		metrics.addCustomChart(new org.bstats.charts.SimplePie("enable_groups",
				() -> String.valueOf(ConfigValues.isPrefixSuffixEnabled())));
	}

	private void registerCommands() {
		Optional.ofNullable(getCommand("tablist")).ifPresent(tl -> {
			Commands cmds = new Commands(this);
			tl.setExecutor(cmds);
			tl.setTabCompleter(cmds);
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

		while (!animations.isEmpty() && name.contains("%anim:")) { // when using multiple animations
			for (AnimCreator ac : animations) {
				name = name.replace("%anim:" + ac.getAnimName() + "%",
						ac.getTime() > 0 ? ac.getRandomText() : ac.getFirstText());
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
		} else {
			objects.cancelTask();

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
				if (!reload) {
					objects.registerHealthTab(p);
				}

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

		tabManager.addPlayer(p);
	}

	public void onPlayerQuit(Player p) {
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
		return getMsg(TypeToken.of(String.class), key, placeholders);
	}

	@SuppressWarnings("unchecked")
	public <T> T getMsg(TypeToken<T> type, String key, Object... placeholders) {
		if (key == null || key.trim().isEmpty()) {
			return (T) "null";
		}

		if (type.getRawType().isAssignableFrom(String.class)) {
			if (!conf.getMessagesFile().exists()) {
				return (T) "FILENF";
			}

			String msg = "";

			if (!conf.getMessages().contains(key)) {
				conf.getMessages().set(key, "");
				try {
					conf.getMessages().save(conf.getMessagesFile());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			if (conf.getMessages().getString(key).isEmpty()) {
				return (T) msg;
			}

			msg = colorMsg(conf.getMessages().getString(key));

			for (int i = 0; i < placeholders.length; i++) {
				if (placeholders.length >= i + 2) {
					msg = msg.replace(String.valueOf(placeholders[i]), String.valueOf(placeholders[i + 1]));
				}

				i++;
			}

			return (T) msg;
		}

		if (type.getRawType().isAssignableFrom(List.class)) {
			if (!conf.getMessagesFile().exists()) {
				return (T) new ArrayList<>();
			}

			List<String> list = new ArrayList<>();

			for (String one : conf.getMessages().getStringList(key)) {
				one = colorMsg(one);

				for (int i = 0; i < placeholders.length; i++) {
					if (placeholders.length >= i + 2) {
						one = one.replace(String.valueOf(placeholders[i]), String.valueOf(placeholders[i + 1]));
					}

					i++;
				}

				list.add(one);
			}

			return (T) list;
		}

		return (T) "no msg";
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
