package hu.montlikadani.tablist.tablist;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.config.constantsLoader.TabConfigValues;

public final class TabToggleBase {

	/**
	 * This property stores of toggled tablists for specific players
	 */
	public static final java.util.Set<UUID> TAB_TOGGLE = new java.util.HashSet<>();

	/**
	 * Holds the information that the tablist is globally (for every online player)
	 * disabled or not.
	 */
	public static boolean globallySwitched = false;

	private final TabList tl;

	/**
	 * Checks if tablist is disabled globally (for everyone) or for the specified
	 * UUID's one.
	 * 
	 * @param id the target {@link UUID}
	 * @return true if globally or for the specific uuid's is disabled, otherwise
	 *         false.
	 */
	public static boolean isDisabled(UUID id) {
		return globallySwitched || TAB_TOGGLE.contains(id);
	}

	public TabToggleBase(TabList tl) {
		this.tl = tl;
	}

	public void loadToggledTabs() {
		TAB_TOGGLE.clear();

		if (!TabConfigValues.isRememberToggledTablistToFile()) {
			return;
		}

		File file = new File(tl.getFolder(), "toggledtablists.yml");
		if (!file.exists()) {
			return;
		}

		FileConfiguration config = YamlConfiguration.loadConfiguration(file);

		if (!(globallySwitched = config.getBoolean("globallySwitched", false))) {
			org.bukkit.configuration.ConfigurationSection section = config.getConfigurationSection("tablists");

			if (section == null) {
				return;
			}

			for (String uuid : section.getKeys(false)) {
				if (!section.getBoolean(uuid, false)) {
					continue;
				}

				try {
					TAB_TOGGLE.add(UUID.fromString(uuid));
				} catch (IllegalArgumentException e) {
				}
			}
		} else {
			config.set("tablists", null);

			try {
				config.save(file);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void saveToggledTabs() {
		File file = new File(tl.getFolder(), "toggledtablists.yml");

		if (!TabConfigValues.isRememberToggledTablistToFile() || (!globallySwitched && TAB_TOGGLE.isEmpty())) {
			if (file.exists()) {
				file.delete();
			}

			return;
		}

		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		FileConfiguration config = YamlConfiguration.loadConfiguration(file);
		config.set("tablists", null);

		if (globallySwitched) {
			config.set("globallySwitched", globallySwitched);
		} else {
			for (UUID key : TAB_TOGGLE) {
				config.set("tablists." + key, false);
			}

			config.set("globallySwitched", null);
		}

		try {
			config.save(file);
		} catch (IOException e) {
			e.printStackTrace();
		}

		TAB_TOGGLE.clear();
	}
}
