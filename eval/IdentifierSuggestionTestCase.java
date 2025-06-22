package dev.aleiis.hintforge.eval;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

public class IdentifierSuggestionTestCase {

	public String kind;
	public String context;
	public String expected;
	public int offset;
	
	public static IdentifierSuggestionTestCase fromJson(String json) {
		if (json != null && !json.isEmpty()) {
			return new Gson().fromJson(json, IdentifierSuggestionTestCase.class);
		}
		return null;
	}
	
	public static IdentifierSuggestionTestCase fromJson(Path file) {
		if (!Files.isDirectory(file) && file.toString().endsWith(".json")) {
			String content;
			try {
				content = Files.readString(file);
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
			return IdentifierSuggestionTestCase.fromJson(content);
		}
		return null;
	}
	
	public static IdentifierSuggestionTestCase[] fromDirectory(Path dir) {
		List<IdentifierSuggestionTestCase> tests = new ArrayList<>();
		for (final File file : dir.toFile().listFiles()) {
			IdentifierSuggestionTestCase test = fromJson(file.toPath());
			if (test != null) {
				tests.add(test);
			}
		}
		return tests.toArray(new IdentifierSuggestionTestCase[0]);
	}
}
