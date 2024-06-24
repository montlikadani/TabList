package hu.montlikadani.tablist.tablist.fakeplayers;

import hu.montlikadani.tablist.utils.Util;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import java.util.logging.Level;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.config.constantsLoader.ConfigValues;

public final class FakePlayerHandler {

	private final TabList plugin;

	public final Set<IFakePlayer> fakePlayers = new java.util.HashSet<>();

	public FakePlayerHandler(TabList plugin) {
		this.plugin = plugin;
	}

	public Optional<IFakePlayer> getFakePlayerByName(String name) {
		if (name.isEmpty()) {
			throw new IllegalArgumentException("The fake player name can not be empty");
		}

		for (IFakePlayer fp : fakePlayers) {
			if (fp.getName().equalsIgnoreCase(name)) {
				return Optional.of(fp);
			}
		}

		return Optional.empty();
	}

	public void load() {
		fakePlayers.clear();

		if (!ConfigValues.isFakePlayers()) {
			java.io.File file = plugin.getConf().getFakeplayersFile();

			if (!file.exists()) {
				return;
			}

			if (file.length() == 0L) {
				if (!file.delete()) {
					throw new RuntimeException("Failed to delete file " + file.getName());
				}

				return;
			}

			ConfigurationSection section = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file)
					.getConfigurationSection("list");

			if (section != null && section.getKeys(false).isEmpty() && !file.delete()) {
				throw new RuntimeException("Failed to delete file " + file.getName());
			}

			return;
		}

		ConfigurationSection section = plugin.getConf().getFakeplayers().getConfigurationSection("list");

		if (section == null) {
			return;
		}

		for (String name : section.getKeys(false)) {
			FakePlayer fakePlayer = new FakePlayer(name, section.getString(name + ".displayname", ""),
					section.getString(name + ".headuuid", ""), section.getInt(name + ".ping", -1));
			fakePlayer.show();
			fakePlayers.add(fakePlayer);
		}
	}

	public void display() {
		for (IFakePlayer fakePlayer : fakePlayers) {
			fakePlayer.show();
		}
	}

	public EditingResult createPlayer(String name, String displayName, final String headUUID, int ping) {
		if (name.isEmpty()) {
			return EditingResult.UNKNOWN;
		}

		if (getFakePlayerByName(name).isPresent()) {
			return EditingResult.ALREADY_EXIST;
		}

		if (name.length() > 16) {
			name = name.substring(0, 16);
		}

		if (ping < -1) {
			ping = -1;
		}

		String path = "list." + name + ".";
		setOptionsAndSave(path + "headuuid", headUUID, path + "ping", ping, path + "displayname", displayName);

		IFakePlayer fakePlayer = new FakePlayer(name, displayName, headUUID, ping);
		fakePlayer.show();
		fakePlayers.add(fakePlayer);
		return EditingResult.OK;
	}

	public void removeAllFakePlayer() {
		fakePlayers.forEach(IFakePlayer::remove);
		fakePlayers.clear();
	}

	public EditingResult removePlayer(String name) {
		Optional<IFakePlayer> optional = getFakePlayerByName(name);

		if (!optional.isPresent()) {
			return EditingResult.NOT_EXIST;
		}

		ConfigurationSection section = plugin.getConf().getFakeplayers().getConfigurationSection("list");

		if (section != null) {
			for (String sName : section.getKeys(false)) {
				if (!sName.equalsIgnoreCase(name)) {
					continue;
				}

				section.set(sName, null);

				try {
					plugin.getConf().getFakeplayers().save(plugin.getConf().getFakeplayersFile());
				} catch (IOException ex) {
					Util.printTrace(Level.SEVERE, plugin, ex.getMessage(), ex);
					return EditingResult.UNKNOWN;
				}

				break;
			}
		}

		IFakePlayer fakePlayer = optional.get();

		fakePlayer.remove();
		fakePlayers.remove(fakePlayer);
		return EditingResult.OK;
	}

	public EditingResult renamePlayer(final String oldName, String newName) {
		Optional<IFakePlayer> optional = getFakePlayerByName(oldName);

		if (!optional.isPresent()) {
			return EditingResult.NOT_EXIST;
		}

		if (newName.length() > 16) {
			newName = newName.substring(0, 16);
		}

		setOptionsAndSave("list." + newName, plugin.getConf().getFakeplayers().get("list." + oldName),
				"list." + oldName, null);
		optional.get().setName(newName);
		return EditingResult.OK;
	}

	public EditingResult setSkin(String name, hu.montlikadani.tablist.utils.PlayerSkinProperties skinProperties) {
		Optional<IFakePlayer> optional = getFakePlayerByName(name);

		if (!optional.isPresent()) {
			return EditingResult.NOT_EXIST;
		}

		setOptionsAndSave("list." + name + ".headuuid", skinProperties.playerId.toString());
		optional.get().setSkin(skinProperties);
		return EditingResult.OK;
	}

	public EditingResult setPing(String name, int amount) {
		if (amount < -1) {
			return EditingResult.PING_AMOUNT;
		}

		Optional<IFakePlayer> optional = getFakePlayerByName(name);

		if (!optional.isPresent()) {
			return EditingResult.NOT_EXIST;
		}

		setOptionsAndSave("list." + name + ".ping", amount);
		optional.get().setPing(amount);
		return EditingResult.OK;
	}

	public EditingResult setDisplayName(String name, String displayName) {
		Optional<IFakePlayer> optional = getFakePlayerByName(name);

		if (!optional.isPresent()) {
			return EditingResult.NOT_EXIST;
		}

		setOptionsAndSave("list." + name + ".displayname", displayName);
		optional.get().setDisplayName(displayName);
		return EditingResult.OK;
	}

	private void setOptionsAndSave(Object... pathOption) {
		FileConfiguration config = plugin.getConf().getFakeplayers();

		for (int i = 0; i < pathOption.length; i += 2) {
			config.set((String) pathOption[i], pathOption[i + 1]);
		}

		try {
			config.save(plugin.getConf().getFakeplayersFile());
		} catch (IOException ex) {
			Util.printTrace(Level.SEVERE, plugin, ex.getMessage(), ex);
		}
	}

	public enum EditingResult {
		NOT_EXIST, ALREADY_EXIST, PING_AMOUNT, UNKNOWN, OK;
	}
}
