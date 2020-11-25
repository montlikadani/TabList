package hu.montlikadani.tablist.sponge.tablist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;

import hu.montlikadani.tablist.sponge.ConfigManager;
import hu.montlikadani.tablist.sponge.ConfigValues;
import hu.montlikadani.tablist.sponge.TabList;
import hu.montlikadani.tablist.sponge.Variables;

public class TabListManager {

	private TabList plugin;
	private UUID playerUuid;
	private List<String> header;
	private List<String> footer;

	private final List<String> worldList = new ArrayList<>();

	public TabListManager(TabList plugin, UUID playerUuid) {
		this(plugin, playerUuid, null, null);
	}

	public TabListManager(TabList plugin, UUID playerUuid, List<String> header, List<String> footer) {
		this.plugin = plugin;
		this.playerUuid = playerUuid;
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

	public UUID getPlayerUuid() {
		return playerUuid;
	}

	public void loadTab() {
		worldList.clear();

		Player p = Sponge.getServer().getPlayer(playerUuid).orElse(null);

		if (!ConfigValues.isTablistEnabled() || p == null) {
			return;
		}

		sendTabList(p, "", "");

		if (TabHandler.TABENABLED.getOrDefault(playerUuid, false)) {
			return;
		}

		final ConfigManager conf = plugin.getC().getConfig();

		header = conf.isList("tablist", "header") ? conf.getStringList("tablist", "header")
				: conf.isString("tablist", "header")
						? Arrays.asList(conf.getString(new Object[] { "tablist", "header" }))
						: null;
		footer = conf.isList("tablist", "footer") ? conf.getStringList("tablist", "footer")
				: conf.isString("tablist", "footer")
						? Arrays.asList(conf.getString(new Object[] { "tablist", "footer" }))
						: null;

		if (conf.contains("tablist", "per-world") && conf.get("tablist", "per-world").hasMapChildren()) {
			t: for (Object w : conf.get("tablist", "per-world").getChildrenMap().keySet()) {
				for (String split : w.toString().split(", ")) {
					if (p.getWorld().getName().equals(split)) {
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
	}

	protected void sendTab() {
		final Player player = Sponge.getServer().getPlayer(playerUuid).orElse(null);
		if (player == null || !player.isOnline()) {
			return;
		}

		final ConfigManager conf = plugin.getC().getConfig();
		if (conf.getStringList("tablist", "disabled-worlds").contains(player.getWorld().getName())
				|| conf.getStringList("tablist", "restricted-players").contains(player.getName())
				|| TabHandler.TABENABLED.getOrDefault(playerUuid, false)) {
			sendTabList(player, "", "");
			return;
		}

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
		if (v == null) {
			return;
		}

		if (!worldList.isEmpty()) {
			for (String l : worldList) {
				Sponge.getServer().getWorld(l).ifPresent(w -> {
					for (Player pl : w.getPlayers()) {
						sendTabList(pl, v.replaceVariables(pl, resultHeader), v.replaceVariables(pl, resultFooter));
					}
				});
			}

			return;
		}

		Sponge.getServer().getPlayer(playerUuid).ifPresent(
				p -> sendTabList(p, v.replaceVariables(p, resultHeader), v.replaceVariables(p, resultFooter)));
	}

	public void sendTabList(Player p, String header, String footer) {
		sendTabList(p, Text.of(header), Text.of(footer));
	}

	public void sendTabList(Player p, Text header, Text footer) {
		p.getTabList().setHeaderAndFooter(header, footer);
	}

	public void clearTab() {
		Sponge.getServer().getPlayer(playerUuid).ifPresent(p -> sendTabList(p, "", ""));
	}
}
