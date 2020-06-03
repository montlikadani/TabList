package hu.montlikadani.tablist.bukkit.tablist.tabEntry;

import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

public class Rows extends GameProfile {

	private String text;

	private int ping;

	public Rows(UUID id, int number, String text, int ping, Property head) {
		super(id, String.valueOf(number));
		this.text = text;
		this.ping = ping;

		setHead(head);
	}

	public String getText() {
		return this.text;
	}

	public boolean setText(String text) {
		if (this.text.equals(text))
			return false;

		this.text = text;
		return true;
	}

	public int getPing() {
		return ping;
	}

	public boolean setPing(int ping) {
		if (this.ping == ping)
			return false;

		this.ping = ping;
		return true;
	}

	public boolean setHead(Property property) {
		Property p = getProperties().get("textures").stream().findFirst().orElse(null);
		if (p != null && p == property)
			return false;

		getProperties().removeAll("textures");
		getProperties().put("textures", property);
		return true;
	}
}
