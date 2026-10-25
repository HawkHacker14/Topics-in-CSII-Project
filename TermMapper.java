import java.util.*;

/**
 * Maps terms to each other through the graph's connections:
 * keyword (L1) -> image (L2) -> keyword (L1).
 * The score is the cosine similarity of the terms' weighted image-connection vectors.
 */
public class TermMapper {

    public record Mapping(String term, double score, Set<String> viaImages) {}

    private final TriPartiteGraph graph;
    private final Map<String, Double> norms = new HashMap<>();

    public TermMapper(TriPartiteGraph graph) {
        this.graph = graph;
        for (String term : graph.keywords()) {
            double sq = 0;
            for (double w : graph.imagesOf(term).values()) sq += w * w;
            norms.put(term, Math.sqrt(sq));
        }
    }

    public List<Mapping> related(String term, int topK, double minScore) {
        Map<String, Double> imgs = graph.imagesOf(term);
        if (imgs.isEmpty()) return List.of();

        Map<String, Double> dot = new HashMap<>();
        Map<String, Set<String>> via = new HashMap<>();

        // Two-hop traversal through the shared image vertices
        for (var e : imgs.entrySet()) {
            String image = e.getKey();
            for (var other : graph.keywordsOf(image).entrySet()) {
                String o = other.getKey();
                if (o.equals(term) || sharesWord(term, o)) continue; // skip "dog" <-> "brown dog"
                dot.merge(o, e.getValue() * other.getValue(), Double::sum);
                via.computeIfAbsent(o, x -> new TreeSet<>()).add(image);
            }
        }

        List<Mapping> out = new ArrayList<>();
        for (var e : dot.entrySet()) {
            double cos = e.getValue() / (norms.get(term) * norms.get(e.getKey()));
            if (cos >= minScore) out.add(new Mapping(e.getKey(), cos, via.get(e.getKey())));
        }
        out.sort(Comparator.comparingDouble(Mapping::score).reversed()
                           .thenComparing(Mapping::term));
        return out.size() > topK ? out.subList(0, topK) : out;
    }

    private static boolean sharesWord(String a, String b) {
        Set<String> sa = new HashSet<>(Arrays.asList(a.split(" ")));
        for (String t : b.split(" ")) if (sa.contains(t)) return true;
        return false;
    }
}

