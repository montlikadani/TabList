package hu.montlikadani.tablist.bukkit;

import static hu.montlikadani.tablist.bukkit.utils.Util.colorMsg;
import static hu.montlikadani.tablist.bukkit.utils.Util.logConsole;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.apache.commons.lang.StringUtils;
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
import hu.montlikadani.tablist.bukkit.config.Configuration;
import hu.montlikadani.tablist.bukkit.config.constantsLoader.ConfigValues;
import hu.montlikadani.tablist.bukkit.config.constantsLoader.TabConfigValues;
import hu.montlikadani.tablist.bukkit.listeners.Listeners;
import hu.montlikadani.tablist.bukkit.listeners.plugins.CMIAfkStatus;
import hu.montlikadani.tablist.bukkit.listeners.plugins.EssAfkStatus;
import hu.montlikadani.tablist.bukkit.tablist.TabManager;
import hu.montlikadani.tablist.bukkit.tablist.fakeplayers.FakePlayerHandler;
import hu.montlikadani.tablist.bukkit.tablist.groups.Groups;
import hu.montlikadani.tablist.bukkit.tablist.playerlist.PlayerList;
import hu.montlikadani.tablist.bukkit.user.TabListPlayer;
import hu.montlikadani.tablist.bukkit.user.TabListUser;
import hu.montlikadani.tablist.bukkit.utils.ServerVersion;
import hu.montlikadani.tablist.bukkit.utils.UpdateDownloader;
import hu.montlikadani.tablist.bukkit.utils.Util;
import hu.montlikadani.tablist.bukkit.utils.Variables;
import hu.montlikadani.tablist.bukkit.utils.plugin.VaultPermission;
import hu.montlikadani.tablist.bukkit.utils.stuff.Complement;
import hu.montlikadani.tablist.bukkit.utils.stuff.Complement1;
import hu.montlikadani.tablist.bukkit.utils.stuff.Complement2;

public final class TabList extends JavaPlugin {

	private VaultPermission vaultPermission;
	private Objects objects;
	private Variables variables;
	private Groups groups;
	private Configuration conf;
	private TabManager tabManager;
	private FakePlayerHandler fakePlayerHandler;
	private Complement complement;

	private boolean isPaper = false, hasVault = false;

	private final Set<AnimCreator> animations = new HashSet<>();
	private final Set<TabListUser> users = Collections.newSetFromMap(new ConcurrentHashMap<>());

	@Override
	public void onEnable() {
		long load = System.currentTimeMillis();

		try {
			if (ServerVersion.isCurrentLower(ServerVersion.v1_8_R1)) {
				logConsole(Level.SEVERE,
						"Your server version does not supported by this plugin! Please use 1.8+ or higher versions!");
				getServer().getPluginManager().disablePlugin(this);
				return;
			}

			verifyServerSoftware();

			conf = new Configuration(this);
			objects = new Objects(this);
			groups = new Groups(this);
			variables = new Variables(this);
			tabManager = new TabManager(this);
			fakePlayerHandler = new FakePlayerHandler(this);

			conf.loadFiles();
			variables.loadExpressions();

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
			groups.load();

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
					"There was an error. Please report it here:\nhttps://github.com/montlikadani/TabList/issues");
		}
	}

	@Override
	public void onDisable() {
		groups.cancelUpdate();
		objects.cancelTask();

		for (ObjectTypes ot : ObjectTypes.values()) {
			objects.unregisterObjectiveForEveryone(ot);
		}

		tabManager.saveToggledTabs();
		tabManager.removeAll();

		fakePlayerHandler.removeAllFakePlayer();

		addBackAllHiddenPlayers();
		conf.deleteEmptyFiles();

		HandlerList.unregisterAll(this);
		getServer().getScheduler().cancelTasks(this);
		users.clear();

		// Async tasks can't be cancelled sometimes, with this we forcedly interrupts
		// the active ones
		for (org.bukkit.scheduler.BukkitWorker worker : getServer().getScheduler().getActiveWorkers()) {
			if (equals(worker.getOwner()) && !worker.getThread().isInterrupted()) {
				worker.getThread().interrupt();
			}
		}
	}

	@Override
	public CommentedConfig getConfig() {
		return conf.getConfig();
	}

	@Override
	public void saveConfig() {
		getConfig().save();
	}

	private void verifyServerSoftware() {
		try {
			Class.forName("com.destroystokyo.paper.PaperConfig");
			isPaper = true;
		} catch (ClassNotFoundException e) {
			isPaper = false;
		}

		boolean kyoriSupported = false;
		try {
			Class.forName("net.kyori.adventure.text.Component");
			kyoriSupported = true;
		} catch (ClassNotFoundException e) {
		}

		complement = (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_16_R3) && kyoriSupported) ? new Complement2()
				: new Complement1();
	}

	private void beginDataCollection() {
		Metrics metrics = new Metrics(this, 1479);

		metrics.addCustomChart(new org.bstats.charts.SimplePie("using_placeholderapi",
				() -> Boolean.toString(ConfigValues.isPlaceholderAPI())));

		if (TabConfigValues.isEnabled()) {
			metrics.addCustomChart(new org.bstats.charts.SimplePie("tab_interval",
					() -> Integer.toString(TabConfigValues.getUpdateInterval())));
		}

		metrics.addCustomChart(
				new org.bstats.charts.SimplePie("enable_tablist", () -> Boolean.toString(TabConfigValues.isEnabled())));

		if (ConfigValues.isTablistObjectiveEnabled()) {
			metrics.addCustomChart(new org.bstats.charts.SimplePie("object_type",
					objects.getCurrentObjectType().toString()::toLowerCase));
		}

		metrics.addCustomChart(new org.bstats.charts.SimplePie("enable_fake_players",
				() -> Boolean.toString(ConfigValues.isFakePlayers())));

		metrics.addCustomChart(new org.bstats.charts.SimplePie("enable_groups",
				() -> Boolean.toString(ConfigValues.isPrefixSuffixEnabled())));
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
		groups.cancelUpdate();
		fakePlayerHandler.removeAllFakePlayer();

		loadListeners();
		conf.loadFiles();
		loadAnimations();
		variables.loadExpressions();
		fakePlayerHandler.load();
		groups.load();

		getServer().getOnlinePlayers().forEach(pl -> updateAll(pl, true));
	}

	private void loadAnimations() {
		animations.clear();

		FileConfiguration c = conf.getAnimCreator();
		if (!c.isConfigurationSection("animations")) {
			return;
		}

		for (String name : c.getConfigurationSection("animations").getKeys(false)) {
			String path = "animations." + name + ".";
			List<String> list = c.getStringList(path + "texts");
			if (!list.isEmpty()) {
				if (c.getInt(path + "interval", 200) < 0) {
					animations.add(new AnimCreator(name, list, c.getBoolean(path + "random")));
				} else {
					animations.add(
							new AnimCreator(name, list, c.getInt(path + "interval"), c.getBoolean(path + "random")));
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
				name = StringUtils.replace(name, "%anim:" + ac.getAnimName() + "%",
						ac.getTime() > 0 ? ac.getRandomText() : ac.getFirstText());
			}
		}

		return name;
	}

	public void updateAll(Player p) {
		updateAll(p, false);
	}

	void updateAll(Player p, boolean reload) {
		TabListUser user = getUser(p.getUniqueId()).orElseGet(() -> {
			TabListUser tlu = new TabListPlayer(this, p.getUniqueId());
			users.add(tlu);
			return tlu;
		});

		if (!ConfigValues.isTablistObjectiveEnabled()) {
			objects.cancelTask();

			for (ObjectTypes t : ObjectTypes.values()) {
				objects.unregisterObjectiveForEveryone(t);
			}
		} else {
			objects.unregisterObjective(objects.getObject(p.getScoreboard(), ObjectTypes.PING));
			objects.unregisterObjective(objects.getObject(p.getScoreboard(), ObjectTypes.CUSTOM));

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

		if (ConfigValues.isFakePlayers()) {
			fakePlayerHandler.display(p);
		}

		tabManager.addPlayer(user);
		groups.startTask();

		if (ConfigValues.isHidePlayersFromTab()) {
			user.setHidden(true);
		} else {
			user.setHidden(false);

			if (ConfigValues.isPerWorldPlayerList()) {
				PlayerList.hideShow(p);
				PlayerList.hideShow();
			} else {
				PlayerList.showEveryone(p);
			}
		}
	}

	public void onPlayerQuit(Player p) {
		if (!ConfigValues.isTablistObjectiveEnabled()) {
			objects.cancelTask();

			for (ObjectTypes t : ObjectTypes.values()) {
				objects.unregisterObjectiveForEveryone(t);
			}
		}

		users.removeIf(user -> {
			tabManager.removePlayer(user);
			groups.removePlayerGroup(user);
			return p.getUniqueId().equals(user.getUniqueId());
		});
	}

	void addBackAllHiddenPlayers() {
		users.forEach(tlu -> tlu.setHidden(false));
	}

	public String getMsg(String key, Object... placeholders) {
		return getMsg(TypeToken.of(String.class), key, placeholders);
	}

	@SuppressWarnings("unchecked")
	public <T> T getMsg(TypeToken<T> type, String key, Object... placeholders) {
		if (key == null || key.isEmpty()) {
			return (T) "null";
		}

		if (type.getRawType().isAssignableFrom(String.class)) {
			if (conf.getMessages().getString(key).isEmpty()) {
				return (T) "";
			}

			String msg = colorMsg(conf.getMessages().getString(key));

			for (int i = 0; i < placeholders.length; i++) {
				if (placeholders.length >= i + 2) {
					msg = msg.replace(String.valueOf(placeholders[i]), String.valueOf(placeholders[i + 1]));
				}

				i++;
			}

			return (T) msg;
		}

		if (type.getRawType().isAssignableFrom(List.class)) {
			List<String> list = new ArrayList<>();

			for (String one : conf.getMessages().getStringList(key)) {
				one = colorMsg(one);

				for (int i = 0; i < placeholders.length; i++) {
					if (placeholders.length >= i + 2) {
						one = StringUtils.replace(one, String.valueOf(placeholders[i]),
								String.valueOf(placeholders[i + 1]));
					}

					i++;
				}

				list.add(one);
			}

			return (T) list;
		}

		return (T) "no msg";
	}

	public Optional<TabListUser> getUser(Player player) {
		return player == null ? Optional.empty() : getUser(player.getUniqueId());
	}

	public Optional<TabListUser> getUser(UUID uuid) {
		if (uuid == null) {
			return Optional.empty();
		}

		for (TabListUser tlp : users) {
			if (uuid.equals(tlp.getUniqueId())) {
				return Optional.of(tlp);
			}
		}

		return Optional.empty();
	}

	public Set<TabListUser> getUsers() {
		return users;
	}

	public boolean hasVault() {
		return hasVault;
	}

	public boolean isPaper() {
		return isPaper;
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
		return groups;
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

	public Complement getComplement() {
		return complement;
	}

	public boolean isPluginEnabled(String name) {
		return getServer().getPluginManager().getPlugin(name) != null
				&& getServer().getPluginManager().isPluginEnabled(name);
	}

	public File getFolder() {
		File dataFolder = getDataFolder();
		dataFolder.mkdirs();
		return dataFolder;
	}
}
