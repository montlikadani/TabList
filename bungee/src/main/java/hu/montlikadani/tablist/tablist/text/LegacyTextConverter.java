package hu.montlikadani.tablist.tablist.text;

import java.util.ArrayList;

import com.google.gson.JsonObject;

import hu.montlikadani.tablist.Global;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

public final class LegacyTextConverter {

	public static final String EMPTY_JSON = "[\"\",{\"text\":\"\"}]";

	private static final com.google.gson.Gson GSON = new com.google.gson.GsonBuilder().disableHtmlEscaping().create();

	private static final ArrayList<JsonObject> JSON_OBJECTS = new ArrayList<>(5);
	private static final ArrayList<BaseComponent> COMPONENTS = new ArrayList<>(10);

	public static synchronized BaseComponent[] toBaseComponent(String legacyText) {
		COMPONENTS.clear();
		COMPONENTS.trimToSize();

		StringBuilder builder = new StringBuilder();
		TextComponent component = new TextComponent();
		int length = legacyText.length();

		for (int i = 0; i < length; i++) {
			if (i >= length) {
				break;
			}

			char c = legacyText.charAt(i);

			if (c == '&') {
				char nextChar = legacyText.charAt(i + 1);

				if (Global.isValidColourCharacter(nextChar)) {
					if (builder.length() != 0) {
						TextComponent old = component;
						component = new TextComponent(old);

						old.setText(builder.toString());
						builder = new StringBuilder();
						COMPONENTS.add(old);
					}

					switch (nextChar) {
					case 'k':
						component.setObfuscated(true);
						break;
					case 'o':
						component.setItalic(true);
						break;
					case 'n':
						component.setUnderlined(true);
						break;
					case 'm':
						component.setStrikethrough(true);
						break;
					case 'l':
						component.setBold(true);
						break;
					case 'r':
						component = new TextComponent();
						component.setColor(ChatColor.WHITE);
						break;
					default:
						ChatColor color = ChatColor.getByChar(nextChar);

						if (color != null) {
							component = new TextComponent();
							component.setColor(color);
						}

						break;
					}

					i++;
				} else {
					builder.append(c);
				}
			} else if (c == '#') {
				int from = i + 1;
				int end = i + 7;

				if (!isHexaColour(from, end, length, legacyText)) {

					// Temporary solution to do not display # character
					if (from < length && legacyText.charAt(from) != '&') {
						builder.append(c);
					}
				} else {
					if (builder.length() != 0) {
						TextComponent old = component;
						component = new TextComponent(old);

						old.setText(builder.toString());
						builder = new StringBuilder();
						COMPONENTS.add(old);
					}

					component = new TextComponent();
					component.setColor(ChatColor.of(legacyText.substring(i, end)));
					i += 6; // Increase loop to skip the next 6 hex digit
				}
			} else {
				builder.append(c);
			}
		}

		component.setText(builder.toString());
		COMPONENTS.add(component);

		return COMPONENTS.toArray(new BaseComponent[0]);
	}

	public static synchronized String toJson(String text) {
		JSON_OBJECTS.clear();
		JSON_OBJECTS.trimToSize();

		JsonObject object = new JsonObject();
		StringBuilder builder = new StringBuilder();

		int length = text.length();

		for (int i = 0; i < length; i++) {
			if (i >= length) {
				break;
			}

			char charAt = text.charAt(i);

			if (charAt == '&') {
				char nextChar = text.charAt(i + 1);

				if (Global.isValidColourCharacter(nextChar)) {
					if (builder.length() != 0) {
						object.addProperty("text", builder.toString());
						JSON_OBJECTS.add(object);

						object = new JsonObject();
						builder = new StringBuilder();
					}

					switch (nextChar) {
					case 'k':
						object.addProperty("obfuscated", true);
						break;
					case 'o':
						object.addProperty("italic", true);
						break;
					case 'n':
						object.addProperty("underlined", true);
						break;
					case 'm':
						object.addProperty("strikethrough", true);
						break;
					case 'l':
						object.addProperty("bold", true);
						break;
					default:
						if (nextChar == 'r' || nextChar == 'f') {
							break;
						}

						ChatColor colorChar = ChatColor.getByChar(nextChar);

						if (colorChar != null) {
							object.addProperty("color", colorChar.getName());
						}

						break;
					}

					i++;
				} else {
					builder.append(charAt);
				}
			} else if (charAt == '#') {
				int from = i + 1;
				int end = i + 7;

				if (!isHexaColour(from, end, length, text)) {
					if (from < length && text.charAt(from) != '&') {
						builder.append(charAt);
					}
				} else {
					if (builder.length() != 0) {
						object.addProperty("text", builder.toString());
						JSON_OBJECTS.add(object);
						builder = new StringBuilder();
					}

					object = new JsonObject();
					object.addProperty("color", text.substring(i, end));
					i += 6;
				}
			} else {
				builder.append(charAt);
			}
		}

		object.addProperty("text", builder.toString());
		JSON_OBJECTS.add(object);

		return "[\"\"," + Global.replaceFrom(GSON.toJson(JSON_OBJECTS), 0, "[", "", 1);
	}

	private static boolean isHexaColour(int start, int endIndex, int maxLength, String text) {
		for (int b = start; b < endIndex; b++) {
			if (b >= maxLength || !Character.isLetterOrDigit(text.charAt(b))) {
				return false;
			}
		}

		return true;
	}
}
