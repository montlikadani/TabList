package hu.montlikadani.tablist.utils.variables;

import java.time.Instant;
import java.util.function.Function;

final class TimedVariable {

	public final String fullName;
	public final transient Function<TimedVariable, String> function;

	private final int refreshSeconds;

	private transient Instant refreshInstant;

	private String keptValue;

	public TimedVariable(String name, int refreshSeconds, Function<TimedVariable, String> function) {
		this.function = function;
		this.refreshSeconds = refreshSeconds;

		fullName = '%' + name + '%';
	}

	public String getKeptValue() {
		return keptValue;
	}

	public String keptValue(String keptValue) {
		return this.keptValue = keptValue;
	}

	public boolean canReplace(String str) {
		if (refreshInstant != null && refreshInstant.isAfter(Instant.now())) {
			return false;
		}

		if (str.indexOf(fullName) != -1) {
			refreshInstant = Instant.now().plusSeconds(refreshSeconds);
			return true;
		}

		return false;
	}
}
