package hu.montlikadani.tablist;

public final class TextAnimation {

	private String name;
	private int time;
	private boolean random;

	private String[] texts;

	public TextAnimation(String name, java.util.List<String> texts, int time, boolean random) {
		this.name = name;
		this.time = (time < 0 || time > Integer.MAX_VALUE) ? 150 : time;
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
		return random
				? texts[java.util.concurrent.ThreadLocalRandom.current().nextInt(texts.length) % (texts.length * time)]
				: texts[(int) ((System.currentTimeMillis() % (texts.length * time)) / time)];
	}

	public String getLastText() {
		return texts[texts.length - 1];
	}
}
