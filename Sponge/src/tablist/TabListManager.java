package hu.montlikadani.tablist.sponge.tablist;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;

import hu.montlikadani.tablist.sponge.ConfigManager;
import hu.montlikadani.tablist.sponge.ConfigValues;
import hu.montlikadani.tablist.sponge.SpongeCommands;
import hu.montlikadani.tablist.sponge.TabList;
import hu.montlikadani.tablist.sponge.Variables;

public class TabListManager {

	private TabList plugin;

	private final Map<UUID, Task> taskMap = new HashMap<>();

	private List<String> header;
	private List<String> footer;

	public TabListManager(TabList plugin) {
		this(plugin, null, null);
	}

	public TabListManager(TabList plugin, List<String> header, List<String> footer) {
		this.plugin = plugin;
		this.header = header;
		this.footer = footer;
	}

	public Optional<List<String>> getHeader() {
		return Optional.ofNullable(header);
	}

	public void setHeader(List<String> header) {
		this.header = header;
	}

	public Optional<List<String>> getFooter() {
		return Optional.ofNullable(footer);
	}

	public void setFooter(List<String> footer) {
		this.footer = footer;
	}

	public void loadTab(Player p) {
		if (!ConfigValues.isTablistEnabled()) {
			return;
		}

		if (p == null) {
			return;
		}

		cancelTab(p);

		final UUID uuid = p.getUniqueId();

		if (SpongeCommands.TABENABLED.containsKey(uuid) && SpongeCommands.TABENABLED.get(uuid)) {
			return;
		}

		final ConfigManager conf = plugin.getC().getConfig();
		final String world = p.getWorld().getName();
		final String pName = p.getName();

		List<String> header = null, footer = null;

		header = conf.isList("tablist", "header") ? conf.getStringList("tablist", "header")
				: conf.isString("tablist", "header")
						? Arrays.asList(conf.getString(new Object[] { "tablist", "header" }))
						: null;
		footer = conf.isList("tablist", "footer") ? conf.getStringList("tablist", "footer")
				: conf.isString("tablist", "footer")
						? Arrays.asList(conf.getString(new Object[] { "tablist", "footer" }))
						: null;

		setHeader(header);
		setFooter(footer);

		final int refreshTime = ConfigValues.getTablistUpdateTime();

		if (refreshTime < 1) {
			cancelTab(p);

			if (conf.getStringList("tablist", "disabled-worlds").contains(world)
					|| conf.getStringList("tablist", "blacklisted-players").contains(pName)) {
				return;
			}

			updateTab(p);
			return;
		}

		taskMap.put(uuid, Task.builder().async().intervalTicks(refreshTime).execute(task -> {
			if (Sponge.getServer().getOnlinePlayers().isEmpty()) {
				cancelTabForAll();
				return;
			}

			if (conf.getStringList("tablist", "disabled-worlds").contains(world)
					|| conf.getStringList("tablist", "blacklisted-players").contains(pName)
					|| (SpongeCommands.TABENABLED.containsKey(uuid) && SpongeCommands.TABENABLED.get(uuid))) {
				cancelTab(p);
				return;
			}

			updateTab(p);
		}).submit(plugin));
	}

	private void updateTab(Player p) {
		String he = "";
		int r = 0;

		if (getHeader().isPresent()) {
			if (ConfigValues.isRandomTablist()) {
				he = header.get(ThreadLocalRandom.current().nextInt(header.size()));
			}

			if (he.isEmpty()) {
				for (String line : header) {
					r++;

					if (r > 1) {
						he = he + "\n\u00a7r";
					}

					he = he + line;
				}
			}
		}

		String fo = "";

		if (getFooter().isPresent()) {
			if (ConfigValues.isRandomTablist()) {
				fo = footer.get(ThreadLocalRandom.current().nextInt(footer.size()));
			}

			if (fo.isEmpty()) {
				r = 0;

				for (String line : footer) {
					r++;

					if (r > 1) {
						fo = fo + "\n\u00a7r";
					}

					fo = fo + line;
				}
			}
		}

		if (!he.trim().isEmpty()) {
			he = plugin.makeAnim(he);
		}

		if (!fo.trim().isEmpty()) {
			fo = plugin.makeAnim(fo);
		}

		Variables v = plugin.getVariables();
		sendTabList(p, v.replaceVariables(p, he), v.replaceVariables(p, fo));
	}

	public void sendTabList(Player p, String header, String footer) {
		sendTabList(p, Text.of(header), Text.of(footer));
	}

	public void sendTabList(Player p, Text header, Text footer) {
		p.getTabList().setHeaderAndFooter(header, footer);
	}

	public void cancelTabForAll() {
		Sponge.getServer().getOnlinePlayers().forEach(this::cancelTab);

		// To make sure all removed
		taskMap.clear();
	}

	public void cancelTab(Player p) {
		UUID uuid = p.getUniqueId();

		if (taskMap.containsKey(uuid)) {
			taskMap.get(uuid).cancel();
			taskMap.remove(uuid);
		}

		sendTabList(p, "", "");
	}
}
