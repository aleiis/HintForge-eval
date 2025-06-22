package dev.aleiis.hintforge.eval;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.aleiis.hintforge.assistant.ContextAwareCompletionAssistant;
import dev.aleiis.hintforge.model.DslProfile;

public class FragmentCompletionTest extends Test {

	private static String MODEL_NAME = "gpt-4o-mini";

	private static int total = 0;
	private static int correct = 0;
	private static double bleuSum = 0;
	private static List<TestResult> results = new ArrayList<>();
	private static BleuCalculator bleuCalculator = new BleuCalculator();

	private static void test(ContextAwareCompletionTestCase testCase, String fileName) {

		ContextAwareCompletionAssistant assistant = new ContextAwareCompletionAssistant(Path.of("."), API_KEY, profile,
				MODEL_NAME);
		assistant.setEmbeddingStore(embeddingStore);

		String completion;
		try {
			completion = assistant.suggest(testCase.instruction, testCase.context, testCase.offset);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		String intent = testCase.context.substring(0, testCase.offset) + completion
				+ testCase.context.substring(testCase.offset);

		SyntaxChecker checker = new SyntaxChecker();
		List<String> errors = checker.validate(intent, true);

		boolean passed = errors.isEmpty();
		if (passed) {
			correct += 1;
		}

		total += 1;

		double bleu = bleuCalculator.calculateBLEU(testCase.expected, completion, 4);

		bleuSum += bleu;

		results.add(new TestResult(fileName, bleu, passed));
	}

	public static void main(String[] args) {

		System.out.println("[INFO] Working Directory: " + System.getProperty("user.dir"));

		init();

		System.out.println("[INFO] DSL Profile: " + DslProfile.toJson(new DslProfile[] { profile }));

		Path testCasesFolder = Path.of("resources/tests/completion_fragment");
		try (Stream<Path> paths = Files.list(testCasesFolder)) {
			List<Path> jsonFiles = paths.filter(path -> path.toString().endsWith(".json")).collect(Collectors.toList());

			System.out.println(String.format("[INFO] Reached %d test cases files", jsonFiles.size()));

			for (Path path : jsonFiles) {
				ContextAwareCompletionTestCase testCase = ContextAwareCompletionTestCase.fromJson(path);
				if (testCase != null) {
					System.out.println(String.format("[INFO] Testing '%s'", path.getFileName().toString()));
					FragmentCompletionTest.test(testCase, path.getFileName().toString());
				} else {
					System.err.println("[WARN] Could not parse file: " + path);
				}
			}

		} catch (IOException e) {
			System.err.println("[ERROR] Failed to read test cases: " + e.getMessage());
			e.printStackTrace();
			return;
		}

		System.out.println("[INFO] Total test cases: " + String.valueOf(total));
		System.out.println("[INFO] Passed test cases: " + String.valueOf(correct));

		if (total <= 0) {
			System.out.println("[ERROR] Evaluation cannot continue because n <= 0");
			return;
		}

		int[] valuesOfK = new int[] { 1, 5, 10 };
		for (int k : valuesOfK) {
			if (total >= k) {
				System.out.println(
						String.format("[INFO] Pass@%d: %.6f", k, PassAtKCalculator.compute(total, correct, k)));
			} else {
				System.out.println(String.format("[ERROR] Pass@%d could not be computed because k > n", k));
			}
		}
		System.out.println(String.format("[INFO] BLEU: %.4f", bleuSum / total));

		Path output = Path.of("resources/results/completion_fragment/" + MODEL_NAME + ".dat");
		try {
		    Files.createDirectories(output.getParent());
		    try (BufferedWriter writer = Files.newBufferedWriter(output)) {
		        writer.write("file\tbleu\tcorrect\n");
		        for (TestResult r : results) {
		            writer.write(r.toString());
		            writer.newLine();
		        }
		        System.out.println("[INFO] Results written to: " + output.toAbsolutePath());
		    }

		} catch (IOException e) {
		    System.err.println("[ERROR] Failed to write results to file: " + output.toAbsolutePath());
		    e.printStackTrace();
		}


	}

	private static record TestResult(String file, double bleu, boolean correct) {
		@Override
		public String toString() {
			return String.format("%s\t%.4f\t%s", file, bleu, correct);
		}
	}

}
