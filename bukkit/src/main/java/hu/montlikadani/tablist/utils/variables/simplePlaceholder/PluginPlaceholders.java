package hu.montlikadani.tablist.utils.variables.simplePlaceholder;

import java.util.Locale;

public enum PluginPlaceholders {

	// Player placeholders
	PING, EXP_TO_LEVEL, LEVEL, LIGHT_LEVEL;

	public final String name;

	PluginPlaceholders() {
		name = '%' + name().replace('_', '-').toLowerCase(Locale.ENGLISH) + '%';
	}
}
