package hu.montlikadani.tablist.utils.reflection;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.NavigableMap;
import java.util.concurrent.CompletableFuture;

import org.bukkit.NamespacedKey;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import hu.montlikadani.tablist.Global;

public final class JsonComponent {

	private final com.google.gson.Gson gson = new com.google.gson.GsonBuilder().disableHtmlEscaping().create();
	private final java.util.List<JsonObject> jsonList = new java.util.ArrayList<>(10);

	private String defaultFontNamespacedKey;

	protected JsonComponent() {
	}

	// Lock until this method is not exited to prevent NPE from client
	synchronized Object parseProperty(String text) throws Exception {
		jsonList.clear();

		JsonObject obj = new JsonObject();
		StringBuilder builder = new StringBuilder();

		text = text.replace('\u00a7', '&');
		text = text.replace("&#", "#");
		text = text.replace("&x", "#");

		int length = text.length();
		String font = "";

		for (int i = 0; i < length; i++) {
			if (i >= length) {
				break;
			}

			char charAt = text.charAt(i);

			if (charAt == '&') {
				char nextChar = text.charAt(i + 1);

				if (Global.isValidColourCharacter(nextChar)) {
					int current = i + 2;

					// Finds hex colours that may be coming from essentials (&x&f ..) and removes
					// the "&" character to match the TL hex colour
					//
					// This is a very expensive inspection and not ideal, it should be a more
					// optimal one (and cleaner).
					// The expected pattern would be: (?>&[0-9a-f]){5}
					if (current < length && text.charAt(current) == '&'
							&& ((current = i + 4) >= length || text.charAt(current) == '&')
							&& ((current = i + 6) >= length || text.charAt(current) == '&')
							&& ((current = i + 8) >= length || text.charAt(current) == '&')
							&& ((current = i + 10) >= length || text.charAt(current) == '&')) {
						text = Global.replaceFrom(text, i, "&", "", 6); // Replace "&" character 6 times
						length = text.length(); // Text length is changed

						int k = i - 2;
						if (k >= 0) { // It may be negative
							i = k; // Go back to the beginning of hex colour
						}

						continue;
					}

					if (builder.length() > 0) {
						obj.addProperty("text", builder.toString());
						jsonList.add(obj);

						obj = new JsonObject();
						builder = new StringBuilder();
					}

					switch (nextChar) {
					case 'k':
						obj.addProperty("obfuscated", true);
						break;
					case 'o':
						obj.addProperty("italic", true);
						break;
					case 'n':
						obj.addProperty("underlined", true);
						break;
					case 'm':
						obj.addProperty("strikethrough", true);
						break;
					case 'l':
						obj.addProperty("bold", true);
						break;
					case 'r':
						obj.addProperty("color", "white");
						break;
					default:
						org.bukkit.ChatColor colorChar = org.bukkit.ChatColor.getByChar(nextChar);

						if (colorChar != null) {
							obj.addProperty("color", colorChar.name().toLowerCase(java.util.Locale.ENGLISH));
						}

						break;
					}

					if (!font.isEmpty()) {
						obj.addProperty("font", font);
						font = "";
					}

					i++;
				} else {
					builder.append(charAt);
				}
			} else if (charAt == '#') {
				boolean isAllDigit = true;
				int from = i + 1;
				int end = i + 7;

				for (int b = from; b < end; b++) {
					if (b >= length || !Character.isLetterOrDigit(text.charAt(b))) {
						isAllDigit = false;
						break;
					}
				}

				if (!isAllDigit) {
					// Temporary solution to do not display # character
					if (from < length && text.charAt(from) != '&') {
						builder.append(charAt);
					}
				} else {
					if (builder.length() > 0) {
						obj.addProperty("text", builder.toString());
						jsonList.add(obj);
						builder = new StringBuilder();
					}

					obj = new JsonObject();
					obj.addProperty("color", text.substring(i, end));
					i += 6; // Increase loop to skip the next 6 hex digit
				}
			} else if (charAt == '{') {
				int closeIndex = -1;
				int fromIndex = i + 6;

				if (text.regionMatches(true, i, "{font=", 0, 6) && (closeIndex = text.indexOf('}', fromIndex)) >= 0) {
					font = NamespacedKey.minecraft(text.substring(fromIndex, closeIndex)).toString();
				} else if (text.regionMatches(true, i, "{/font", 0, 6)
						&& (closeIndex = text.indexOf('}', fromIndex)) >= 0) {
					if (defaultFontNamespacedKey == null)
						defaultFontNamespacedKey = NamespacedKey.minecraft("default").toString();

					font = defaultFontNamespacedKey;
				}

				if (closeIndex >= 0) {
					if (builder.length() > 0) {
						obj.addProperty("text", builder.toString());
						jsonList.add(obj);
						builder = new StringBuilder();
					}

					obj = new JsonObject();
					obj.addProperty("font", font);
					font = "";
					i += closeIndex - i;
				}
			} else {
				builder.append(charAt);
			}
		}

		obj.addProperty("text", builder.toString());
		jsonList.add(obj);

		// Minecraft JSON is weird, it must begin with ["", to actually return the
		// expected format
		return ReflectionUtils.jsonComponentMethod.invoke(ClazzContainer.getIChatBaseComponent(),
				"[\"\"," + Global.replaceFrom(gson.toJson(jsonList), 0, "[", "", 1));
	}

	@SuppressWarnings("deprecation")
	public CompletableFuture<NavigableMap<String, String>> getSkinValue(String uuid) {
		return CompletableFuture.supplyAsync(() -> {
			NavigableMap<String, String> map = new java.util.TreeMap<>();

			try (InputStreamReader content = getContent(
					"https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unasigned=false")) {
				if (content == null) {
					return map;
				}

				JsonObject json;

				try {
					json = JsonParser.parseReader(content).getAsJsonObject();
				} catch (NoSuchMethodError e) {
					json = new JsonParser().parse(content).getAsJsonObject();
				}

				com.google.gson.JsonArray jsonArray = json.get("properties").getAsJsonArray();
				if (jsonArray.isEmpty()) {
					return map;
				}

				String value = jsonArray.get(0).getAsJsonObject().get("value").getAsString();
				String decodedValue = new String(java.util.Base64.getDecoder().decode(value));

				try {
					json = JsonParser.parseString(decodedValue).getAsJsonObject();
				} catch (NoSuchMethodError e) {
					json = new JsonParser().parse(decodedValue).getAsJsonObject();
				}

				String texture = json.get("textures").getAsJsonObject().get("SKIN").getAsJsonObject().get("url")
						.getAsString();

				map.put(value, texture);
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			return map;
		});
	}

	private InputStreamReader getContent(String link) {
		try {
			return new InputStreamReader(new java.net.URL(link).openStream());
		} catch (java.io.IOException e) {
			e.printStackTrace();
		}

		return null;
	}
}
