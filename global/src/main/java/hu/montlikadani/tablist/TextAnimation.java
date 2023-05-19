package hu.montlikadani.tablist;

import java.util.Random;

public final class TextAnimation {

	public final String name;

	private final int time;
	private final boolean randomized;
	private final String[] texts;

	private Random random;
	private int last;

	public TextAnimation(String name, java.util.List<String> texts, int time, boolean randomized) {
		this.name = name;
		this.time = time < 1 ? 150 : time;
		this.randomized = randomized;
		this.texts = texts.toArray(new String[0]);

		if (randomized) {
			random = new Random();
		}
	}

	public String next() {
		int index = (int) ((System.currentTimeMillis() % (texts.length * time)) / time);

		if (last != index && randomized) {

			// We're using Fisher–Yates shuffle algorithm to randomize
			for (int i = texts.length - 1; i > 0; i--) {
				int randomIndexToSwap = random.nextInt(i + 1);
				String temp = texts[randomIndexToSwap];

				texts[randomIndexToSwap] = texts[i];
				texts[i] = temp;
			}

			last = index;
		}

		return texts[index];
	}
}
