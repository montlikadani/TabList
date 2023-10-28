package hu.montlikadani.tablist.tablist.text;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;

public final class LegacyTextConverter {

	public static final BaseComponent EMPTY_JSON = TextComponent.fromLegacy("[\"\",{\"text\":\"\"}]");

	public static synchronized BaseComponent toBaseComponent(String legacyText) {
		ComponentBuilder componentBuilder = new ComponentBuilder();
		StringBuilder stringBuilder = new StringBuilder();
		TextComponent textComponent = new TextComponent();
		int length = legacyText.length();

		for (int i = 0; i < length; i++) {
			char c = legacyText.charAt(i);

			if (c == '&') {
				char nextChar = legacyText.charAt(i + 1);

				if (isValidColourCharacter(nextChar)) {
					if (stringBuilder.length() != 0) {
						TextComponent old = textComponent;
						textComponent = new TextComponent(old);

						old.setText(stringBuilder.toString());
						stringBuilder = new StringBuilder();
						componentBuilder.append(old);
					}

					switch (nextChar) {
					case 'k':
						textComponent.setObfuscated(true);
						break;
					case 'o':
						textComponent.setItalic(true);
						break;
					case 'n':
						textComponent.setUnderlined(true);
						break;
					case 'm':
						textComponent.setStrikethrough(true);
						break;
					case 'l':
						textComponent.setBold(true);
						break;
					case 'r':
						textComponent = new TextComponent();
						textComponent.setColor(ChatColor.WHITE);
						break;
					default:
						ChatColor color = ChatColor.getByChar(nextChar);

						if (color != null) {
							textComponent = new TextComponent();
							textComponent.setColor(color);
						}

						break;
					}

					i++;
				} else {
					stringBuilder.append(c);
				}
			} else if (c == '#') {
				int from = i + 1;
				int end = i + 7;

				if (!isHexColour(from, end, length, legacyText)) {

					// Temporary solution to do not display # character
					if (from < length && legacyText.charAt(from) != '&') {
						stringBuilder.append(c);
					}
				} else {
					if (stringBuilder.length() != 0) {
						textComponent.setText(stringBuilder.toString());
						stringBuilder = new StringBuilder();
						componentBuilder.append(textComponent);
					}

					textComponent = new TextComponent();
					textComponent.setColor(ChatColor.of(legacyText.substring(i, end)));
					i += 6; // Increase loop to skip the next 6 hex digit
				}
			} else {
				stringBuilder.append(c);
			}
		}

		textComponent.setText(stringBuilder.toString());
		componentBuilder.append(textComponent);

		return componentBuilder.build();
	}

	private static boolean isValidColourCharacter(char ch) {
		return ((ch >= 'a' && ch <= 'f') || (ch == 'k' || ch == 'l' || ch == 'm' || ch == 'n' || ch == 'o' || ch == 'r')) || Character.isDigit(ch);
	}

	private static boolean isHexColour(int start, int endIndex, int maxLength, String text) {
		for (int b = start; b < endIndex; b++) {
			if (b >= maxLength || !Character.isLetterOrDigit(text.charAt(b))) {
				return false;
			}
		}

		return true;
	}
}
