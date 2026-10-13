import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class ShortenComments {
    public static void main(String[] args) throws Exception {
        Path srcDir = Paths.get("src");
        long origTotal = 0;
        long newTotal = 0;
        int modifiedFiles = 0;

        try (var stream = Files.newDirectoryStream(srcDir, "*.java")) {
            List<Path> files = new ArrayList<>();
            stream.forEach(files::add);
            files.sort(Comparator.naturalOrder());

            for (Path file : files) {
                String text = Files.readString(file);
                StringBuilder out = new StringBuilder();
                int i = 0;
                while (i < text.length()) {
                    if (text.startsWith("/*", i)) {
                        int end = text.indexOf("*/", i + 2);
                        if (end == -1) {
                            out.append(text.substring(i));
                            break;
                        }
                        String body = text.substring(i + 2, end);
                        String shortBody = shortenComment(body);
                        if (shortBody.isEmpty()) {
                            out.append("/* */");
                        } else {
                            out.append("/* ").append(shortBody).append(" */");
                        }
                        origTotal += body.length();
                        newTotal += shortBody.length() + 6;
                        i = end + 2;
                    } else if (text.startsWith("//", i)) {
                        int end = text.indexOf('\n', i + 2);
                        if (end == -1) {
                            end = text.length();
                        }
                        String body = text.substring(i + 2, end);
                        String shortBody = shortenComment(body);
                        if (shortBody.isEmpty()) {
                            out.append("//");
                        } else {
                            out.append("// ").append(shortBody);
                        }
                        origTotal += body.length();
                        newTotal += shortBody.length() + 3;
                        i = end;
                    } else {
                        out.append(text.charAt(i));
                        i++;
                    }
                }

                String newText = out.toString();
                if (!newText.equals(text)) {
                    Files.writeString(file, newText);
                    modifiedFiles++;
                }
            }
        }

        double reduction = (origTotal == 0) ? 0.0 : ((origTotal - newTotal) * 100.0 / origTotal);
        System.out.println("Files updated: " + modifiedFiles);
        System.out.println("Original comment chars: " + origTotal);
        System.out.println("New comment chars: " + newTotal);
        System.out.printf("Reduction: %.1f%%%n", reduction);
    }

    private static String shortenComment(String raw) {
        String text = raw.trim();
        if (text.isEmpty()) {
            return "";
        }

        List<String> pieces = new ArrayList<>();
        for (String line : text.split("\\R")) {
            String s = line.trim();
            if (s.startsWith("/**")) {
                s = s.substring(3).trim();
            }
            if (s.startsWith("/*")) {
                s = s.substring(2).trim();
            }
            if (s.startsWith("*")) {
                s = s.substring(1).trim();
            }
            if (s.startsWith("//")) {
                s = s.substring(2).trim();
            }
            if (s.endsWith("*/")) {
                s = s.substring(0, s.length() - 2).trim();
            }
            if (!s.isEmpty()) {
                pieces.add(s);
            }
        }

        String body = String.join(" ", pieces).replaceAll("\\s+", " ").trim();
        body = body.replaceFirst("^(?i)(what is this file for\\?|how a java program starts|what is this file for|this file|plain english|secure coding ideas used here|what is this file for\\? \\(plain english\\))\\s*[:\\-]*", "").trim();
        body = body.replaceAll("^[\\-#*/\\s]+", "").trim();

        if (body.isEmpty()) {
            return "";
        }

        String[] words = body.split("\\s+");
        int keep = Math.max(1, Math.min(words.length, Math.min((int) Math.ceil(words.length * 0.3), 12)));
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < keep; i++) {
            if (i > 0) {
                result.append(' ');
            }
            result.append(words[i]);
        }

        String finished = result.toString().replaceAll("[\\s,;:]+$", "");
        if (finished.length() > 120) {
            finished = String.join(" ", Arrays.copyOfRange(words, 0, Math.min(words.length, 8)));
        }
        if (!finished.isEmpty() && !".!?".contains(String.valueOf(finished.charAt(finished.length() - 1)))) {
            finished += ".";
        }
        return finished;
    }
}
