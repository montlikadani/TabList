package hu.montlikadani.tablist.user;

import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;

import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.tablist.TabListManager;
import hu.montlikadani.tablist.tablist.groups.TabGroupPlayer;

public final class TabListPlayer implements TabListUser {

	private final UUID uuid;
	private final Text name;

	private final TabGroupPlayer tabGroupPlayer;
	private final TabListManager tabListManager;

	public TabListPlayer(TabList tl, UUID uuid) {
		this.uuid = uuid;

		Optional<Player> player = getPlayer();
		this.name = player.isPresent() ? Text.of(player.get().getName()) : Text.EMPTY;

		tabGroupPlayer = new TabGroupPlayer(tl, this);
		tabListManager = new TabListManager(tl, this);
	}

	@Override
	public UUID getUniqueId() {
		return uuid;
	}

	@Override
	public Text getName() {
		return name;
	}

	@Override
	public TabGroupPlayer getTabPlayer() {
		return tabGroupPlayer;
	}

	@Override
	public TabListManager getTabListManager() {
		return tabListManager;
	}

	@Override
	public Optional<Player> getPlayer() {
		return Sponge.getGame().getServer().getPlayer(uuid);
	}

	@Override
	public boolean equals(Object o) {
		return o == this || (o instanceof TabListPlayer && uuid.equals(((TabListPlayer) o).uuid));
	}
}
