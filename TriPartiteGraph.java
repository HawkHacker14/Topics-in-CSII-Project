import java.util.*;

/**
 * Level 1: keywords/phrases (weighted) 
 * Level 2: Images  
 * Level 3: Annotations
 */
public class TriPartiteGraph {

    public record Annotation(String id, String imageId, String subjectId,
                             String text, Set<String> terms) {}

    private final Map<String, Map<String, Double>> keywordToImages = new TreeMap<>();
    private final Map<String, Map<String, Double>> imageToKeywords = new TreeMap<>();
    private final Map<String, List<Annotation>> imageToAnnotations = new TreeMap<>();

    public TriPartiteGraph(List<Annotation> annotations, int minPhraseSupport) {
        // Level 2 <-> Level 3 edges
        Map<String, Integer> globalFreq = new HashMap<>();
        for (Annotation a : annotations) {
            imageToAnnotations.computeIfAbsent(a.imageId(), x -> new ArrayList<>()).add(a);
            for (String t : a.terms()) globalFreq.merge(t, 1, Integer::sum);
        }

        // Level 1 <-> Level 2 edges, weighted by agreement among the Level 3 annotations
        for (var entry : imageToAnnotations.entrySet()) {
            String image = entry.getKey();
            List<Annotation> anns = entry.getValue();

            Map<String, Integer> counts = new HashMap<>();
            for (Annotation a : anns)
                for (String t : a.terms()) counts.merge(t, 1, Integer::sum);

            for (var c : counts.entrySet()) {
                String term = c.getKey();
                if (isPhrase(term) && globalFreq.get(term) < minPhraseSupport) continue;
                double support = c.getValue() / (double) anns.size();
                keywordToImages.computeIfAbsent(term, x -> new TreeMap<>()).put(image, support);
                imageToKeywords.computeIfAbsent(image, x -> new TreeMap<>()).put(term, support);
            }
        }
    }

    public static boolean isPhrase(String term)              { return term.contains(" "); }
    public Set<String> keywords()                            { return keywordToImages.keySet(); }
    public Set<String> images()                              { return imageToAnnotations.keySet(); }
    public Map<String, Double> imagesOf(String keyword)      { return keywordToImages.getOrDefault(keyword, Map.of()); }
    public Map<String, Double> keywordsOf(String image)      { return imageToKeywords.getOrDefault(image, Map.of()); }
    public List<Annotation> annotationsOf(String image)      { return imageToAnnotations.getOrDefault(image, List.of()); }
}

