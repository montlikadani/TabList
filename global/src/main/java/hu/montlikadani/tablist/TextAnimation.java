package hu.montlikadani.tablist;

import java.util.Random;

public final class TextAnimation {

	public final String name;

	private final String firstElement;
	private final int time, multipliedTime;
	private final boolean isRandom;
	private final String[] texts;

	private Random random;

	public TextAnimation(String name, java.util.List<String> texts, int time, boolean isRandom) {
		this.name = name;
		this.time = time < 0 ? 150 : time;
		this.isRandom = isRandom;
		this.texts = texts.toArray(new String[0]);

		if (isRandom) {
			random = new Random();
		}

		firstElement = this.texts[0];
		multipliedTime = this.texts.length * this.time;
	}

	public String getText() {
		if (time < 1) {
			return firstElement;
		}

		return isRandom ? texts[random.nextInt(texts.length) % multipliedTime] : texts[(int) ((System.currentTimeMillis() % multipliedTime) / time)];
	}
}
