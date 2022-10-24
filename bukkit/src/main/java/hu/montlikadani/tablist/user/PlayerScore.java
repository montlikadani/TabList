package hu.montlikadani.tablist.user;

/**
 * The class which holds information about player's score
 */
public final class PlayerScore {

	private final String scoreName;

	private int lastScore = -1;
	private boolean isObjectiveCreated = false;

	public PlayerScore(String scoreName) {
		this.scoreName = scoreName;
	}

	/**
	 * @return the score name
	 */
	public String getScoreName() {
		return scoreName;
	}

	/**
	 * @return the last cached score value
	 */
	public int getLastScore() {
		return lastScore;
	}

	/**
	 * Sets the last score cache
	 * 
	 * @param lastScore
	 */
	public void setLastScore(int lastScore) {
		this.lastScore = lastScore;
	}

	/**
	 * @return true if the objective is already created
	 */
	public boolean isObjectiveCreated() {
		return isObjectiveCreated;
	}

	/**
	 * Sets the objective as created or not
	 */
	public void setObjectiveCreated() {
		isObjectiveCreated = !isObjectiveCreated;
	}
}
