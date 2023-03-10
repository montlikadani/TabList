package hu.montlikadani.tablist.tablist.fakeplayers;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.config.constantsLoader.ConfigValues;

public final class FakePlayerHandler {

	protected final TabList plugin;

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
			return;
		}

		ConfigurationSection cs = plugin.getConf().getFakeplayers().getConfigurationSection("list");
		if (cs == null) {
			cs = plugin.getConf().getFakeplayers().createSection("list");
		}

		for (String name : cs.getKeys(false)) {
			FakePlayer fp = new FakePlayer(name, cs.getString(name + ".displayname", ""), cs.getString(name + ".headuuid", ""), cs.getInt(name + ".ping", -1));
			fp.show();
			fakePlayers.add(fp);
		}
	}

	public void display() {
		for (IFakePlayer fp : fakePlayers) {
			fp.show();
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

		String path = "list." + name + ".";
		FileConfiguration c = plugin.getConf().getFakeplayers();

		c.set(path + "headuuid", headUUID);

		if (ping < -1) {
			ping = -1;
		}

		c.set(path + "ping", ping);
		c.set(path + "displayname", displayName);

		try {
			c.save(plugin.getConf().getFakeplayersFile());
		} catch (IOException e) {
			e.printStackTrace();
			return EditingResult.UNKNOWN;
		}

		IFakePlayer fp = new FakePlayer(name, displayName, headUUID, ping);
		fp.show();
		fakePlayers.add(fp);
		return EditingResult.OK;
	}

	public void removeAllFakePlayer() {
		fakePlayers.forEach(IFakePlayer::remove);
		fakePlayers.clear();
	}

	public EditingResult removePlayer(String name) {
		Optional<IFakePlayer> fp = getFakePlayerByName(name);

		if (!fp.isPresent()) {
			return EditingResult.NOT_EXIST;
		}

		ConfigurationSection section = plugin.getConf().getFakeplayers().getConfigurationSection("list");
		if (section == null) {
			return EditingResult.NOT_EXIST;
		}

		for (String sName : section.getKeys(false)) {
			if (sName.equalsIgnoreCase(name)) {
				section.set(sName, null);

				try {
					plugin.getConf().getFakeplayers().save(plugin.getConf().getFakeplayersFile());
				} catch (IOException e) {
					e.printStackTrace();
					return EditingResult.UNKNOWN;
				}

				break;
			}
		}

		IFakePlayer fpl = fp.get();

		fpl.remove();
		fakePlayers.remove(fpl);
		return EditingResult.OK;
	}

	public EditingResult renamePlayer(final String oldName, String newName) {
		Optional<IFakePlayer> fp = getFakePlayerByName(oldName);

		if (!fp.isPresent()) {
			return EditingResult.NOT_EXIST;
		}

		FileConfiguration config = plugin.getConf().getFakeplayers();

		if (!config.isConfigurationSection("list")) {
			return EditingResult.NOT_EXIST;
		}

		if (newName.length() > 16) {
			newName = newName.substring(0, 16);
		}

		config.set("list." + newName, config.get("list." + oldName));
		config.set("list." + oldName, null);

		try {
			config.save(plugin.getConf().getFakeplayersFile());
		} catch (IOException e) {
			e.printStackTrace();
			return EditingResult.UNKNOWN;
		}

		fp.get().setName(newName);
		return EditingResult.OK;
	}

	public EditingResult setSkin(String name, UUID uuid) {
		Optional<IFakePlayer> fp = getFakePlayerByName(name);

		if (!fp.isPresent()) {
			return EditingResult.NOT_EXIST;
		}

		plugin.getConf().getFakeplayers().set("list." + name + ".headuuid", uuid.toString());

		try {
			plugin.getConf().getFakeplayers().save(plugin.getConf().getFakeplayersFile());
		} catch (IOException e) {
			e.printStackTrace();
			return EditingResult.UNKNOWN;
		}

		fp.get().setSkin(uuid);
		return EditingResult.OK;
	}

	public EditingResult setPing(String name, int amount) {
		if (amount < -1) {
			return EditingResult.PING_AMOUNT;
		}

		Optional<IFakePlayer> fp = getFakePlayerByName(name);

		if (!fp.isPresent()) {
			return EditingResult.NOT_EXIST;
		}

		plugin.getConf().getFakeplayers().set("list." + name + ".ping", amount);

		try {
			plugin.getConf().getFakeplayers().save(plugin.getConf().getFakeplayersFile());
		} catch (IOException e) {
			e.printStackTrace();
			return EditingResult.UNKNOWN;
		}

		fp.get().setPing(amount);
		return EditingResult.OK;
	}

	public EditingResult setDisplayName(String name, String displayName) {
		Optional<IFakePlayer> fp = getFakePlayerByName(name);

		if (!fp.isPresent()) {
			return EditingResult.NOT_EXIST;
		}

		plugin.getConf().getFakeplayers().set("list." + name + ".displayname", displayName);

		try {
			plugin.getConf().getFakeplayers().save(plugin.getConf().getFakeplayersFile());
		} catch (IOException e) {
			e.printStackTrace();
			return EditingResult.UNKNOWN;
		}

		fp.get().setDisplayName(displayName);
		return EditingResult.OK;
	}

	public enum EditingResult {
		NOT_EXIST, ALREADY_EXIST, PING_AMOUNT, UNKNOWN, OK;
	}
}
