package hu.montlikadani.tablist.utils.variables.simplePlaceholder;

public final class SimplePluginPlaceholder {

	public static final PluginPlaceholders[] VALUES = PluginPlaceholders.values();

	public static SimplePluginPlaceholder findOne(String str) {
		for (PluginPlaceholders placeholder : VALUES) {
			if (!placeholder.isPapiVariable() && str.indexOf(placeholder.name) != -1) {
				return new SimplePluginPlaceholder(placeholder);
			}
		}

		return null;
	}

	public final PluginPlaceholders placeholder;

	private SimplePluginPlaceholder(PluginPlaceholders placeholder) {
		this.placeholder = placeholder;
	}
}
