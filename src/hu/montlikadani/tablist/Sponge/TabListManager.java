package hu.montlikadani.tablist.Sponge;

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
import org.spongepowered.api.scheduler.Task.Builder;
import org.spongepowered.api.text.Text;

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
		if (!Config.isTabEnabled()) {
			return;
		}

		if (p == null) {
			return;
		}

		final UUID uuid = p.getUniqueId();
		if (SpongeCommands.enabled.containsKey(uuid) && SpongeCommands.enabled.get(uuid)) {
			return;
		}

		final String world = p.getWorld().getName();
		final String pName = p.getName();

		List<String> header = null;
		List<String> footer = null;

		boolean worldEnable = false;

		ConfigManager config = plugin.getConfig().getConfig();

		header = config.isList("tablist", "header") ? config.getStringList("tablist", "header")
				: config.isString("tablist", "header")
						? Arrays.asList(config.getString(new Object[] { "tablist", "header" }))
						: null;
		footer = config.isList("tablist", "footer") ? config.getStringList("tablist", "footer")
				: config.isString("tablist", "footer")
						? Arrays.asList(config.getString(new Object[] { "tablist", "footer" }))
						: null;

		setHeader(header);
		setFooter(footer);

		if (Config.getTabUpdateTime() < 1) {
			cancelTab(p);

			if (Config.getDisabledWorlds().contains(world)) {
				return;
			}

			if (Config.getBlackListedPlayers().contains(pName)) {
				return;
			}

			updateTab(p, worldEnable);
			return;
		}

		final boolean enableW = worldEnable;

		Builder t = Task.builder().execute(task -> {
			if (!Sponge.getPluginManager().isLoaded("tablist") || Sponge.getServer().getOnlinePlayers().isEmpty()) {
				cancelTabForAll();
				return;
			}

			if (Config.getDisabledWorlds().contains(world)) {
				return;
			}

			if (Config.getBlackListedPlayers().contains(pName)) {
				return;
			}

			if (SpongeCommands.enabled.containsKey(uuid) && SpongeCommands.enabled.get(uuid)) {
				sendTabList(p, "", "");
			} else {
				updateTab(p, enableW);
			}
		}).async().intervalTicks(Config.getTabUpdateTime());

		taskMap.put(uuid, t.submit(plugin));
	}

	public void updateTab(Player p, boolean yesWorld) {
		String he = "";
		int r = 0;

		if (getHeader().isPresent()) {
			if (Config.isTabTextRandom()) {
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
			if (Config.isTabTextRandom()) {
				he = footer.get(ThreadLocalRandom.current().nextInt(footer.size()));
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

		if (he.trim().isEmpty()) {
			he = "Something wrong with your tablist config in header section! Please check it!";
		}

		if (fo.trim().isEmpty()) {
			fo = "Something wrong with your tablist config in footer section! Please check it!";
		}

		Variables v = plugin.getVariables();

		if (yesWorld) {
			for (Player player : p.getWorld().getPlayers()) {
				sendTabList(player, v.replaceVariables(p, he), v.replaceVariables(p, fo));
			}
		} else {
			sendTabList(p, v.replaceVariables(p, he), v.replaceVariables(p, fo));
		}
	}

	public void sendTabList(Player p, String header, String footer) {
		sendTabList(p, Text.of(header), Text.of(footer));
	}

	public void sendTabList(Player p, Text header, Text footer) {
		p.getTabList().setHeaderAndFooter(header, footer);
	}

	public void cancelTabForAll() {
		Sponge.getServer().getOnlinePlayers().forEach(this::cancelTab);
		taskMap.clear();
	}

	public void cancelTab(Player p) {
		UUID uuid = p.getUniqueId();
		if (taskMap.containsKey(uuid)) {
			taskMap.get(uuid).cancel();
		}
	}
}
