package hu.montlikadani.tablist;

import java.util.List;

public final class TextAnimation {

	private String name;
	private int time;
	private boolean random;

	private String[] texts;

	private int index = -1;

	public TextAnimation(String name, List<String> texts, int time, boolean random) {
		this.name = name;
		this.time = time < 0 ? 0 : time;
		this.random = random;
		this.texts = new String[texts.size()];

		for (int i = 0; i < this.texts.length; i++) {
			this.texts[i] = texts.get(i);
		}
	}

	public String getName() {
		return name;
	}

	public String[] getTexts() {
		return texts;
	}

	public boolean isRandom() {
		return random;
	}

	public int getTime() {
		return time;
	}

	public String getText() {
		if (index + 1 >= texts.length) {
			index = -1;
		}

		++index;

		return random
				? texts[java.util.concurrent.ThreadLocalRandom.current().nextInt(texts.length) % (texts.length * time)]
				: texts[(int) ((index * time % System.currentTimeMillis()) / time)];
	}

	public String getLastText() {
		return texts[texts.length - 1];
	}
}
