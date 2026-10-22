package ua.kidlearn.attempts;

/**
 * One scene of one attempt as the teacher sees it (QA1-031): how it went, never what the child
 * chose — the answers themselves stay with the parents (docs/Ролі_та_приватність.md §3, §6).
 *
 * @param type scene type from the lesson's scenario (choice, sorting, dialog), or null if unknown
 * @param control the lesson's control scene, played without hints
 * @param correctFirstTry every first try in the scene was right (for sorting: every card went to
 * the right basket the first time)
 * @param tries the most tries any answer in the scene needed
 * @param hintsUsed the highest hint level the guide reached in the scene (0–2)
 */
public record TeacherSceneResult(String sceneKey, String type, boolean control, boolean correctFirstTry, int tries,
		int hintsUsed) {
}
