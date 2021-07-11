package hu.montlikadani.tablist.bukkit.utils.variables;

import java.time.Instant;
import java.util.function.BiConsumer;

final class Variable {

	public final String name, fullName;
	private final int refreshSeconds;

	private BiConsumer<Variable, String> consumer;
	private boolean replacedOnce = false;
	private Instant rateInstant;
	private String remainingValue;

	public Variable(String name, int refreshSeconds) {
		this.name = name;
		fullName = '%' + name + '%';
		this.refreshSeconds = refreshSeconds;
	}

	public boolean isReplacedBefore() {
		return replacedOnce;
	}

	public String getRemainingValue() {
		return remainingValue;
	}

	Variable setVariable(BiConsumer<Variable, String> consumer) {
		this.consumer = consumer;
		return this;
	}

	public BiConsumer<Variable, String> getReplacer() {
		if (refreshSeconds == -1) {
			replacedOnce = true;
		}

		return consumer;
	}

	public String setAndGetRemainingValue(String remainingValue) {
		this.remainingValue = remainingValue;
		return remainingValue;
	}

	public boolean canReplace(String str) {
		if (refreshSeconds == -1) {
			return true;
		}

		if (rateInstant != null && rateInstant.isAfter(Instant.now())) {
			return false;
		}

		if (str.indexOf(fullName) >= 0) {
			rateInstant = Instant.now().plusSeconds(refreshSeconds);
			return true;
		}

		return false;
	}
}
