package dev.aleiis.hintforge.eval;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import dev.aleiis.hintforge.model.ContextAwareCompletionConfig;
import dev.aleiis.hintforge.model.DslProfile;
import dev.aleiis.hintforge.model.ExternalFile;
import dev.aleiis.hintforge.model.IdentifierSuggestionConfig;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

public abstract class Test {
	
	protected static String API_KEY = ""; // Set your OpenAI API key here
	protected static DslProfile profile;
	protected static InMemoryEmbeddingStore<TextSegment> embeddingStore;
	
	protected static void init() {
		loadDslProfile();
		loadEmbeddingStore();
	}
	
	protected static void loadDslProfile() {
		ExternalFile xtextFile = new ExternalFile(Path.of("resources/graphql/GraphQL.xtext").toString(), "GraphQL.xtext");
		profile = new DslProfile("GraphQL", "graphql", xtextFile);
		profile.setDescription("The GraphQL type system describes what data can be queried from the API.");
		List<ExternalFile> scriptExamples = profile.getScriptExamples();
		Path examplesFolder = Path.of("resources/graphql/examples");
		for (final File file : examplesFolder.toFile().listFiles()) {
			if (file.isFile()) {
				scriptExamples.add(new ExternalFile(file.toString(), file.getName()));
			}
		}
		
		ContextAwareCompletionConfig configContextAwareCompletion = profile.getCodeCompletionConfig();
		configContextAwareCompletion.setFewShotPrompt("""
				CODE:
				type Token {
					id: String!
					[[CURSOR]]
					followed: [Token]!
				}
				INSTRUCTION: Add an attribute for the content of the token.
				OUTPUT: content: String
				CODE:
				[[CURSOR]]
					start: Date!
					end: Date!
					duration: int
				}
				INSTRUCTION: Complete the line of the type definition.
				OUTPUT: type TimeRange {
				CODE:
				{{code}}
				INSTRUCTION: {{instruction}}
				OUTPUT:
				""");
		configContextAwareCompletion.setMaxFixAttempts(2);
		configContextAwareCompletion.setMaxGenerationAttempts(1);
		
		IdentifierSuggestionConfig configIdentifierSuggestion = profile.getIdentifierSuggestionConfig();
		configIdentifierSuggestion.setFewShotPrompt("""
				CONTEXT:
				type [[CURSOR]] {
					id: Int!
					shape_id: String!
					geometry: LineString!
					generated: Boolean!
				}
				IDENTIFIERS:
				ShapePoint
				RouteSegment
				PathStep
				LinePath
				RoutePoint
				CONTEXT:
				type Frequency {
					id: Int!
					start_time: [[CURSOR]]
					end_time: Seconds!
					headway_secs: Int!
					exact_times: Int!
				}
				IDENTIFIERS:
				Seconds!
				Seconds
				Int!
				Int
				Timestamp!
				CONTEXT:
				{{code}}
				IDENTIFIERS:
				""");
		configIdentifierSuggestion.setMaxGenerationAttempts(2);
		
		System.out.println("[INFO] Loaded DSL profile: " + DslProfile.toJson(new DslProfile[] { profile }));
	}
	
	protected static void loadEmbeddingStore() {	
		Path embeddingStorePath = Path.of("resources/graphql/doc/embeddings.json");
		if (Files.exists(embeddingStorePath)) {
			embeddingStore = InMemoryEmbeddingStore.fromFile(embeddingStorePath);
			System.out.println("[INFO] Loaded EmbeddingStore");
		} else {
			System.out.println("[ERROR] EmbeddingStore JSON does not exist.");
		}
	}

}
