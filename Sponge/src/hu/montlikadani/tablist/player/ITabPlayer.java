package hu.montlikadani.tablist.player;

import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

import hu.montlikadani.tablist.tablist.groups.TabGroup;

public interface ITabPlayer {

	UUID getPlayerUUID();

	void setGroup(TabGroup group);

	Optional<TabGroup> getGroup();

	boolean updateGroup();

	String getServerWorldName();

	Optional<ServerPlayer> asServerPlayer();

	Optional<Player> asPlayer();
}
