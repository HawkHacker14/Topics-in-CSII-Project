import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Usage: java Main [annotations.txt] [stopwords.txt] [topK] */
public class Main {

    public static void main(String[] args) throws IOException {
        // 1. Read the arguments
        String annotationsFile = args.length >= 1 ? args[0] : "annotations.txt";
        String stopWordFile    = args.length >= 2 ? args[1] : "";
        String topK            = args.length >= 3 ? args[2] : "5";

        // 2. Check that the files exist
        if (!Files.isRegularFile(Path.of(annotationsFile))) {
            System.err.println("Annotations file not found: " + Path.of(annotationsFile).toAbsolutePath());
            System.exit(1);
        }
        if (!stopWordFile.isEmpty() && !Files.isRegularFile(Path.of(stopWordFile))) {
            System.err.println("Stop-word file not found: " + Path.of(stopWordFile).toAbsolutePath());
            System.exit(1);
        }
        try {
            Integer.parseInt(topK);
        } catch (NumberFormatException e) {
            System.err.println("topK must be a whole number: " + topK);
            System.exit(1);
        }

        // 3. Call ImageAnnotationMapper.main
        ImageAnnotationMapper.main(new String[] { annotationsFile, stopWordFile, topK });
    }
}
