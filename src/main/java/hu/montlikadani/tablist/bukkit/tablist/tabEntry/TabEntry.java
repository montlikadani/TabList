package hu.montlikadani.tablist.bukkit.tablist.tabEntry;

import com.mojang.authlib.properties.Property;

public class TabEntry {

	public static final Property DEFAULT_HEAD_SKIN = new Property("textures", "=", "=");

	String text = "";
	int ping = 10000;
	Property headSkin;

	public TabEntry() {
		this.headSkin = DEFAULT_HEAD_SKIN;
	}

	public TabEntry setText(String text) {
		this.text = text;
		return this;
	}

	public TabEntry setPing(int ping) {
		this.ping = ping;
		return this;
	}

	public TabEntry setHead(Property head) {
		this.headSkin = head;
		return this;
	}

	public TabEntry removeHead() {
		return setHead(DEFAULT_HEAD_SKIN);
	}

	public String getText() {
		return text;
	}

	public int getPing() {
		return ping;
	}

	public Property getHeadSkin() {
		return headSkin;
	}
}
