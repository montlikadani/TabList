package hu.montlikadani.tablist.utils.reflection;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import org.bukkit.NamespacedKey;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import hu.montlikadani.tablist.Global;
import hu.montlikadani.tablist.tablist.TabText;

public final class JsonComponent {

	public static final com.google.gson.Gson GSON = new com.google.gson.GsonBuilder().disableHtmlEscaping().create();

	private final ArrayList<JsonObject> jsonList = new ArrayList<>(10);
	private final java.util.Map<String, String> fonts = new java.util.HashMap<>(1);

	private final Pattern colorRegexPattern = Pattern.compile("#&([0-9a-fA-F])");

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
		jsonList.trimToSize();

		text = text.replace('\u00a7', '&');
		text = text.replace("&#", "#");
		text = text.replace("&x", "#");

		// Finds hex colours that may be coming from essentials (&x&f ..) and removes
		// the "&" character to match the TL hex colour
		java.util.regex.Matcher matcher = colorRegexPattern.matcher(text);

		while (matcher.find()) {
			text = Global.replaceFrom(text, matcher.start(), "&", "", 6);
			matcher = matcher.reset(text);
		}

		int length = text.length(), index = 0;
		String font = "";

		JsonObject obj = new JsonObject();
		StringBuilder builder = new StringBuilder(length);

		for (int i = 0; i < length; i++) {
			char charAt = text.charAt(i);

			if (charAt == '[' && existingJson != null && index < existingJson.size() && text.charAt(i + 1) == '"'
					&& text.charAt(i + 2) == '"' && text.charAt(i + 3) == ',' && text.charAt(i + 4) == '{') {
				if (builder.length() != 0) {
					obj.addProperty("text", builder.toString());
					jsonList.add(obj);
					builder = new StringBuilder();
				}

				obj = new JsonObject();
				obj.addProperty("text", "");

				TabText.JsonElementDataNew data = (TabText.JsonElementDataNew) existingJson.get(index);
				obj.add("extra", data.element);

				jsonList.add(obj);

				// GSON is escaping unicode characters \u258b to the actual char instead of leaving it as-is
				// This means that the formatting will break and also the last 4 or more json text is displayed
				// as the length of the json was changed
				// we need to avoid using unicode characters or a temporary solution
				// https://stackoverflow.com/questions/43091804/
				i += data.jsonLength - 1;
				obj = new JsonObject();

				index++;
				continue;
			}

			if (charAt == '&') {
				int next = i + 1;
				MColor mColor = next < length ? MColor.byCode(text.charAt(next)) : null;

				if (mColor != null) {
					if (builder.length() != 0) {
						obj.addProperty("text", builder.toString());
						jsonList.add(obj);

						obj = new JsonObject();
						builder = new StringBuilder();
					}

					if (mColor != MColor.WHITE && mColor != MColor.RESET) { // We don't need these formatting as the
																			// default colour is white
						if (mColor.formatter) {
							obj.addProperty(mColor.propertyName, true);
						} else {
							obj.addProperty("color", mColor.propertyName);
						}
					}

					if (!font.isEmpty()) {
						obj.addProperty("font", font);
						font = "";
					}

					i = next;
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
					String res = fonts.computeIfAbsent(text.substring(fromIndex, closeIndex), key -> {
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
					font = fonts.computeIfAbsent("default", s -> NamespacedKey.minecraft(s).toString());
				} else {
					builder.append(charAt);
					continue;
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
					i = closeIndex;
				}
			} else {
				builder.append(charAt);
			}
		}

		obj.addProperty("text", builder.toString());
		jsonList.add(obj);

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
