package dev.aleiis.hintforge.eval;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.aleiis.hintforge.assistant.IdentifierSuggestionAssistant;
import dev.aleiis.hintforge.model.DslProfile;

public class IdentifierSuggestionTest extends Test {

	private static String MODEL_NAME = "gpt-4o-mini";
	
	private static int total = 0;
	private static int correct = 0;
	private static HashMap<String, ResultRow> results = new HashMap<>();
	
	
	private static void test(IdentifierSuggestionTestCase testCase) {
		
		IdentifierSuggestionAssistant assistant = new IdentifierSuggestionAssistant(Path.of("."), API_KEY, profile, "gpt-4o-mini");
		assistant.setEmbeddingStore(embeddingStore);
		
		List<String> suggestedIdentifiers;
		try {
			suggestedIdentifiers = assistant.suggest(testCase.context, testCase.offset);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		if (!results.containsKey(testCase.kind)) {
			results.put(testCase.kind, new ResultRow());
		}
		ResultRow resultRow = results.get(testCase.kind);
		
		int position = -1;
		for (int i = 0; i < suggestedIdentifiers.size(); i++) {
			if (suggestedIdentifiers.get(i).strip().equals(testCase.expected.strip())) {
				position = i + 1;
				correct++;
				resultRow.pass++;
				break;
			}
		}
		
		if (position != -1) {
			resultRow.mrrAtK.score(position);
		} else {
			resultRow.mrrAtK.score();
		}
		
		total += 1;
		resultRow.total++;
	}

	public static void main(String[] args) {
		
		System.out.println("[INFO] Working Directory: " + System.getProperty("user.dir"));

		init();
		
		System.out.println("[INFO] DSL Profile: " + DslProfile.toJson(new DslProfile[] { profile }));

		Path testCasesFolder = Path.of("resources/tests/identifiers_suggestion");
		try (Stream<Path> paths = Files.list(testCasesFolder)) {
		    List<Path> jsonFiles = paths
		        .filter(path -> path.toString().endsWith(".json"))
		        .collect(Collectors.toList());
		    
		    System.out.println(String.format("[INFO] Reached %d test cases files", jsonFiles.size()));

		    for (Path path : jsonFiles) {
		        IdentifierSuggestionTestCase testCase = IdentifierSuggestionTestCase.fromJson(path);
		        if (testCase != null) {
		        	System.out.println(String.format("[INFO] Testing '%s'", path.getFileName().toString()));
		            IdentifierSuggestionTest.test(testCase);
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
			System.out.println("[ERROR] Evaluation cannot continue because there are not enough test cases");
			return;
		}
		
		System.out.println("[INFO] Global pass rate: " + String.valueOf((double) correct / total));
		
		// Save results
		Path outputDir = Path.of("resources/results/identifiers_suggestion");
		try {
		    Files.createDirectories(outputDir);
		    try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputDir.resolve(MODEL_NAME + ".dat").toFile()))) {
		        writer.write("kind\tmrr@5\ttotal\tpass\n");
		        for (String kind : results.keySet()) {
					ResultRow result = results.get(kind);
					writer.write(String.format("%s\t%.6f\t%d\t%d\n", kind, result.mrrAtK.collect(), result.total, result.pass));
				}		
		    }
		} catch (IOException e) {
		    System.err.println("[ERROR] Failed to write results to file: " + e.getMessage());
		}	
	}
	
	private static class ResultRow {
		MRRAtKCalculator mrrAtK = new MRRAtKCalculator(5);
		int total;
		int pass;
	}
}
