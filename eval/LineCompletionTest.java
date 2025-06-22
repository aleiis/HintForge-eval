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

public class LineCompletionTest extends Test {

	private static String MODEL_NAME = "gpt-4o-mini";
	
	private static int total = 0;
	private static int correctEM = 0;
	private static int correctSyntax = 0;
	private static double sumLevenshtein = 0;
	
	private static final List<TestResult> results = new ArrayList<>();
	
	private static void test(ContextAwareCompletionTestCase testCase, String fileName) {
		
		ContextAwareCompletionAssistant assistant = new ContextAwareCompletionAssistant(Path.of("."), API_KEY, profile, MODEL_NAME);
		
		String instructions = testCase.instruction;
		
		String completion;
		try {
			completion = assistant.suggest(instructions, testCase.context, testCase.offset);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		double levenshtein = LevenshteinAccuracyCalculator.calculate(completion, testCase.expected);
		sumLevenshtein += levenshtein;
		
		boolean em = completion.strip().equals(testCase.expected.strip());
		if (em) correctEM++;
		
		String intent = testCase.context.substring(0, testCase.offset) + completion + testCase.context.substring(testCase.offset);
		SyntaxChecker checker = new SyntaxChecker();
		boolean compiles = checker.validate(intent, true).isEmpty();
		if (compiles) correctSyntax++;
		
		total += 1;
		
		results.add(new TestResult(fileName, levenshtein, em, compiles));
	}

	public static void main(String[] args) {
		
		System.out.println("[INFO] Working Directory: " + System.getProperty("user.dir"));

		init();

		System.out.println("[INFO] DSL Profile: " + DslProfile.toJson(new DslProfile[] { profile }));

		Path testCasesFolder = Path.of("resources/tests/completion_line");
		try (Stream<Path> paths = Files.list(testCasesFolder)) {
		    List<Path> jsonFiles = paths
		        .filter(path -> path.toString().endsWith(".json"))
		        .collect(Collectors.toList());
		    
		    System.out.println(String.format("[INFO] Reached %d test cases files", jsonFiles.size()));

		    for (Path path : jsonFiles) {
		        ContextAwareCompletionTestCase testCase = ContextAwareCompletionTestCase.fromJson(path);
		        if (testCase != null) {
		        	System.out.println(String.format("[INFO] Testing '%s'", path.getFileName().toString()));
		            LineCompletionTest.test(testCase, path.getFileName().toString());
		        } else {
		            System.err.println("[WARN] Could not parse file: " + path);
		        }
		    }

		} catch (IOException e) {
		    System.err.println("[ERROR] Failed to read test cases: " + e.getMessage());
		    e.printStackTrace();
		    return;
		}

		if (total <= 0) {
			System.out.println("[ERROR] Evaluation cannot continue because there are not enough test cases");
			return;
		}

		System.out.println("[INFO] Total test cases: " + total);
		System.out.println("[INFO] # of test cases that passed EM: " + correctEM);
		System.out.println("[INFO] # of test cases with no syntax errors: " + correctSyntax);
		System.out.println("[INFO] EM: " + (double) correctEM / total);
		System.out.println("[INFO] Levenshtein Accuracy: " + (double) sumLevenshtein / total);

		int[] valuesOfK = new int[] {1, 5, 10};
		for (int k : valuesOfK) {
			if (total >= k) {
				System.out.println(String.format("[INFO] Pass@%d: %.6f", k, PassAtKCalculator.compute(total, correctSyntax, k)));
			} else {
				System.out.println(String.format("[ERROR] Pass@%d could not be computed because k > n", k));
			}
		}

		Path output = Path.of("resources/results/completion_line/" + MODEL_NAME + ".dat");
		try {
			Files.createDirectories(output.getParent());
			try (BufferedWriter writer = Files.newBufferedWriter(output)) {
				writer.write("file\tlevenshtein\tem\tcompiles\n");
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
	
	
	private static record TestResult(String file, double levenshtein, boolean em, boolean compiles) {
		@Override
		public String toString() {
			return String.format("%s\t%.4f\t%s\t%s", file, levenshtein, em, compiles);
		}
	}
}
