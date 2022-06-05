package hu.montlikadani.tablist.utils.reflection;

import java.io.InputStreamReader;
import java.util.List;
import java.util.NavigableMap;
import java.util.concurrent.CompletableFuture;

import org.bukkit.NamespacedKey;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import hu.montlikadani.tablist.Global;
import hu.montlikadani.tablist.tablist.TabText;

public final class JsonComponent {

	public static final com.google.gson.Gson GSON = new com.google.gson.GsonBuilder().disableHtmlEscaping().create();

	private final List<JsonObject> jsonList = new java.util.ArrayList<>(10);
	private final java.util.Map<String, String> fonts = new java.util.HashMap<>(1);

	private Object emptyJson;

	protected JsonComponent() {
	}

	Object parseProperty(String text, List<TabText.JsonElementData> existingJson) throws Exception {
		if (text.isEmpty()) {
			if (emptyJson == null) {
				emptyJson = ReflectionUtils.jsonComponentMethod.invoke(ClazzContainer.getIChatBaseComponent(), GSON.toJson(""));
			}

			return emptyJson;
		}

		jsonList.clear();

		JsonObject obj = new JsonObject();
		StringBuilder builder = new StringBuilder();

		text = text.replace('\u00a7', '&');
		text = text.replace("&#", "#");
		text = text.replace("&x", "#");

		int length = text.length(), index = 0;
		String font = "";

		for (int i = 0; i < length; i++) {
			char charAt = text.charAt(i);

			if (charAt == '[' && existingJson != null && index < existingJson.size() && text.charAt(i + 1) == '"'
					&& text.charAt(i + 2) == '"' && text.charAt(i + 3) == ',' && text.charAt(i + 4) == '{') {
				if (obj.size() == 0 || !obj.has("text")) {
					obj.addProperty("text", builder.toString());
				}

				TabText.JsonElementDataNew data = (TabText.JsonElementDataNew) existingJson.get(index);
				obj.add("extra", data.element);

				jsonList.add(obj);

				// GSON is escaping unicode characters \u258b to the actual char instead of leaving it as-is
				// This means that the formatting will break and also the last 4 or more json text is displayed
				// as the length of the json was changed
				// we need to avoid using unicode characters or a temporary solution
				// https://stackoverflow.com/questions/43091804/
				i += data.length - 1;
				obj = new JsonObject();

				index++;
				continue;
			}

			if (charAt == '&') {
				char code = text.charAt(i + 1);

				if (Global.isValidColourCharacter(code)) {
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
						length = text.length();

						int k = i - 2;
						if (k >= 0) { // It may be negative
							i = k; // Go back to the beginning of hex colour
						}

						continue;
					}

					if (builder.length() != 0) {
						obj.addProperty("text", builder.toString());
						jsonList.add(obj);

						obj = new JsonObject();
						builder = new StringBuilder();
					}

					switch (code) {
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
						org.bukkit.ChatColor colorChar = org.bukkit.ChatColor.getByChar(code);

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
					if (builder.length() != 0) {
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

				if (text.regionMatches(true, i, "{font=", 0, 6) && (closeIndex = text.indexOf('}', fromIndex)) != -1) {
					final String key = text.substring(fromIndex, closeIndex);

					String res = fonts.computeIfAbsent(key, s -> {
						try {
							return NamespacedKey.minecraft(key).toString();
						} catch (IllegalArgumentException ie) {
							org.bukkit.Bukkit.getLogger().log(java.util.logging.Level.WARNING, ie.getMessage());
						}

						return null;
					});

					if (res != null) {
						font = res;
					}
				} else if (text.regionMatches(true, i, "{/font", 0, 6) && (closeIndex = text.indexOf('}', fromIndex)) != -1) {
					font = fonts.computeIfAbsent("default", s -> NamespacedKey.minecraft("default").toString());
				}

				if (closeIndex != -1) {
					if (builder.length() != 0) {
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
				"[\"\"," + Global.replaceFrom(GSON.toJson(jsonList, List.class), 0, "[", "", 1));
	}

	@SuppressWarnings("deprecation")
	public CompletableFuture<NavigableMap<String, String>> getSkinValue(String uuid) {
		return CompletableFuture.supplyAsync(() -> {
			NavigableMap<String, String> map = new java.util.TreeMap<>();

			try (InputStreamReader content = new InputStreamReader(
					new java.net.URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unasigned=false")
							.openStream())) {
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

				map.put(value, json.get("textures").getAsJsonObject().get("SKIN").getAsJsonObject().get("url").getAsString());
			} catch (java.io.IOException e1) {
				e1.printStackTrace();
			}

			return map;
		});
	}
}
