package hu.montlikadani.tablist.user;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;

import hu.montlikadani.tablist.tablist.TabListManager;
import hu.montlikadani.tablist.tablist.groups.TabGroupPlayer;

public interface TabListUser {

	java.util.UUID getUniqueId();

	Text getName();

	java.util.Optional<Player> getPlayer();

	TabGroupPlayer getTabPlayer();

	TabListManager getTabListManager();

}
