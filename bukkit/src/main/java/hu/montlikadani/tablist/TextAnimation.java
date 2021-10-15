package hu.montlikadani.tablist;

public final class TextAnimation {

	private final String name;
	private final int time, multipliedTime;
	private final boolean random;
	private final String[] texts;

	public TextAnimation(String name, java.util.List<String> texts, int time, boolean random) {
		this.name = name;
		this.time = (time < 0 || time > Integer.MAX_VALUE) ? 150 : time;
		this.random = random;
		this.texts = new String[texts.size()];

		for (int i = 0; i < this.texts.length; i++) {
			this.texts[i] = texts.get(i);
		}

		multipliedTime = this.texts.length * this.time;
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
		return random ? texts[java.util.concurrent.ThreadLocalRandom.current().nextInt(texts.length) % multipliedTime]
				: texts[(int) ((System.currentTimeMillis() % multipliedTime) / time)];
	}

	public String getLastText() {
		return texts[texts.length - 1];
	}
}
