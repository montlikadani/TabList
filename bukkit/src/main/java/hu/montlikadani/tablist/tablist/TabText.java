package hu.montlikadani.tablist.tablist;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import hu.montlikadani.tablist.utils.reflection.JsonComponent;

public final class TabText {

	public static final TabText EMPTY = new TabText();

	// This holds both the original and the updated text in separated instances
	protected String plainText = "";

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

	public List<JsonElementData> getJsonElements() {
		return jsonElements;
	}

	public static TabText parseFromText(String from) {
		if (from == null) {
			return EMPTY;
		}

		TabText tabText = new TabText();
		tabText.updateText(from);
		return tabText;
	}

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
