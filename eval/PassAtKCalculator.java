package dev.aleiis.hintforge.eval;

import java.math.BigInteger;

public class PassAtKCalculator {

    private static BigInteger binomial(int n, int k) {
    	if (n < 0 ) return BigInteger.ZERO;
        if (k < 0 || k > n) return BigInteger.ZERO;
        if (k == 0 || k == n) return BigInteger.ONE;
        
        k = Math.min(k, n - k);

        BigInteger result = BigInteger.ONE;
        for (int i = 1; i <= k; i++) {
            result = result.multiply(BigInteger.valueOf(n - i + 1))
                           .divide(BigInteger.valueOf(i));
        }
        return result;
    }

    /**
     * Computes the estimated Pass@k metric for code generation evaluation.
     *
     * @param n total number of generated samples (n ≥ 1)
     * @param c number of correct samples (0 ≤ c ≤ n)
     * @param k number of top samples to consider (1 ≤ k ≤ n)
     * 
     * @return the estimated Pass@k value in the range [0.0, 1.0]
     * 
     * @throws IllegalArgumentException if k > n
     */
    public static double compute(int n, int c, int k) {
        if (k > n) {
            throw new IllegalArgumentException("k cannot be greater than n");
        }

        if (n - c < k) return 1.0;

        return 1.0 - binomial(n - c, k).doubleValue() / binomial(n, k).doubleValue();
    }

}
