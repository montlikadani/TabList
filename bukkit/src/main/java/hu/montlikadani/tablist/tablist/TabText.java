package hu.montlikadani.tablist.tablist;

import hu.montlikadani.tablist.utils.reflection.JsonComponent;
import hu.montlikadani.tablist.utils.reflection.ReflectionUtils;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * The class which holds both plain text and json texts to append somewhere.
 */
public final class TabText {

	/**
	 * An empty {@link TabText}
	 */
	public static final TabText EMPTY = new TabText();

	// This holds both the original and the updated text in separated instances
	String plainText = "";

	private List<JsonElementData> jsonElements = new ArrayList<>();

	public TabText() {
	}

	public TabText(TabText another) {
		if (another != null) {
			jsonElements = another.jsonElements;
			plainText = another.plainText;
		}
	}

	public String getPlainText() {
		return plainText;
	}

	public void setPlainText(String plainText) {
		this.plainText = plainText;
	}

	/**
	 * @return the list of identified {@link JsonElementData} in the plain text
	 */
	public List<JsonElementData> getJsonElements() {
		return jsonElements;
	}

	/**
	 * Parses {@link TabText} from a plain {@link String} text. If the parameter {@code from} is null this method will
	 * returns {@link #EMPTY}. If not then it tries to parse the text into {@link TabText} containing the json.
	 * 
	 * @param from the plain text to parse
	 * @return the {@link TabText} containing json texts
	 */
	public static TabText parseFromText(String from) {
		if (from == null) {
			return EMPTY;
		}

		TabText tabText = new TabText();
		tabText.updateText(from);
		return tabText;
	}

	/**
	 * Parses this tabText into NMS component to append in tablist.
	 * 
	 * @return the component object (can be IChatBaseComponent), or an empty object if this {@link #EMPTY}
	 */
	public Object toComponent() {
		return this == EMPTY ? ReflectionUtils.EMPTY_COMPONENT : ReflectionUtils.asComponent(this);
	}

	/**
	 * Updates this TabText plainText to the specified one including jsons.
	 * 
	 * @param plainText to update
	 */
	public void updateText(String plainText) {
		this.plainText = plainText;
		findJsonInText(new StringBuilder(plainText));
	}

	// Caching jsons to avoid recreating continuously
	private final List<JsonElementData> skippedDatas = new ArrayList<>(1);

	private void findJsonInText(StringBuilder text) {
		int start, end = 0;

		while ((start = text.indexOf("[\"\",{", end)) != -1) {

			// JSON may contain "raw translate with" array, we should check this also
			if ((end = text.indexOf("]}]", start)) == -1) {
				if ((end = text.indexOf("}]", start)) == -1) {
					break;
				}
			} else {
				end++;
			}

			end += 2;

			if (end > text.length()) {
				end = text.length();
			}

			addJson(text.substring(start, end));
		}
	}

	private void addJson(String str) {
		for (JsonElementData jsonElementData : jsonElements) {
			if (skippedDatas.indexOf(jsonElementData) != -1) {
				return;
			}

			if (jsonElementData.plainJson.equals(str)) {
				skippedDatas.add(jsonElementData);
				return;
			}
		}

		if (!skippedDatas.isEmpty()) {
			jsonElements.clear();
			jsonElements.addAll(skippedDatas);
			skippedDatas.clear();
		}

		try {
			com.google.gson.JsonElement element = null;

			try {
				element = JsonComponent.GSON.fromJson(new StringReader(str), com.google.gson.JsonElement.class);
			} catch (Exception e) { // Compiler fails to understand JsonSyntaxException for legacy versions
			}

			if (element != null) {
				JsonElementDataNew data = new JsonElementDataNew(str);
				data.element = element;

				jsonElements.add(data);
			}
		} catch (Throwable noc) { // For legacy versions
			jsonElements.add(new JsonElementDataOld(str));
		}
	}

	public final class JsonElementDataNew extends JsonElementData {

		public com.google.gson.JsonElement element;

		public JsonElementDataNew(String plainJson) {
			super(plainJson);
		}
	}

	public final class JsonElementDataOld extends JsonElementData {

		public JsonElementDataOld(String plainJson) {
			super(plainJson);
		}
	}

	public class JsonElementData {

		public final String plainJson;
		public final int jsonLength;

		public JsonElementData(String plainJson) {
			this.plainJson = plainJson;
			jsonLength = plainJson.length();
		}
	}
}
