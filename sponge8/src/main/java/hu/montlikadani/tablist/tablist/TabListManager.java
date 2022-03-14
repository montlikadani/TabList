package hu.montlikadani.tablist.tablist;

import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.configurate.ConfigurationNode;

import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.config.ConfigManager;
import hu.montlikadani.tablist.config.ConfigValues;
import hu.montlikadani.tablist.user.TabListUser;
import hu.montlikadani.tablist.utils.Variables;
import net.kyori.adventure.text.Component;

public class TabListManager {

	private final TabListUser user;
	private final TabList tl;

	private String[] header, footer;
	private String linedHeader, linedFooter;

	private Random random;

	private final List<ResourceKey> worldList = new java.util.ArrayList<>();

	private final static Pattern SEPARATED_WORLD_NAMES = Pattern.compile(", ");

	public TabListManager(TabList tl, TabListUser user) {
		this.user = user;
		this.tl = tl;
	}

	public TabListUser getUser() {
		return user;
	}

	public void loadTab() {
		worldList.clear();

		if (!ConfigValues.isTablistEnabled()) {
			return;
		}

		ServerPlayer player = user.getPlayer().orElse(null);
		if (player == null) {
			return;
		}

		sendTabList(player, Component.empty(), Component.empty());

		if (TabHandler.TABENABLED.getOrDefault(user.getUniqueId(), false)) {
			return;
		}

		final ConfigManager conf = tl.getConfig().get();

		ConfigurationNode node = conf.getNode("tablist", "header");

		if (conf.isList(node)) {
			header = conf.getAsList(node).toArray(new String[0]);
		} else if (conf.isString(node)) {
			header = new String[] { node.getString("") };
		}

		node = conf.getNode("tablist", "footer");

		if (conf.isList(node)) {
			footer = conf.getAsList(node).toArray(new String[0]);
		} else if (conf.isString(node)) {
			footer = new String[] { node.getString("") };
		}

		node = conf.getNode("tablist", "per-world");

		if (!conf.contains(node) || !node.isMap()) {
			return;
		}

		String playerWorldName = player.world().context().getKey();

		t: for (Object w : node.childrenMap().keySet()) {
			for (String split : SEPARATED_WORLD_NAMES.split(w.toString())) {
				if (!playerWorldName.equals(split)) {
					continue;
				}

				ConfigurationNode h = node.node(w, "header");

				if (conf.isList(h)) {
					header = conf.getAsList(h).toArray(new String[0]);
				} else if (conf.isString(h)) {
					header = new String[] { h.getString("") };
				}

				h = node.node(w, "footer");

				if (conf.isList(h)) {
					footer = conf.getAsList(h).toArray(new String[0]);
				} else if (conf.isString(h)) {
					footer = new String[] { h.getString("") };
				}

				worldList.add(ResourceKey.minecraft(split));
				break t;
			}
		}

		if (header != null) {
			linedHeader = "";

			for (int a = 0; a < header.length; a++) {
				if (a + 1 > 1) {
					linedHeader += "\n\u00a7r";
				}

				header[a] = tl.getVariables().setSymbols(header[a]);
				linedHeader += header[a];
			}
		}

		if (footer != null) {
			linedFooter = "";

			for (int a = 0; a < footer.length; a++) {
				if (a + 1 > 1) {
					linedFooter += "\n\u00a7r";
				}

				footer[a] = tl.getVariables().setSymbols(footer[a]);
				linedFooter += footer[a];
			}
		}
	}

	protected void sendTab() {
		if (header == null && footer == null) {
			return;
		}

		if (header != null && header.length == 0 && footer != null && footer.length == 0) {
			return;
		}

		user.getPlayer().ifPresent(player -> {
			if (ConfigValues.getTabDisabledWorlds().contains(player.world().context().getKey())
					|| ConfigValues.getTabRestrictedPlayers().contains(player.name())
					|| TabHandler.TABENABLED.getOrDefault(user.getUniqueId(), false)) {
				sendTabList(player, Component.empty(), Component.empty());
				return;
			}

			String he = "";
			String fo = "";

			if (ConfigValues.isRandomTablist()) {
				if (random == null) {
					random = new Random();
				}

				if (header != null)
					he = header[header.length == 1 ? 0 : random.nextInt(header.length)];

				if (footer != null)
					fo = footer[footer.length == 1 ? 0 : random.nextInt(footer.length)];
			}

			if (linedHeader != null && he.isEmpty()) {
				he = linedHeader;
			}

			if (linedFooter != null && fo.isEmpty()) {
				fo = linedFooter;
			}

			if (he.isEmpty() && fo.isEmpty()) {
				return;
			}

			he = tl.makeAnim(he);
			fo = tl.makeAnim(fo);

			final String resultHeader = he;
			final String resultFooter = fo;

			final Variables v = tl.getVariables();

			if (!worldList.isEmpty()) {
				worldList.forEach(worldKey -> Sponge.game().server().worldManager().world(worldKey)
						.ifPresent(world -> world.players().forEach(pl -> sendTabList(pl, v.replaceVariables(pl, resultHeader),
								v.replaceVariables(pl, resultFooter)))));

				return;
			}

			sendTabList(player, v.replaceVariables(player, resultHeader), v.replaceVariables(player, resultFooter));
		});
	}

	public void sendTabList(ServerPlayer player, String header, String footer) {
		sendTabList(player, Component.text(header), Component.text(footer));
	}

	public void sendTabList(ServerPlayer player, Component header, Component footer) {
		player.tabList().setHeaderAndFooter(header, footer);
	}

	public void clearTab() {
		user.getPlayer().ifPresent(player -> sendTabList(player, Component.empty(), Component.empty()));
	}
}
