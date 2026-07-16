package ua.kidlearn.attempts;

public record AnswerEntry(String sceneKey, int tryNo, String chosenOption, boolean isCorrect, short hintsUsed) {

	static AnswerEntry from(SceneAnswer answer) {
		return new AnswerEntry(answer.getSceneKey(), answer.getTryNo(), answer.getChosenOption(),
				answer.isCorrect(), answer.getHintsUsed());
	}

}
