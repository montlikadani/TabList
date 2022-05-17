package hu.montlikadani.tablist.tablist;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import hu.montlikadani.tablist.utils.reflection.JsonComponent;

public final class TabText {

	public static final TabText EMPTY = new TabText();

	// This holds both the original and the updated text in separated instances
	protected String plainText = "";

	private List<JsonElementData> jsonElements = new ArrayList<>();

	public TabText() {
	}

	public TabText(TabText another) {
		jsonElements = another.jsonElements;
		plainText = another.plainText;
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
		tabText.plainText = from;

		tabText.findJsonInText(from);
		return tabText;
	}

	public void updateText(String plainText) {
		this.plainText = plainText;
		findJsonInText(plainText);
	}

	// Caching jsons to avoid recreating continuously
	private final List<JsonElementData> skippedDatas = new ArrayList<>();

	public void findJsonInText(String text) {
		int start, end = 0;
		int length = text.length();

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

			if (end > length) {
				end = length;
			}

			addJson(text.substring(start, end));
		}
	}

	public void addJson(String str) {
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
			JsonElement element = JsonComponent.GSON.fromJson(new StringReader(str), JsonElement.class);

			if (element != null) {
				JsonElementData data = new JsonElementData();

				data.element = element;
				data.plainJson = str;

				jsonElements.add(data);
			}
		} catch (JsonSyntaxException | JsonIOException e) {
		}
	}

	public class JsonElementData {

		public JsonElement element;
		public String plainJson;

	}
}
