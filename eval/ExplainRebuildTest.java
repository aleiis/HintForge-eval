package dev.aleiis.hintforge.eval;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.aleiis.hintforge.assistant.CodeExplanationAssistant;
import dev.aleiis.hintforge.assistant.ContextAwareCompletionAssistant;

public class ExplainRebuildTest extends Test {
	
	private static String MODEL_NAME = "gpt-4o-mini";

	private static int totalTests = 0;
	private static double sumBleu = 0.0;

	private static List<ResultRow> results = new ArrayList<>();

	private static void test(String testCase, String scriptName) {

		CodeExplanationAssistant explanationAssistant = new CodeExplanationAssistant(Path.of("."), API_KEY, profile, MODEL_NAME);
		explanationAssistant.setEmbeddingStore(embeddingStore);

		String explanation;
		try {
			explanation = explanationAssistant.explain(testCase);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		ContextAwareCompletionAssistant completionAssistant = new ContextAwareCompletionAssistant(Path.of("."), API_KEY,
				profile, "gpt-4o-mini");
		completionAssistant.setEmbeddingStore(embeddingStore);

		String instruction = "Write an entire script using the following explanation:\n\n" + explanation;

		String rebuildedCode;
		try {
			rebuildedCode = completionAssistant.suggest(instruction, "", 0);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		BleuCalculator bleu = new BleuCalculator();
		double bleuScore = bleu.calculateBLEU(testCase, rebuildedCode, 4);

		results.add(new ResultRow(scriptName, testCase.length(), rebuildedCode.length(), bleuScore));

		sumBleu += bleuScore;
		totalTests += 1;
	}

	public static void main(String[] args) {

		System.out.println("[INFO] Working Directory: " + System.getProperty("user.dir"));

		init();

		Path testCasesFolder = Path.of("resources/tests/explain_rebuild");
		try (Stream<Path> paths = Files.list(testCasesFolder)) {
			List<Path> graphqlFiles = paths.filter(path -> path.toString().endsWith(".graphql"))
					.collect(Collectors.toList());

			System.out.println(String.format("[INFO] Reached %d test cases files", graphqlFiles.size()));

			for (Path path : graphqlFiles) {
				String testCase = Files.readString(path);
				if (testCase != null) {
					String scriptName = path.getFileName().toString();
					System.out.println(String.format("[INFO] Testing '%s'", scriptName));
					ExplainRebuildTest.test(testCase, scriptName);
				} else {
					System.err.println("[WARN] Could not parse file: " + path);
				}
			}

		} catch (IOException e) {
			System.err.println("[ERROR] Failed to read test cases: " + e.getMessage());
			e.printStackTrace();
			return;
		}

		// Metrics
		System.out.println("[INFO] Total test cases: " + String.valueOf(totalTests));

		if (totalTests <= 0) {
			System.out.println("[ERROR] Evaluation cannot continue because there are not enough evaluated test cases");
			return;
		}

		System.out.println(String.format("[INFO] BLEU: %.6f", sumBleu / totalTests));

		// Save results
		Path outputDir = Path.of("resources/results/explain_rebuild");
		try {
		    Files.createDirectories(outputDir);
		    try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputDir.resolve(MODEL_NAME + ".dat").toFile()))) {
		        writer.write("ScriptName\tOriginalLength\tRegeneratedLength\tBLEU\n");
		        for (ResultRow row : results) {
		            writer.write(String.format("%s\t%d\t%d\t%.6f\n",
		                    row.scriptName(), row.originalLength(), row.regeneratedLength(), row.bleu()));
		        }
		    }
		} catch (IOException e) {
		    System.err.println("[ERROR] Failed to write results to file: " + e.getMessage());
		}
	}

	private static record ResultRow(String scriptName, int originalLength, int regeneratedLength, double bleu) {
	}
}
