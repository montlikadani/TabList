package hu.montlikadani.tablist.bukkit.tablist;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.config.constantsLoader.TabConfigValues;
import hu.montlikadani.tablist.bukkit.tablist.entry.TabEntries;
import hu.montlikadani.tablist.bukkit.user.TabListUser;
import hu.montlikadani.tablist.bukkit.utils.task.Tasks;

public class TabManager {

	public static final Map<UUID, Boolean> TABENABLED = new HashMap<>();

	private TabList plugin;
	private BukkitTask task;

	private final TabEntries tabEntries;

	public TabManager(TabList plugin) {
		this.plugin = plugin;
		tabEntries = new TabEntries(plugin);
	}

	public BukkitTask getTask() {
		return task;
	}

	public void cancelTask() {
		if (task != null) {
			task.cancel();
			task = null;
		}
	}

	public TabEntries getTabEntries() {
		return tabEntries;
	}

	public void addPlayer(TabListUser user) {
		tabEntries.beginUpdate(user);

		if (!TabConfigValues.isEnabled()) {
			return;
		}

		user.getTabHandler().updateTab();

		final int refreshTime = TabConfigValues.getUpdateInterval();
		if (refreshTime < 1) {
			user.getTabHandler().sendTab();
			return;
		}

		if (task == null) {
			task = Tasks.submitAsync(() -> {
				if (plugin.getUsers().isEmpty()) {
					cancelTask();
					return;
				}

				for (TabListUser u : plugin.getUsers()) {
					u.getTabHandler().sendTab();
				}
			}, refreshTime, refreshTime);
		}
	}

	public void removePlayer(TabListUser user) {
		TabTitle.sendTabTitle(user.getPlayer(), "", "");
		tabEntries.removePlayer(user.getUniqueId());
	}

	public void removeAll() {
		cancelTask();

		for (TabListUser user : plugin.getUsers()) {
			TabTitle.sendTabTitle(user.getPlayer(), "", "");
		}

		tabEntries.removeAll();
	}

	public void loadToggledTabs() {
		TABENABLED.clear();

		if (!TabConfigValues.isRememberToggledTablistToFile()) {
			return;
		}

		File f = new File(plugin.getFolder(), "toggledtablists.yml");
		if (!f.exists()) {
			return;
		}

		FileConfiguration t = YamlConfiguration.loadConfiguration(f);
		org.bukkit.configuration.ConfigurationSection section = t.getConfigurationSection("tablists");
		if (section == null) {
			return;
		}

		for (String uuid : section.getKeys(false)) {
			TABENABLED.put(UUID.fromString(uuid), section.getBoolean(uuid));
		}

		t.set("tablists", null);
		try {
			t.save(f);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void saveToggledTabs() {
		File f = new File(plugin.getFolder(), "toggledtablists.yml");

		if (!TabConfigValues.isRememberToggledTablistToFile() || TABENABLED.isEmpty()) {
			if (f.exists()) {
				f.delete();
			}

			return;
		}

		if (!f.exists()) {
			try {
				f.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		FileConfiguration t = YamlConfiguration.loadConfiguration(f);
		t.set("tablists", null);

		for (Map.Entry<UUID, Boolean> list : TABENABLED.entrySet()) {
			if (list.getValue()) {
				t.set("tablists." + list.getKey(), list.getValue());
			}
		}

		try {
			t.save(f);
		} catch (IOException e) {
			e.printStackTrace();
		}

		TABENABLED.clear();
	}
}
