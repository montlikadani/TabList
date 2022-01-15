package hu.montlikadani.tablist.user;

import hu.montlikadani.tablist.tablist.fakeplayers.FakePlayer;

public interface TabListUser {

	org.bukkit.entity.Player getPlayer();

	boolean isHidden();

	void setHidden(boolean hidden);

	boolean isRemovedFromPlayerList();

	void removeFromPlayerList();

	void addToPlayerList();

	java.util.UUID getUniqueId();

	hu.montlikadani.tablist.tablist.groups.GroupPlayer getGroupPlayer();

	hu.montlikadani.tablist.tablist.TabHandler getTabHandler();

	PlayerScore getPlayerScore();

	boolean isFakePlayerVisible(FakePlayer fakePlayer);

	void setCanSeeFakePlayer(FakePlayer fakePlayer);

	void removeAllVisibleFakePlayer();

}
