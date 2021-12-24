package hu.montlikadani.tablist.user;

public final class PlayerScore {

	private final String scoreName;

	private int lastScore = -1;
	private boolean isObjectiveCreated = false;

	public PlayerScore(String scoreName) {
		this.scoreName = scoreName;
	}

	public String getScoreName() {
		return scoreName;
	}

	public int getLastScore() {
		return lastScore;
	}

	public void setLastScore(int lastScore) {
		this.lastScore = lastScore;
	}

	public boolean isObjectiveCreated() {
		return isObjectiveCreated;
	}

	public void setObjectiveCreated() {
		isObjectiveCreated = !isObjectiveCreated;
	}
}
