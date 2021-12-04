package hu.montlikadani.tablist.utils.variables.simplePlaceholder;

import java.util.Locale;

public enum PluginPlaceholders {

	// Player placeholders
	PING, EXP_TO_LEVEL, LEVEL, LIGHT_LEVEL, IP_ADDRESS, XP, PLAYER_MAX_HEALTH, PLAYER_HEALTH, PLAYER_DISPLAYNAME,
	PLAYER_GAMEMODE, WORLD, PLAYER_UUID, PLAYER,

	// Misc tablist placeholders
	TPS, TPS_OVERFLOW, SERVER_RAM_USED, SERVER_RAM_MAX, SERVER_RAM_FREE, SERVER_TIME, MEMORY_BAR,

	// Misc placeholderapi variables to retrieve in synchronous thread
	SERVER_TOTAL_CHUNKS(true), SERVER_TOTAL_LIVING_ENTITIES(true), SERVER_TOTAL_ENTITIES(true);

	public final String name;

	private boolean isPapiVariable = false;

	PluginPlaceholders() {
		if (name().equals("MEMORY_BAR")) {
			name = '%' + name().toLowerCase(Locale.ENGLISH) + '%';
		} else {
			name = '%' + name().replace('_', '-').toLowerCase(Locale.ENGLISH) + '%';
		}
	}

	PluginPlaceholders(boolean papiVariable) {
		isPapiVariable = papiVariable;
		name = '%' + name().toLowerCase(Locale.ENGLISH) + '%';
	}

	public boolean isPapiVariable() {
		return isPapiVariable;
	}
}
