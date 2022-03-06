package hu.montlikadani.tablist.user;

import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.tablist.TabListManager;
import hu.montlikadani.tablist.tablist.groups.TabGroupPlayer;
import net.kyori.adventure.text.Component;

public final class TabListPlayer implements TabListUser {

	private final UUID uuid;
	private final Component name;

	private final TabGroupPlayer tabGroupPlayer;
	private final TabListManager tabListManager;

	public TabListPlayer(TabList tl, UUID uuid) {
		this.uuid = uuid;

		Optional<ServerPlayer> player = getPlayer();
		name = player.isPresent() ? Component.text(player.get().name()) : Component.empty();

		tabGroupPlayer = new TabGroupPlayer(tl, this);
		tabListManager = new TabListManager(tl, this);
	}

	@Override
	public UUID getUniqueId() {
		return uuid;
	}

	@Override
	public Component getName() {
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
	public Optional<ServerPlayer> getPlayer() {
		return Sponge.game().server().player(uuid);
	}

	@Override
	public boolean equals(Object o) {
		return o == this || (o instanceof TabListPlayer && uuid.equals(((TabListPlayer) o).uuid));
	}
}
