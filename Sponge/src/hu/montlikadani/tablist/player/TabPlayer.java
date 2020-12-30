package hu.montlikadani.tablist.player;

import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

import hu.montlikadani.tablist.tablist.groups.TabGroup;

public class TabPlayer implements ITabPlayer {

	private UUID playerUUID;

	public TabPlayer(UUID playerUUID) {
		this.playerUUID = playerUUID;
	}

	@Override
	public UUID getPlayerUUID() {
		return playerUUID;
	}

	@Override
	public String getServerWorldName() {
		return asServerPlayer().map(p -> p.getWorld().getKey().examinableName()).orElse("");
	}

	@Override
	public Optional<ServerPlayer> asServerPlayer() {
		return Sponge.getServer().getPlayer(playerUUID);
	}

	@Override
	public Optional<Player> asPlayer() {
		ServerPlayer sp = asServerPlayer().orElse(null);
		return sp instanceof Player ? Optional.of((Player) sp) : Optional.empty();
	}

	@Override
	public void setGroup(TabGroup group) {
		throw new UnsupportedOperationException("setGroup can only be used in TabUser");
	}

	@Override
	public Optional<TabGroup> getGroup() {
		throw new UnsupportedOperationException("getGroup can only be used in TabUser");
	}

	@Override
	public boolean updateGroup() {
		throw new UnsupportedOperationException("updateGroup can only be used in TabUser");
	}
}
