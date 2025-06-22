package dev.aleiis.hintforge.eval;

public class LevenshteinAccuracyCalculator {

    private static int levenshteinDistance(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();

        int[][] dp = new int[len1 + 1][len2 + 1];

        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= len1; i++) {
            char c1 = s1.charAt(i - 1);
            for (int j = 1; j <= len2; j++) {
                char c2 = s2.charAt(j - 1);
                int cost = (c1 == c2) ? 0 : 1;

                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1,        // delete
                             dp[i][j - 1] + 1),       // insert
                             dp[i - 1][j - 1] + cost  // substitution
                );
            }
        }

        return dp[len1][len2];
    }
    
    public static double calculate(String s1, String s2) {
    	return 1 - (double) levenshteinDistance(s1, s2) / Math.max(s1.length(), s2.length());
    }
}
