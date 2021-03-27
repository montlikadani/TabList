package hu.montlikadani.tablist;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class AnimCreator {

	private String animName;
	private int time;
	private boolean random;

	private final List<String> texts = new ArrayList<>();

	public AnimCreator(String animName, List<String> texts, int time, boolean random) {
		this.animName = animName;
		this.time = time < 0 ? 0 : time;
		this.random = random;

		if (texts != null) {
			this.texts.addAll(texts);
		}
	}

	public String getAnimName() {
		return animName;
	}

	public List<String> getTexts() {
		return new ArrayList<>(texts);
	}

	public boolean isRandom() {
		return random;
	}

	public int getTime() {
		return time;
	}

	public String getRandomText() {
		int size = texts.size();
		return random ? texts.get(ThreadLocalRandom.current().nextInt(size) % (size * time))
				: texts.get((int) (System.currentTimeMillis() % (size * time) / time));
	}

	public String getFirstText() {
		return texts.get(0);
	}

	public String getLastText() {
		return texts.get(texts.size() - 1);
	}
}