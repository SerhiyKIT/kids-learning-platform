package ua.kidlearn.scenario;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The stop list of the scenario validator (scenario/stop-words.txt): words and phrases that never
 * belong in a lesson for 3–6-year-olds. Words are matched whole, or by prefix when the entry ends
 * with «*» (Ukrainian inflects: «кров*» covers «крові», «кров'ю»); a phrase is matched as text.
 */
final class StopWords {

	private static final String CLASSPATH_LOCATION = "scenario/stop-words.txt";
	/** A word: letters, with the apostrophe that Ukrainian keeps inside words. */
	private static final Pattern WORD = Pattern.compile("[\\p{L}'’ʼ]+");

	private final Set<String> words = new LinkedHashSet<>();
	private final List<String> prefixes = new ArrayList<>();
	private final List<String> phrases = new ArrayList<>();

	static StopWords load() {
		try (InputStream in = StopWords.class.getClassLoader().getResourceAsStream(CLASSPATH_LOCATION)) {
			if (in == null) {
				throw new IllegalStateException(CLASSPATH_LOCATION + " missing from classpath");
			}
			StopWords stopWords = new StopWords();
			for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
				String entry = line.strip().toLowerCase(Locale.ROOT);
				if (entry.isEmpty() || entry.startsWith("#")) {
					continue;
				}
				if (entry.contains(" ")) {
					stopWords.phrases.add(entry);
				} else if (entry.endsWith("*")) {
					stopWords.prefixes.add(entry.substring(0, entry.length() - 1));
				} else {
					stopWords.words.add(entry);
				}
			}
			return stopWords;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/** The stop-list entries found in the text, in order of appearance; empty when it's clean. */
	List<String> find(String text) {
		String lower = text.toLowerCase(Locale.ROOT);
		Set<String> hits = new LinkedHashSet<>();
		Matcher matcher = WORD.matcher(lower);
		while (matcher.find()) {
			String word = matcher.group();
			if (words.contains(word) || prefixes.stream().anyMatch(word::startsWith)) {
				hits.add(word);
			}
		}
		String spaced = lower.replaceAll("\\s+", " ");
		for (String phrase : phrases) {
			if (spaced.contains(phrase)) {
				hits.add(phrase);
			}
		}
		return List.copyOf(hits);
	}

}
