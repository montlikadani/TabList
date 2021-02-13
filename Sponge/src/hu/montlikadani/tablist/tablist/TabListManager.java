package hu.montlikadani.tablist.tablist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

import com.google.common.base.Preconditions;

import hu.montlikadani.tablist.ConfigManager;
import hu.montlikadani.tablist.ConfigValues;
import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.Variables;
import hu.montlikadani.tablist.player.ITabPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

public class TabListManager {

	private TabList plugin;
	private ITabPlayer tabPlayer;
	private List<String> header;
	private List<String> footer;

	private final List<String> worldList = new ArrayList<>();

	public TabListManager(TabList plugin, ITabPlayer player) {
		this(plugin, player, null, null);
	}

	public TabListManager(TabList plugin, ITabPlayer tabPlayer, List<String> header, List<String> footer) {
		Preconditions.checkNotNull(tabPlayer, "tabPlayer cannot be null");

		this.plugin = plugin;
		this.header = header;
		this.tabPlayer = tabPlayer;
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

	public ITabPlayer getPlayer() {
		return tabPlayer;
	}

	public void loadTab() {
		worldList.clear();

		if (!ConfigValues.isTablistEnabled()) {
			return;
		}

		sendTabList(tabPlayer, "", "");

		if (TabHandler.TABENABLED.getOrDefault(tabPlayer.getPlayerUUID(), false)) {
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

		if (conf.contains("tablist", "per-world") && conf.get("tablist", "per-world").isMap()) {
			t: for (Object w : conf.get("tablist", "per-world").childrenMap().keySet()) {
				for (String split : w.toString().split(", ")) {
					if (tabPlayer.getServerWorldName().equals(split)) {
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
		tabPlayer.asServerPlayer().filter(player -> player.isOnline()).ifPresent(player -> {
			if (plugin.getC().getConfig().getStringList("tablist", "disabled-worlds")
					.contains(tabPlayer.getServerWorldName())
					|| plugin.getC().getConfig().getStringList("tablist", "restricted-players").contains(
							player.getName())
					|| TabHandler.TABENABLED.getOrDefault(player.getUniqueId(), false)) {
				sendTabList(tabPlayer, "", "");
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
					Sponge.getServer().getWorldManager().world(ResourceKey.minecraft(l))
							.ifPresent(w -> w.getPlayers().forEach(pl -> sendTabList(Optional.ofNullable(pl),
									v.replaceVariables(pl, resultHeader), v.replaceVariables(pl, resultFooter))));
				}

				return;
			}

			sendTabList(tabPlayer.asServerPlayer(), v.replaceVariables(player, resultHeader),
					v.replaceVariables(player, resultFooter));
		});
	}

	public void sendTabList(ITabPlayer p, String header, String footer) {
		sendTabList(p.asServerPlayer(), Component.text(header), Component.text(footer));
	}

	public void sendTabList(Optional<ServerPlayer> p, TextComponent header, TextComponent footer) {
		p.ifPresent(sp -> sp.getTabList().setHeaderAndFooter(header, footer));
	}

	public void clearTab() {
		sendTabList(tabPlayer, "", "");
	}
}
