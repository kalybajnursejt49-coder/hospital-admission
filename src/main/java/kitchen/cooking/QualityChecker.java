package kitchen.cooking;

public class QualityChecker {
    public static final int MIN_SCORE = 60;

    public boolean passes(int score) {
        return score >= MIN_SCORE;
    }
}
