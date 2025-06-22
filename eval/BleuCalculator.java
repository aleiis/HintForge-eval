package dev.aleiis.hintforge.eval;

import java.util.*;
import java.util.Map.Entry;

public class BleuCalculator {
	
	private ITokenizer tokenizer;
	
	public BleuCalculator() {
		this.tokenizer = new SimpleCodeTokenizer();
	}
	
	public BleuCalculator(ITokenizer tokenizer) {
		this.tokenizer = tokenizer;
	}
	
	public double calculateBLEU(String reference, String candidate, int maxN) {
		List<String> referenceTokenized = this.tokenizer.tokenize(reference);
		List<String> candidateTokenized = this.tokenizer.tokenize(candidate);
		return this.calculateBLEU(referenceTokenized, candidateTokenized, maxN);
	}

    public double calculateBLEU(List<String> reference, List<String> candidate, int maxN) {
        double brevityPenalty = computeBrevityPenalty(reference, candidate);
        double geometricMean = 1.0;

        for (int n = 1; n <= maxN; n++) {
            double precision = modifiedNGramPrecision(reference, candidate, n);
            if (precision == 0) {
                return 0.0;  // avoid log(0)
            }
            geometricMean *= Math.pow(precision, 1.0 / maxN);
        }

        return brevityPenalty * geometricMean;
    }

    private static double computeBrevityPenalty(List<String> reference, List<String> candidate) {
        double refLength = reference.size();
        double candLength = candidate.size();

        if (candLength > refLength) {
            return 1.0;
        } else {
            return Math.exp(1.0 - refLength / candLength);
        }
    }

    private static double modifiedNGramPrecision(List<String> reference, List<String> candidate, int n) {
        Map<String, Integer> refNGrams = getNGramCounts(reference, n);
        Map<String, Integer> candNGrams = getNGramCounts(candidate, n);

        int overlap = 0;
        int total = 0;

        for (Entry<String, Integer> entry : candNGrams.entrySet()) {
            String ngram = entry.getKey();
            int count = entry.getValue();

            int refCount = refNGrams.getOrDefault(ngram, 0);
            overlap += Math.min(count, refCount);
            total += count;
        }

        return total > 0 ? (double) overlap / total : 0.0;
    }

    private static Map<String, Integer> getNGramCounts(List<String> tokens, int n) {
        Map<String, Integer> counts = new HashMap<>();
        for (int i = 0; i <= tokens.size() - n; i++) {
            List<String> ngramList = tokens.subList(i, i + n);
            String ngram = String.join(" ", ngramList);
            counts.put(ngram, counts.getOrDefault(ngram, 0) + 1);
        }
        return counts;
    }
}
