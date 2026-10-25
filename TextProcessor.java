import java.util.*;

/** Tokenizes text, removes stop words, stems the rest and optionally builds 2-word phrases. */
public class TextProcessor {

    private final StopList stopList;
    private final PorterStemmer stemmer = new PorterStemmer();
    /** stem -> (original word -> count), so readable forms can be shown in the output */
    private final Map<String, Map<String, Integer>> surfaceForms = new HashMap<>();

    public TextProcessor(StopList stopList) {
        this.stopList = stopList;
    }

    /**
     * Returns the stemmed keywords. If includePhrases is true, it also returns bigrams of
     * adjacent surviving words. A stop word or punctuation mark breaks a phrase.
     */
    public List<String> extractTerms(String text, boolean includePhrases) {
        List<String> terms = new ArrayList<>();
        for (String chunk : text.toLowerCase(Locale.ROOT).split("[.,;:!?()\\[\\]\"]+")) {
            String prev = null;
            for (String tok : chunk.split("[^a-z]+")) {
                if (tok.isEmpty()) continue;
                if (tok.length() < 2 || stopList.contains(tok)) { prev = null; continue; }
                String stem = stemmer.stem(tok);
                if (stopList.contains(stem)) { prev = null; continue; }

                surfaceForms.computeIfAbsent(stem, s -> new HashMap<>()).merge(tok, 1, Integer::sum);
                terms.add(stem);
                if (includePhrases && prev != null) terms.add(prev + " " + stem);
                prev = stem;
            }
        }
        return terms;
    }

    /** Maps a stemmed term back to its most frequent original form, e.g. "puppi" -> "puppy". */
    public String display(String term) {
        StringJoiner sj = new StringJoiner(" ");
        for (String s : term.split(" ")) {
            Map<String, Integer> forms = surfaceForms.get(s);
            sj.add(forms == null ? s
                    : Collections.max(forms.entrySet(), Map.Entry.comparingByValue()).getKey());
        }
        return sj.toString();
    }
}

