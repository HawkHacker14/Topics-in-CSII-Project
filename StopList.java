import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Dictionary of stop words that carry no contextual meaning. */
public class StopList {

    private static final String[] DEFAULT_WORDS = {
        "a","about","above","after","again","against","all","am","an","and","any","are","as","at",
        "be","because","been","before","being","below","between","both","but","by","can","could",
        "did","do","does","doing","down","during","each","few","for","from","further","had","has",
        "have","having","he","her","here","hers","herself","him","himself","his","how","i","if","in",
        "into","is","it","its","itself","just","me","more","most","my","myself","no","nor","not","now",
        "of","off","on","once","only","or","other","our","ours","ourselves","out","over","own","same",
        "she","should","so","some","such","than","that","the","their","theirs","them","themselves",
        "then","there","these","they","this","those","through","to","too","under","until","up","very",
        "was","we","were","what","when","where","which","while","who","whom","why","will","with",
        "would","you","your","yours","yourself","yourselves","also","next","near","across","onto",
        // image and picture related words
        
        "image","picture","photo","photograph","show","shows","showing","shown","depict","depicts",
        "depicting","appear","appears","seems","see","seen","visible"
    };

    private final Set<String> words = new HashSet<>(Arrays.asList(DEFAULT_WORDS));

    /** Adds words from a file (one per line, '#' for comments). */
    public void load(Path file) throws IOException {
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String w = line.strip().toLowerCase(Locale.ROOT);
            if (!w.isEmpty() && !w.startsWith("#")) words.add(w);
        }
    }

    public boolean contains(String word) {
        return words.contains(word);
    }
    public int size() {
        return words.size();
    }
}
