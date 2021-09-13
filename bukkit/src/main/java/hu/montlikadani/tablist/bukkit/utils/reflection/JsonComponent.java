package hu.montlikadani.tablist.bukkit.utils.reflection;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.NavigableMap;
import java.util.concurrent.CompletableFuture;

import javax.net.ssl.HttpsURLConnection;

import org.bukkit.NamespacedKey;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import hu.montlikadani.tablist.bukkit.utils.StrUtil;

public final class JsonComponent {

	private final com.google.gson.Gson gson = new com.google.gson.GsonBuilder().create();
	private final JsonParser parser = new JsonParser();
	private final java.util.List<JsonObject> jsonList = new java.util.concurrent.CopyOnWriteArrayList<>();

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
		String font = "", colorName = "";

		for (int i = 0; i < length; i++) {
			if (i >= length) {
				break;
			}

			char charAt = text.charAt(i);

			if (charAt == '&') {
				char nextChar = text.charAt(i + 1);

				if (isColorLetterOrDigit(nextChar)) {

					// Finds hex colours that may be coming from essentials (&x&f ..) and removes
					// the "&" character to match the TL hex colour
					if (i + 2 < length && text.charAt(i + 2) == '&' && (i + 4 >= length || text.charAt(i + 4) == '&')
							&& (i + 6 >= length || text.charAt(i + 6) == '&')) {
						text = StrUtil.replaceNextChar(text, i, "&", "", 6); // Replace "&" character 6 times
						length = text.length(); // Text length is changed

						if (i - 2 >= 0) { // It may be negative
							i -= 2; // Go back to the beginning of hex colour
						}

						continue;
					}

					obj.addProperty("text", builder.toString());
					jsonList.add(obj);

					obj = new JsonObject();
					builder = new StringBuilder();

					if (!colorName.isEmpty()) {
						obj.addProperty("color", colorName);
					}

					if (!font.isEmpty()) {
						obj.addProperty("font", font);
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
						obj.addProperty("color", colorName = "white");
						break;
					default:
						org.bukkit.ChatColor colorChar = org.bukkit.ChatColor.getByChar(nextChar);

						if (colorChar != null) {
							obj.addProperty("color", colorName = colorChar.name().toLowerCase());
						}

						break;
					}

					i++;
				} else {
					builder.append(charAt);
				}
			} else if (charAt == '#') {
				boolean isAllDigit = true;

				for (int b = i + 1; b < i + 7; b++) {
					if (!Character.isLetterOrDigit(text.charAt(b))) {
						isAllDigit = false;
						break;
					}
				}

				if (!isAllDigit) {
					// Temporary solution to do not display # character
					if (i + 1 < length && text.charAt(i + 1) != '&') {
						builder.append(charAt);
					}
				} else {
					colorName = text.substring(i, i + 7);

					if (builder.length() > 0) {
						obj.addProperty("text", builder.toString());
						jsonList.add(obj);
						builder = new StringBuilder();
					}

					obj = new JsonObject();
					obj.addProperty("color", colorName);
					i += 6; // Increase loop with 6 to ignore hex digit
				}
			} else if (charAt == '{') {
				int closeIndex = -1;

				if (text.regionMatches(true, i, "{font=", 0, 6) && (closeIndex = text.indexOf('}', i + 6)) >= 0) {
					font = NamespacedKey.minecraft(text.substring(i + 6, closeIndex)).toString();
				} else if (text.regionMatches(true, i, "{/font", 0, 6)
						&& (closeIndex = text.indexOf('}', i + 6)) >= 0) {
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
					i += closeIndex - i;
				}
			} else {
				builder.append(charAt);
			}
		}

		obj.addProperty("text", builder.toString());
		jsonList.add(obj);

		return ReflectionUtils.jsonComponentMethod.invoke(ClazzContainer.getIChatBaseComponent(),
				gson.toJson(jsonList));
	}

	private boolean isColorLetterOrDigit(char ch) {
		return ((ch >= 'a' && ch <= 'f')
				|| (ch == 'k' || ch == 'l' || ch == 'm' || ch == 'n' || ch == 'o' || ch == 'r'))
				|| Character.isDigit(ch);
	}

	public CompletableFuture<NavigableMap<String, String>> getSkinValue(String uuid) {
		return CompletableFuture.supplyAsync(() -> {
			NavigableMap<String, String> map = new java.util.TreeMap<>();
			String content = getContent(
					"https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.replace("-", ""));

			if (content == null) {
				return map;
			}

			JsonObject json = parser.parse(content).getAsJsonObject();
			String value = json.get("properties").getAsJsonArray().get(0).getAsJsonObject().get("value").getAsString();

			json = parser.parse(new String(org.apache.commons.codec.binary.Base64.decodeBase64(value)))
					.getAsJsonObject();
			String texture = json.get("textures").getAsJsonObject().get("SKIN").getAsJsonObject().get("url")
					.getAsString();
			map.put(value, texture);
			return map;
		});
	}

	private String getContent(String link) {
		try {
			HttpsURLConnection conn = (HttpsURLConnection) new java.net.URL(link).openConnection();

			try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
				String inputLine;

				while ((inputLine = br.readLine()) != null) {
					return inputLine;
				}
			}

			conn.disconnect();
		} catch (java.io.IOException e) {
			e.printStackTrace();
		}

		return null;
	}
}
