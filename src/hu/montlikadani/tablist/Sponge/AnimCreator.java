package hu.montlikadani.tablist.Sponge;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class AnimCreator {

	private String animName;
	private ArrayList<String> texts;
	private int time;
	private boolean random;

	public AnimCreator(String animName, ArrayList<String> texts, int time) {
		this(animName, texts, time, false);
	}

	public AnimCreator(String animName, ArrayList<String> texts, int time, boolean random) {
		this.animName = animName;
		this.texts = texts;
		this.time = time;
		this.random = random;
	}

	public AnimCreator(String animName, ArrayList<String> texts) {
		this(animName, texts, 0, false);
	}

	public AnimCreator(String animName, ArrayList<String> texts, boolean random) {
		this(animName, texts, 0, random);
	}

	public String getAnimName() {
		return animName;
	}

	public ArrayList<String> getTexts() {
		return texts;
	}

	public String getRandomText() {
		int size = texts.size();
		return random ? texts.get(ThreadLocalRandom.current().nextInt(size) % (size * time))
				: texts.get(((int) (System.currentTimeMillis() % (size * time) / time)));
	}

	public int getTime() {
		return time;
	}

	public String getFirstText() {
		return texts.get(0);
	}

	public String getLastText() {
		return texts.get(texts.size() - 1);
	}

	public boolean isRandom() {
		return random;
	}
}