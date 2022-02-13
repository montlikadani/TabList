package hu.montlikadani.tablist;

import java.util.Random;

public final class TextAnimation {

	private final String name, firstElement;
	private final int time, multipliedTime;
	private final boolean isRandom;
	private final String[] texts;

	private Random random;

	public TextAnimation(String name, java.util.List<String> texts, int time, boolean isRandom) {
		this.name = name;
		this.time = (time < 0 || time > Integer.MAX_VALUE) ? 150 : time;
		this.isRandom = isRandom;
		this.texts = new String[texts.size()];

		if (isRandom) {
			random = new Random();
		}

		for (int i = 0; i < this.texts.length; i++) {
			this.texts[i] = texts.get(i);
		}

		firstElement = this.texts[0];
		multipliedTime = this.texts.length * this.time;
	}

	public String getName() {
		return name;
	}

	public String getText() {
		if (time < 1) {
			return firstElement;
		}

		return isRandom ? texts[random.nextInt(texts.length) % multipliedTime]
				: texts[(int) ((System.currentTimeMillis() % multipliedTime) / time)];
	}
}
