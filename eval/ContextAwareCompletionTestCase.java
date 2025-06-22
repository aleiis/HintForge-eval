package dev.aleiis.hintforge.eval;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

public class ContextAwareCompletionTestCase {

	public String context;
	public String expected;
	public int offset;
	public String instruction;
	
	public static ContextAwareCompletionTestCase fromJson(String json) {
		if (json != null && !json.isEmpty()) {
			return new Gson().fromJson(json, ContextAwareCompletionTestCase.class);
		}
		return null;
	}
	
	public static ContextAwareCompletionTestCase fromJson(Path file) {
		if (!Files.isDirectory(file) && file.toString().endsWith(".json")) {
			String content;
			try {
				content = Files.readString(file);
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
			return ContextAwareCompletionTestCase.fromJson(content);
		}
		return null;
	}
	
	public static ContextAwareCompletionTestCase[] fromDirectory(Path dir) {
		List<ContextAwareCompletionTestCase> tests = new ArrayList<>();
		for (final File file : dir.toFile().listFiles()) {
			ContextAwareCompletionTestCase test = fromJson(file.toPath());
			if (test != null) {
				tests.add(test);
			}
		}
		return tests.toArray(new ContextAwareCompletionTestCase[0]);
	}
}
