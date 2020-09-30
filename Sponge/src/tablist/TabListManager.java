package hu.montlikadani.tablist.sponge.tablist;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
		if (!ConfigValues.isTablistEnabled() || p == null) {
			return;
		}

		final UUID uuid = p.getUniqueId();

		cancelTab(uuid);

		if (SpongeCommands.TABENABLED.getOrDefault(uuid, false)) {
			return;
		}

		final ConfigManager conf = plugin.getC().getConfig();
		final String world = p.getWorld().getName();
		final String pName = p.getName();

		final Set<String> worldList = new HashSet<>();

		header = conf.isList("tablist", "header") ? conf.getStringList("tablist", "header")
				: conf.isString("tablist", "header")
						? Arrays.asList(conf.getString(new Object[] { "tablist", "header" }))
						: null;
		footer = conf.isList("tablist", "footer") ? conf.getStringList("tablist", "footer")
				: conf.isString("tablist", "footer")
						? Arrays.asList(conf.getString(new Object[] { "tablist", "footer" }))
						: null;

		if (conf.contains("tablist", "per-world")
				&& plugin.getC().getConfig().get("tablist", "per-world").hasMapChildren()) {
			t: for (Object w : plugin.getC().getConfig().get("tablist", "per-world").getChildrenMap().keySet()) {
				for (String split : w.toString().split(", ")) {
					if (world.equals(split)) {
						header = conf.isList("tablist", "per-world", w, "header")
								? conf.getStringList("tablist", "per-world", w, "header")
								: conf.isString("tablist", "per-world", w, "header")
										? Arrays.asList(
												conf.getString(new Object[] { "tablist", "per-world", w, "header" }))
										: null;
						footer = conf.isList("tablist", "per-world", w, "footer")
								? conf.getStringList("tablist", "per-world", w, "footer")
								: conf.isString("tablist", "per-world", w, "footer")
										? Arrays.asList(
												conf.getString(new Object[] { "tablist", "per-world", w, "footer" }))
										: null;
						worldList.add(split);
						break t;
					}
				}
			}
		}

		final int refreshTime = ConfigValues.getTablistUpdateTime();

		if (refreshTime < 1) {
			cancelTab(uuid);

			if (conf.getStringList("tablist", "disabled-worlds").contains(world)
					|| conf.getStringList("tablist", "restricted-players").contains(pName)) {
				return;
			}

			updateTab(uuid, worldList);
			return;
		}

		taskMap.put(uuid, Task.builder().async().intervalTicks(refreshTime).execute(task -> {
			if (Sponge.getServer().getOnlinePlayers().isEmpty()) {
				return;
			}

			if (conf.getStringList("tablist", "disabled-worlds").contains(world)
					|| conf.getStringList("tablist", "restricted-players").contains(pName)
					|| SpongeCommands.TABENABLED.getOrDefault(uuid, false)) {
				cancelTab(uuid);
				return;
			}

			updateTab(uuid, worldList);
		}).submit(plugin));
	}

	private void updateTab(final UUID playerUUID, final Set<String> worlds) {
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
						he += "\n\u00a7r";
					}

					he += line;
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
						fo += "\n\u00a7r";
					}

					fo += line;
				}
			}
		}

		if (!he.trim().isEmpty()) {
			he = plugin.makeAnim(he);
		}

		if (!fo.trim().isEmpty()) {
			fo = plugin.makeAnim(fo);
		}

		final String resultHeader = he;
		final String resultFooter = fo;

		final Variables v = plugin.getVariables();

		if (!worlds.isEmpty()) {
			for (String l : worlds) {
				Sponge.getServer().getWorld(l).ifPresent(w -> {
					for (Player player : w.getPlayers()) {
						sendTabList(player, v.replaceVariables(player, resultHeader),
								v.replaceVariables(player, resultFooter));
					}
				});
			}

			return;
		}

		Sponge.getServer().getPlayer(playerUUID).ifPresent(
				p -> sendTabList(p, v.replaceVariables(p, resultHeader), v.replaceVariables(p, resultFooter)));
	}

	public void sendTabList(Player p, String header, String footer) {
		sendTabList(p, Text.of(header), Text.of(footer));
	}

	public void sendTabList(Player p, Text header, Text footer) {
		p.getTabList().setHeaderAndFooter(header, footer);
	}

	public void cancelTabForAll() {
		Sponge.getServer().getOnlinePlayers().forEach(p -> cancelTab(p.getUniqueId()));

		// To make sure all removed
		taskMap.clear();
	}

	public void cancelTab(UUID uuid) {
		if (taskMap.containsKey(uuid)) {
			taskMap.get(uuid).cancel();
			taskMap.remove(uuid);
		}

		Sponge.getServer().getPlayer(uuid).ifPresent(p -> sendTabList(p, "", ""));
	}
}
