package dev.aleiis.hintforge.eval;

import java.util.Arrays;
import java.util.List;

public class SimpleCodeTokenizer implements ITokenizer {

	@Override
	public List<String> tokenize(String code) {
		return Arrays.asList(code
	            .replaceAll("([{}();=,+\\-*/<>]!)", " $1 ")  // space common symbols
	            .replaceAll("\\s+", " ")                    // normalize spaces
	            .trim()
	            .split(" "));
	}
}
