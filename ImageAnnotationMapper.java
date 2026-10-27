import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Setup and annotation processing: reads the file and removes stop words. */
public class ImageAnnotationMapper {

    /** One subject's annotation for one image, after stop listing. */
    public record Annotation(String id, String imageId, String subjectId,
                             String originalText, List<String> remainingWords) {}

    public static void main(String[] args) throws IOException {
        // Defaults let this class run on its own, without Main
        String annotationsFile = args.length >= 1 ? args[0] : "annotations.txt";
        String stopWordFile    = args.length >= 2 ? args[1] : "";
        int topK               = args.length >= 3 ? Integer.parseInt(args[2]) : 5;

        // Setup
        StopList stopList = createStopList(stopWordFile);

        // Processing
        List<Annotation> annotations = readAnnotations(Path.of(annotationsFile), stopList);

        System.out.println("Annotations file : " + annotationsFile);
        System.out.println("Top K            : " + topK);
        System.out.println("Stop words loaded: " + stopList.size());
        System.out.println("Annotations read : " + annotations.size());
        printResults(annotations);
    }

    /** Creates the StopList with the built-in words, then adds any words from the custom file. */
    static StopList createStopList(String stopWordFile) throws IOException {
        StopList stopList = new StopList();          // built-in words
        if (!stopWordFile.isEmpty()) {
            stopList.load(Path.of(stopWordFile));    // custom words
        }
        return stopList;
    }

    /** Reads each annotation line, then removes stop words from its text. */
    static List<Annotation> readAnnotations(Path file, StopList stopList) throws IOException {
        List<Annotation> result = new ArrayList<>();
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);

        for (int i = 0; i < lines.size(); i++) {
            int lineNo = i + 1;
            String line = lines.get(i).strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            // Format: imageId|subjectId|annotation|alternativeAnnotation
            
            String[] parts = line.split("\\|", 4);
            String imageId;
            String subjectId;
            String text;
            if (parts.length == 4) {
                imageId   = parts[0].strip();
                subjectId = parts[1].strip();
                text      = parts[2].strip();      // parts[3] is the alternative annotation
            } else if (parts.length == 3) {
                imageId   = parts[0].strip();
                subjectId = parts[1].strip();
                text      = parts[2].strip();
            } else if (parts.length == 2) {
                imageId   = parts[0].strip();
                subjectId = "S" + lineNo;            // subject assigned automatically
                text      = parts[1].strip();
            } else {
                System.err.println("Skipping malformed line " + lineNo + ": " + line);
                continue;
            }

            List<String> words = removeStopWords(text, stopList);
            result.add(new Annotation("A" + (result.size() + 1), imageId, subjectId, text, words));
        }
        return result;
    }

    /** Lowercases the text, splits it into letter-only tokens, and keeps the words that are not stop words. */
    static List<String> removeStopWords(String text, StopList stopList) {
        List<String> kept = new ArrayList<>();
        for (String token : text.toLowerCase(Locale.ROOT).split("[^a-z]+")) {
            if (token.length() < 2) continue;        // drops empty strings and single letters
            if (stopList.contains(token)) continue;  // drops stop words
            kept.add(token);
        }
        return kept;
    }

    /** Prints each image with its annotations, the original text and the words that remain. */
    static void printResults(List<Annotation> annotations) {
        System.out.println();
        String currentImage = null;
        for (Annotation a : annotations) {
            if (!a.imageId().equals(currentImage)) {
                currentImage = a.imageId();
                System.out.println("[" + currentImage + "]");
            }
            System.out.printf("  %s (%s)%n", a.id(), a.subjectId());
            System.out.println("    original : " + a.originalText());
            System.out.println("    kept     : " + String.join(", ", a.remainingWords()));
        }
    }
}
