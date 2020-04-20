package hu.montlikadani.tablist.bukkit;

import static hu.montlikadani.tablist.bukkit.utils.Util.colorMsg;
import static hu.montlikadani.tablist.bukkit.utils.Util.logConsole;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;

import hu.montlikadani.ragemode.gameUtils.GameUtils;
import hu.montlikadani.tablist.AnimCreator;
import hu.montlikadani.tablist.Global;
import hu.montlikadani.tablist.bukkit.commands.Commands;
import hu.montlikadani.tablist.bukkit.commands.TabNameCmd;
import hu.montlikadani.tablist.bukkit.listeners.EssAfkStatus;
import hu.montlikadani.tablist.bukkit.listeners.Listeners;
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

	private Objects objects = null;
	private Variables variables = null;
	private Groups g = null;
	private ServerVersion mcVersion = null;
	private Configuration conf = null;
	private TabHandler tabHandler = null;

	private boolean papi = false;
	private boolean oldTab = false;
	private boolean isSpigot = false;

	private final Set<FakePlayers> fpList = new HashSet<>();
	private final Set<AnimCreator> animations = new HashSet<>();

	private final Map<Player, HidePlayers> hidePlayers = new HashMap<>();

	@Override
	public void onEnable() {
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

			if (papi) {
				if (isPluginEnabled("PlaceholderAPI")) {
					logConsole("Hooked PlaceholderAPI version: "
							+ me.clip.placeholderapi.PlaceholderAPIPlugin.getInstance().getDescription().getVersion());
				} else {
					logConsole(Level.WARNING, "Could not find PlaceholderAPI!");
					logConsole("PlaceholderAPI Download: https://www.spigotmc.org/resources/6245/");
				}
			}

			if (isPluginEnabled("Vault")) {
				initVaultPerm();
			}

			loadFakePlayers();
			loadAnimations();
			loadListeners();
			registerCommands();

			tabHandler.loadToggledTabs();
			g.load();

			getServer().getOnlinePlayers().forEach(this::updateAll);

			if (oldTab) {
				logConsole(
						"Seems you still using the deprecated (old) tablist. The new tablist moved to a new file named with tablist.yml.");
				logConsole("It's very important to move your tablist settings to the new file to prevent any issues.");
			}

			logConsole(UpdateDownloader.checkFromGithub("console"));

			Metrics metrics = new Metrics(this, 1479);
			if (metrics.isEnabled()) {
				metrics.addCustomChart(new Metrics.SimplePie("using_placeholderapi", () -> String.valueOf(papi)));
				metrics.addCustomChart(new Metrics.SimplePie("tab_interval", () -> getTabC().getString("interval")));
				metrics.addCustomChart(new Metrics.SimplePie("enable_tablist", () -> getTabC().getString("enabled")));
				metrics.addCustomChart(
						new Metrics.SimplePie("enable_tabname", () -> getC().getString("tabname.enable")));
				if (getC().getBoolean("tablist-object-type.enable")) {
					metrics.addCustomChart(
							new Metrics.SimplePie("object_type", () -> getC().getString("tablist-object-type.type")));
				}
				metrics.addCustomChart(
						new Metrics.SimplePie("enable_fake_players", () -> getC().getString("enable-fake-players")));
				metrics.addCustomChart(new Metrics.SimplePie("enable_groups",
						() -> getC().getString("change-prefix-suffix-in-tablist.enable")));
				logConsole("Metrics enabled.");
			}

			if (getC().getBoolean("logconsole")) {
				String msg = "&6&l[&5&lTab&c&lList&6&l]&7&l >&a The plugin successfully enabled&6 v"
						+ getDescription().getVersion() + "&a!";
				Util.sendMsg(getServer().getConsoleSender(), colorMsg(msg));
			}
		} catch (Throwable e) {
			e.printStackTrace();
			logConsole(Level.WARNING,
					"There was an error. Please report it here:\nhttps://github.com/montlikadani/TabList/issues",
					false);
			instance = null;
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
			tabHandler.saveToggledTabs();
			tabHandler.unregisterTab();
			addBackAllHiddenPlayers();
			removeAllFakePlayer();
			HandlerList.unregisterAll(this);
			getServer().getScheduler().cancelTasks(this);

			if (getC().getBoolean("logconsole")) {
				String msg = "&6&l[&5&lTab&c&lList&6&l]&7&l >&c The plugin successfully disabled!";
				Util.sendMsg(getServer().getConsoleSender(), colorMsg(msg));
			}

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
		tabHandler = new TabHandler(this);
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
	}

	public void reload() {
		tabHandler.unregisterTab();
		g.cancelUpdate();

		loadListeners();
		conf.loadFiles();
		loadAnimations();
		loadValues();

		g.load();

		getServer().getOnlinePlayers().forEach(pl -> updateAll(pl, true));
	}

	private void loadValues() {
		this.papi = getC().contains("placeholderapi") ? getC().getBoolean("placeholderapi")
				: getC().getBoolean("hook.placeholderapi", false);

		int utick = 0;
		if (getC().contains("tablist")) {
			oldTab = true;
			utick = getC().getInt("tablist.interval");
		} else {
			oldTab = false;
			utick = getTabC().getInt("interval", 4);
		}

		tabHandler.setUpdateInterval(utick);
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
					animations.add(new AnimCreator(ac, new ArrayList<String>(t), c.getBoolean(path + "random")));
				} else {
					animations.add(new AnimCreator(ac, new ArrayList<String>(t), c.getInt(path + "interval"),
							c.getBoolean(path + "random")));
				}
			}
		}
	}

	public void loadFakePlayers() {
		if (!getC().getBoolean("enable-fake-players")) {
			return;
		}

		fpList.clear();

		List<String> fpls = conf.getFakeplayers().getStringList("fakeplayers");
		for (String l : fpls) {
			FakePlayers fp = new FakePlayers(colorMsg(l));
			fpList.add(fp);

			getServer().getOnlinePlayers().forEach(fp::createFakeplayer);
		}
	}

	private boolean initVaultPerm() {
		org.bukkit.plugin.RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager()
				.getRegistration(Permission.class);
		perm = rsp == null ? null : rsp.getProvider();
		return perm != null;
	}

	public void setTabName(Player p, String name) {
		if (!getC().getBoolean("tabname.enable")) {
			return;
		}

		String result = "";
		String tName = "";

		if (getC().getBoolean("tabname.use-essentials-nickname")) {
			if (isPluginEnabled("Essentials")) {
				User user = getPlugin(Essentials.class).getUser(p);
				if (user.getNickname() != null) {
					result = colorMsg(user.getNickname());
					tName = user.getNickname();
				}
			} else {
				logConsole(Level.WARNING, "The Essentials plugin not found. Without the nickname option not work.");
				return;
			}
		} else {
			if (getC().getBoolean("tabname.default-color.enable")) {
				result = colorMsg(getC().getString("tabname.default-color.color")
						+ variables.setPlaceholders(p, Global.setSymbols(name)) + "&r");
			} else {
				result = getC().getBoolean("tabname.enable-color-code")
						? colorMsg(variables.setPlaceholders(p, Global.setSymbols(name)) + "&r")
						: name + "\u00a7r";
			}

			tName = name;
		}

		if (!result.isEmpty()) {
			p.setPlayerListName(result);
		}

		if (!tName.isEmpty()) {
			conf.getNames().set("players." + p.getName() + ".tabname", tName);

			try {
				conf.getNames().save(conf.getNamesFile());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void loadTabName(Player p) {
		if (!getC().getBoolean("tabname.enable")) {
			return;
		}

		String result = "";
		if (getC().getBoolean("tabname.use-essentials-nickname")) {
			if (!isPluginEnabled("Essentials")) {
				logConsole(Level.WARNING, "The Essentials plugin not found. Without the nickname option not work.");
				return;
			}

			User user = getPlugin(Essentials.class).getUser(p);
			if (user.getNickname() != null) {
				result = colorMsg(user.getNickname() + "&r");
			}
		} else {
			String name = conf.getNames().getString("players." + p.getName() + ".tabname", "");
			if (!name.isEmpty()) {
				name = variables.setPlaceholders(p, Global.setSymbols(name));

				if (getC().getBoolean("tabname.default-color.enable")) {
					result = colorMsg(getC().getString("tabname.default-color.color") + name + "&r");
				} else {
					result = getC().getBoolean("tabname.enable-color-code") ? colorMsg(name + "&r") : name + "\u00a7r";
				}
			} else {
				if (getC().getBoolean("tabname.default-color.enable")) {
					result = colorMsg(getC().getString("tabname.default-color.color") + p.getName());
				}
			}
		}

		if (!result.isEmpty()) {
			p.setPlayerListName(result);
		}
	}

	public void unTabName(Player p) {
		if (!getC().getBoolean("tabname.enable")) {
			return;
		}

		p.setPlayerListName(p.getName());

		conf.getNames().set("players." + p.getName() + ".tabname", null);
		conf.getNames().set("players." + p.getName(), null);

		try {
			conf.getNames().save(conf.getNamesFile());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected String makeAnim(String name) {
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
		if (!getC().getBoolean("tablist-object-type.enable")) {
			objects.unregisterPingTab();
			objects.unregisterCustomValue();
			objects.unregisterHealthObjective();
		} else if (!reload) {
			objects.unregisterPingTab(p);
			objects.unregisterCustomValue(p);

			switch (getC().getString("tablist-object-type.type").toLowerCase()) {
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

		if (getC().getBoolean("hide-players-from-tablist")) {
			HidePlayers h = null;
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

			if (getC().getBoolean("per-world-player-list")) {
				PlayerList.hideShow(p);
				PlayerList.hideShow();
			} else {
				PlayerList.showEveryone(p);
			}
		}

		loadTabName(p);
		tabHandler.updateTab(p);
	}

	public boolean createPlayer(Player p, String name) {
		if (name == null || name.trim().isEmpty()) {
			return false;
		}

		if (getFakePlayerByName(name) != null) {
			return false;
		}

		List<String> fakepls = conf.getFakeplayers().getStringList("fakeplayers");
		fakepls.add(name);

		conf.getFakeplayers().set("fakeplayers", fakepls);
		try {
			conf.getFakeplayers().save(conf.getFakeplayersFile());
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		name = colorMsg(name);

		FakePlayers fp = new FakePlayers(name);
		fpList.add(fp);

		fp.createFakeplayer(p);
		return true;
	}

	void removeAllFakePlayer() {
		for (FakePlayers fp : fpList) {
			if (fp != null) {
				fp.removeFakePlayer();
			}
		}

		fpList.clear();
	}

	public boolean removePlayer(String name) {
		if (name == null || name.trim().isEmpty()) {
			return false;
		}

		List<String> fakepls = conf.getFakeplayers().getStringList("fakeplayers");
		fakepls.remove(name);

		conf.getFakeplayers().set("fakeplayers", fakepls);
		try {
			conf.getFakeplayers().save(conf.getFakeplayersFile());
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		for (Iterator<FakePlayers> it = fpList.iterator(); it.hasNext();) {
			FakePlayers fp = it.next();
			if (fp != null && fp.getName().equalsIgnoreCase(name)) {
				fp.removeFakePlayer();
				it.remove();
				break;
			}
		}

		return true;
	}

	public void onPlayerQuit(Player p) {
		if (getC().getBoolean("tabname.enable") && getC().getBoolean("tabname.clear-player-tabname-on-quit")
				&& conf.getNames().contains("players." + p.getName() + ".tabname")) {
			unTabName(p);
		}

		if (!getC().getBoolean("tablist-object-type.enable")) {
			objects.unregisterHealthObjective();
			objects.unregisterPingTab();
			objects.unregisterCustomValue();
		}

		if (hidePlayers.containsKey(p)) {
			hidePlayers.remove(p);
		}

		tabHandler.unregisterTab(p);
		g.removePlayerGroup(p);
	}

	void addBackAllHiddenPlayers() {
		hidePlayers.entrySet().forEach(e -> e.getValue().addPlayerToTab());
		hidePlayers.clear();
	}

	public String getChangeType() {
		String path = "change-prefix-suffix-in-tablist.changing-type";
		return getC().getString(path, "").isEmpty() ? "namer" : getC().getString(path).toLowerCase();
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

	boolean isAfk(Player p, boolean log) {
		if (isPluginEnabled("Essentials")) {
			return getPlugin(Essentials.class).getUser(p).isAfk();
		}

		if (log) {
			logConsole(Level.WARNING, "The Essentials plugin not found.");
		}

		return false;
	}

	boolean isVanished(Player p, boolean log) {
		if (isPluginEnabled("Essentials")) {
			return getPlugin(Essentials.class).getUser(p).isVanished();
		}

		if (log) {
			logConsole(Level.WARNING, "The Essentials plugin not found.");
		}

		return false;
	}

	public FakePlayers getFakePlayerByName(String name) {
		for (FakePlayers fp : fpList) {
			if (fp.getName().equalsIgnoreCase(name)) {
				return fp;
			}
		}

		return null;
	}

	public Map<Player, HidePlayers> getHidePlayers() {
		return hidePlayers;
	}

	public boolean isSpigot() {
		return isSpigot;
	}

	public boolean isUsingOldTab() {
		return oldTab;
	}

	public Variables getPlaceholders() {
		return variables;
	}

	public Set<FakePlayers> getFakePlayers() {
		return fpList;
	}

	public Set<AnimCreator> getAnimations() {
		return animations;
	}

	public TabHandler getTabHandler() {
		return tabHandler;
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

	Permission getVaultPerm() {
		return perm;
	}

	ServerVersion getMCVersion() {
		return mcVersion;
	}

	public boolean hasPapi() {
		return papi;
	}

	/**
	 * Gets this class instance
	 * @return {@link TabList}
	 */
	public static TabList getInstance() {
		return instance;
	}

	public boolean isPluginEnabled(String name) {
		return getServer().getPluginManager().getPlugin(name) != null
				&& getServer().getPluginManager().isPluginEnabled(name);
	}

	public boolean isHookPreventTask(Player p) {
		if (isPluginEnabled("RageMode") && getC().getBoolean("hook.RageMode") && GameUtils.isPlayerPlaying(p)
				&& GameUtils.getGameByPlayer(p).isGameRunning()) {
			return true;
		}

		return false;
	}

	public File getFolder() {
		File dataFolder = getDataFolder();
		if (!dataFolder.exists()) {
			dataFolder.mkdir();
		}

		return dataFolder;
	}
}