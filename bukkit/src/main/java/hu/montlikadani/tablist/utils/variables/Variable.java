package hu.montlikadani.tablist.utils.variables;

import java.time.Instant;
import java.util.function.BiConsumer;

final class Variable {

	public final String name, fullName;
	private final int refreshSeconds;

	private BiConsumer<Variable, String> consumer;
	private Instant rateInstant;
	private String remainingValue;

	public Variable(String name, int refreshSeconds) {
		this.name = name;
		fullName = '%' + name + '%';
		this.refreshSeconds = refreshSeconds;
	}

	public String getRemainingValue() {
		return remainingValue;
	}

	Variable setVariable(BiConsumer<Variable, String> consumer) {
		this.consumer = consumer;
		return this;
	}

	public BiConsumer<Variable, String> getReplacer() {
		return consumer;
	}

	public String setAndGetRemainingValue(String remainingValue) {
		return this.remainingValue = remainingValue;
	}

	public boolean canReplace(String str) {
		if (rateInstant != null && rateInstant.isAfter(Instant.now())) {
			return false;
		}

		if (str.indexOf(fullName) != -1) {
			rateInstant = Instant.now().plusSeconds(refreshSeconds);
			return true;
		}

		return false;
	}
}
