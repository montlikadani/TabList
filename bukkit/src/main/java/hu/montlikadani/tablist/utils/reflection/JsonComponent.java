package hu.montlikadani.tablist.utils.reflection;

import java.awt.Color;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.bukkit.NamespacedKey;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import hu.montlikadani.tablist.Global;
import hu.montlikadani.tablist.packets.PacketNM;
import hu.montlikadani.tablist.tablist.TabText;

public final class JsonComponent {

	public static final com.google.gson.Gson GSON = new com.google.gson.GsonBuilder().disableHtmlEscaping().create();

	private final ArrayList<JsonObject> jsonList = new ArrayList<>(10);
	private final java.util.Map<String, String> fonts = new java.util.HashMap<>(1);

	private Object emptyJson;

	protected JsonComponent() {
	}

	Object parseProperty(String text, List<TabText.JsonElementData> existingJson) {
		if (text.isEmpty()) {
			if (emptyJson == null) {
				emptyJson = PacketNM.NMS_PACKET.fromJson(GSON.toJson(""));
			}

			return emptyJson;
		}

		jsonList.clear();
		jsonList.trimToSize();

		text = text.replace('\u00a7', '&').replace("&#", "#").replace("&x", "#");

		int length = text.length(), index = 0;
		String font = "";
		MColor lastColor = null;

		JsonObject obj = new JsonObject();
		StringBuilder builder = new StringBuilder(length);

		for (int i = 0; i < length; i++) {
			boolean containsHex = false;
			int count = i + 13; // = #§a§a§e§2§a§5

			// Finds hex colours that may be coming from essentials (&x&f ..) and removes "&" character to match the correct hex colour
			for (int j = i + 1; j < count && j < length; j += 2) {
				if (!(containsHex = text.charAt(j) == '&')) {
					break;
				}
			}

			if (containsHex) {
				text = Global.replaceFrom(text, i, "&", "", 6);
				length = text.length();
			}

			char charAt = text.charAt(i);

			if (charAt == '[' && existingJson != null && index < existingJson.size() && text.charAt(i + 1) == '"' && text.charAt(i + 2) == '"' && text.charAt(i + 3) == ','
					&& text.charAt(i + 4) == '{') {
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

				if (next < length) {
					lastColor = MColor.byCode(text.charAt(next));
				}

				if (lastColor != null) {
					if (builder.length() != 0) {
						obj.addProperty("text", builder.toString());
						jsonList.add(obj);

						obj = new JsonObject();
						builder = new StringBuilder();
					}

					// We don't need these formatting as the default colour is white
					if (lastColor != MColor.WHITE && lastColor != MColor.RESET) {
						if (lastColor.formatter) {
							obj.addProperty(lastColor.propertyName, true);
						} else {
							obj.addProperty("color", lastColor.propertyName);
						}
					}

					i = next;
				} else {
					builder.append(charAt);
				}
			} else if (charAt == '#') {
				int end = i + 7;

				if (!validateHex(text, i + 1, end, length)) {
					builder.append(charAt);
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
				int closeIndex;
				int fromIndex = i + 10;

				if (text.regionMatches(true, i, "{gradient=", 0, 10) && (closeIndex = text.indexOf('}', fromIndex)) != -1) {
					Color[] colors = new Color[2];
					int co = 0;

					for (String one : text.substring(fromIndex, closeIndex).split(":", 2)) {
						if (one.isEmpty() || one.charAt(0) != '#' || !validateHex(one, 1, 6, one.length())) {
							closeIndex = -1;
							break;
						}

						colors[co] = Color.decode(one);
						co++;
					}

					if (co == 2) {
						Color startColor = colors[0];
						Color endColor = colors[1];
						int g = closeIndex + 1;
						int endIndex = text.indexOf("{/gradient}", g);
						int ls = endIndex - 1;

						for (; g < endIndex; g++) {
							obj.addProperty("text", text.charAt(g));

							// Don't know what is this but works
							// https://www.spigotmc.org/threads/470496/
							int red = (int) (startColor.getRed() + g * (float) (endColor.getRed() - startColor.getRed()) / ls);
							int green = (int) (startColor.getGreen() + g * (float) (endColor.getGreen() - startColor.getGreen()) / ls);
							int blue = (int) (startColor.getBlue() + g * (float) (endColor.getBlue() - startColor.getBlue()) / ls);

							// https://stackoverflow.com/questions/4801366
							obj.addProperty("color", String.format("#%06x", ((red & 0xff) << 16) | ((green & 0xff) << 8) | (blue & 0xff)));

							if (lastColor != null && lastColor.formatter) {
								obj.addProperty(lastColor.propertyName, true);
							}

							jsonList.add(obj);

							if (g + 1 < endIndex) {
								obj = new JsonObject();
							}
						}

						lastColor = null;
						i = endIndex + 10;
						continue;
					}
				}

				fromIndex = i + 6;

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

				if (builder.length() != 0) {
					obj.addProperty("text", builder.toString());
					jsonList.add(obj);
					builder = new StringBuilder();
				}

				obj = new JsonObject();
				obj.addProperty("font", font);
				font = "";
				i = closeIndex;
			} else {
				builder.append(charAt);
			}
		}

		obj.addProperty("text", builder.toString());
		jsonList.add(obj);

		return PacketNM.NMS_PACKET.fromJson("[\"\"," + Global.replaceFrom(GSON.toJson(jsonList, List.class), 0, "[", "", 1));
	}

	private boolean validateHex(String text, int start, int end, int length) {
		for (int b = start; b < end; b++) {
			if (b >= length || !Character.isLetterOrDigit(text.charAt(b))) {
				return false;
			}
		}

		return true;
	}

	@SuppressWarnings("deprecation")
	public CompletableFuture<Pair<String, String>> getSkinValue(String uuid) {
		return CompletableFuture.supplyAsync(() -> {
			try (InputStreamReader content = new InputStreamReader(
					new java.net.URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unasigned=false").openStream())) {
				JsonObject json;

				try {
					json = JsonParser.parseReader(content).getAsJsonObject();
				} catch (NoSuchMethodError e) {
					json = new JsonParser().parse(content).getAsJsonObject();
				}

				com.google.gson.JsonArray jsonArray = json.get("properties").getAsJsonArray();

				if (jsonArray.isEmpty()) {
					return null;
				}

				String value = jsonArray.get(0).getAsJsonObject().get("value").getAsString();
				String decodedValue = new String(java.util.Base64.getDecoder().decode(value));

				try {
					json = JsonParser.parseString(decodedValue).getAsJsonObject();
				} catch (NoSuchMethodError e) {
					json = new JsonParser().parse(decodedValue).getAsJsonObject();
				}

				return new Pair<>(value, json.get("textures").getAsJsonObject().get("SKIN").getAsJsonObject().get("url").getAsString());
			} catch (java.io.IOException e1) {
				e1.printStackTrace();
			}

			return null;
		});
	}

	public final class Pair<K, V> {

		public final K key;
		public final V value;

		public Pair(K key, V value) {
			this.key = key;
			this.value = value;
		}
	}
}
