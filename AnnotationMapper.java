import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class AnnotationMapper {

    private static final int MIN_PHRASE_SUPPORT = 2;   // a phrase must appear in at least 2 annotations
    private static final double MIN_SCORE = 0.10;

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage: java ImageAnnotationMapper <annotations.txt> [stopwords.txt] [topK]");
            System.exit(1);
        }
        StopList stopList = new StopList();
        if (args.length >= 2) stopList.load(Path.of(args[1]));
        int topK = args.length >= 3 ? Integer.parseInt(args[2]) : 5;

        TextProcessor processor = new TextProcessor(stopList);
        List<TriPartiteGraph.Annotation> annotations = readAnnotations(Path.of(args[0]), processor);
        if (annotations.isEmpty()) {
            System.err.println("No annotations found.");
            return;
        }

        TriPartiteGraph graph = new TriPartiteGraph(annotations, MIN_PHRASE_SUPPORT);
        TermMapper mapper = new TermMapper(graph);

        printGraph(graph, processor);
        printMappings(graph, mapper, processor, topK);
        interactive(mapper, processor, topK);
    }

    static List<TriPartiteGraph.Annotation> readAnnotations(Path file, TextProcessor p) throws IOException {
        List<TriPartiteGraph.Annotation> list = new ArrayList<>();
        int lineNo = 0;
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            lineNo++;
            line = line.strip();
            if (line.isEmpty() || line.startsWith("#")) continue;

            String[] parts = line.split("\\|", 3);
            String image, subject, text;
            if (parts.length == 3)      { image = parts[0].strip(); subject = parts[1].strip(); text = parts[2]; }
            else if (parts.length == 2) { image = parts[0].strip(); subject = "S" + lineNo;    text = parts[1]; }
            else { System.err.println("Skipping malformed line " + lineNo + ": " + line); continue; }

            Set<String> terms = new LinkedHashSet<>(p.extractTerms(text, true));
            list.add(new TriPartiteGraph.Annotation("A" + (list.size() + 1), image, subject, text.strip(), terms));
        }
        return list;
    }

    static void printGraph(TriPartiteGraph g, TextProcessor p) {
        System.out.println("=== LEVEL 1: KEYWORDS / PHRASES (" + g.keywords().size() + ") ===");
        for (String k : g.keywords())
            System.out.printf("  %-22s [%s] -> %s%n", k, p.display(k), g.imagesOf(k).keySet());

        System.out.println("\n=== LEVEL 2: IMAGES  <->  LEVEL 3: ANNOTATIONS ===");
        for (String img : g.images()) {
            StringJoiner sj = new StringJoiner(", ");
            g.keywordsOf(img).forEach((k, w) -> sj.add(String.format("%s(%.2f)", k, w)));
            System.out.println("  [" + img + "]");
            System.out.println("    keywords: " + sj);
            for (var a : g.annotationsOf(img))
                System.out.printf("    %s (%s): \"%s\"%n        filtered+stemmed: %s%n",
                        a.id(), a.subjectId(), a.text(), a.terms());
        }
    }

    static void printMappings(TriPartiteGraph g, TermMapper m, TextProcessor p, int topK) {
        System.out.println("\n=== TERM MAPPINGS (keyword -> image -> keyword) ===");
        for (String k : g.keywords()) {
            if (TriPartiteGraph.isPhrase(k)) continue;           // keep the report readable
            List<TermMapper.Mapping> rel = m.related(k, topK, MIN_SCORE);
            if (rel.isEmpty()) continue;
            StringJoiner sj = new StringJoiner(", ");
            for (var r : rel) sj.add(String.format("%s %.2f %s", p.display(r.term()), r.score(), r.viaImages()));
            System.out.printf("  %-14s => %s%n", p.display(k), sj);
        }
    }

    static void interactive(TermMapper m, TextProcessor p, int topK) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("\nEnter a word or phrase to map (blank to quit): ");
            String line = in.readLine();
            if (line == null || line.isBlank()) break;

            List<String> stems = p.extractTerms(line, false);
            if (stems.isEmpty()) { System.out.println("  Only stop words were entered."); continue; }

            String query = String.join(" ", stems);
            List<TermMapper.Mapping> rel = m.related(query, topK, 0.0);
            if (rel.isEmpty()) { System.out.println("  '" + query + "' is not connected in the graph."); continue; }
            for (var r : rel)
                System.out.printf("  %-18s score=%.3f via %s%n", p.display(r.term()), r.score(), r.viaImages());
        }
    }
}

