package ai.gradpath.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import ai.gradpath.reference.Career;
import ai.gradpath.reference.CareerCatalog;

/**
 * Transparent, keyword-based resume analysis (no AI model and no external service).
 *
 * <p>Uploaded files are only parsed for text with Apache PDFBox; they are never executed, rendered or stored on disk
 * by this service. Size, file signature and page count are checked first.
 */
@Service
public class ResumeService {

    public static final int MAX_BYTES = 5 * 1024 * 1024;
    public static final int MAX_PAGES = 15;
    public static final int MIN_WORDS = 40;

    static final Set<String> ACTION_VERBS = Set.of("built", "developed", "designed", "implemented", "led", "created",
            "analysed", "analyzed", "improved", "automated", "deployed", "optimised", "optimized", "managed",
            "launched", "tested", "delivered", "reduced", "increased");
    static final Map<String, String> SECTIONS = new LinkedHashMap<>();

    static {
        SECTIONS.put("education", "Education");
        SECTIONS.put("experience", "Experience");
        SECTIONS.put("project", "Projects");
        SECTIONS.put("skill", "Skills");
    }

    private static final Pattern EMAIL = Pattern.compile("(?U)[\\w.+-]+@[\\w-]+\\.[\\w.]+");
    private static final Pattern MEASURABLE = Pattern.compile("(?U)\\d+\\s?%|\\b\\d{2,}\\b|\\d+\\+");

    /** One structure check. */
    public record Check(String label, boolean ok) { }

    /** Result of comparing a resume with a career's requirements. */
    public record Analysis(String career, int score, int coverage, List<String> found, List<String> matched,
                           List<String> missing, List<Check> checks, List<String> tips, int words,
                           List<String> projectIdeas) {
        public long checksPassed() {
            return checks.stream().filter(Check::ok).count();
        }
    }

    /**
     * Read the text of an uploaded PDF. Returns a failure with a user-facing message when the file is not a readable
     * PDF (wrong type, too large, encrypted, damaged or image-only).
     */
    public Result<String> extractText(byte[] pdf) {
        if (pdf == null || pdf.length == 0) {
            return Result.fail("Please upload your resume as a PDF file.");
        }
        if (pdf.length > MAX_BYTES) {
            return Result.fail("The PDF is larger than 5 MB. Please upload a smaller file.");
        }
        if (!isPdf(pdf)) {
            return Result.fail("This file is not a PDF. Please upload a .pdf resume.");
        }
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            if (doc.getNumberOfPages() > MAX_PAGES) {
                return Result.fail("The PDF has more than " + MAX_PAGES + " pages. Please upload your resume only.");
            }
            String text = new PDFTextStripper().getText(doc).replace('\u0000', ' ').strip();
            if (wordCount(text) < MIN_WORDS) {
                return Result.fail("We could not read enough text from this PDF (at least " + MIN_WORDS
                        + " words are needed). Scanned images cannot be read - please export your resume as a text PDF.");
            }
            return Result.ok(text);
        } catch (InvalidPasswordException e) {
            return Result.fail("This PDF is password-protected. Please upload an unprotected copy.");
        } catch (IOException | RuntimeException e) {
            return Result.fail("This PDF could not be read. Please re-export it and try again.");
        }
    }

    /** True when the bytes start with the PDF signature. */
    public static boolean isPdf(byte[] bytes) {
        return bytes != null && bytes.length >= 5
                && new String(bytes, 0, 5, StandardCharsets.ISO_8859_1).equals("%PDF-");
    }

    public static int wordCount(String text) {
        String t = text == null ? "" : text.strip();
        return t.isEmpty() ? 0 : t.split("\\s+").length;
    }

    /** Find a keyword without matching it inside a larger token ("java" must not match "javascript"). */
    static boolean contains(String lowerText, String alias) {
        return Pattern.compile("(?<![a-z0-9+#.])" + Pattern.quote(alias) + "(?![a-z0-9+#])").matcher(lowerText).find();
    }

    /** Skills (canonical names) whose keywords appear in the text. */
    public boolean mentions(String text, String skill) {
        String low = text == null ? "" : text.toLowerCase();
        return CareerCatalog.aliases(skill).stream().anyMatch(a -> contains(low, a));
    }

    /** Percentage of the given skills mentioned in the text. */
    public int skillCoverage(String text, List<String> skills) {
        if (skills.isEmpty()) {
            return 0;
        }
        long hit = skills.stream().filter(s -> mentions(text, s)).count();
        return (int) Math.round(100.0 * hit / skills.size());
    }

    /** Compare resume keywords and structure with a career's requirements. */
    public Analysis analyze(String text, String careerName) {
        Career c = CareerCatalog.career(careerName).orElseThrow(() -> new NotFoundException("Unknown career."));
        String low = text.toLowerCase();
        List<String> found = new ArrayList<>();
        CareerCatalog.SKILL_ALIASES.forEach((skill, aliases) -> {
            if (aliases.stream().anyMatch(a -> contains(low, a))) {
                found.add(skill);
            }
        });
        List<String> required = c.skillNames();
        List<String> matched = required.stream().filter(s -> found.contains(s) || mentions(text, s)).toList();
        List<String> missing = required.stream().filter(s -> !matched.contains(s)).toList();
        double coverage = (double) matched.size() / required.size();
        int words = wordCount(text);

        List<Check> checks = new ArrayList<>();
        checks.add(new Check("Contact email present", EMAIL.matcher(text).find()));
        checks.add(new Check("GitHub, LinkedIn or portfolio link",
                low.contains("github") || low.contains("linkedin") || low.contains("portfolio")));
        checks.add(new Check("Measurable results (numbers or %)", MEASURABLE.matcher(text).find()));
        checks.add(new Check("Strong action verbs (3 or more)", ACTION_VERBS.stream().filter(v -> contains(low, v)).count() >= 3));
        checks.add(new Check("Length between 150 and 800 words", words >= 150 && words <= 800));
        SECTIONS.forEach((key, label) -> checks.add(new Check(label + " section", low.contains(key))));

        long passed = checks.stream().filter(Check::ok).count();
        int score = (int) Math.round(0.7 * coverage * 100 + 0.3 * ((double) passed / checks.size()) * 100);
        List<String> tips = new ArrayList<>();
        if (!missing.isEmpty()) {
            tips.add("Add evidence for: " + String.join(", ", missing.subList(0, Math.min(5, missing.size())))
                    + " - only if you genuinely have these skills.");
        }
        for (Check ch : checks) {
            if (!ch.ok()) {
                tips.add(tipFor(ch.label(), words));
            }
        }
        return new Analysis(c.name(), score, (int) Math.round(coverage * 100), List.copyOf(new TreeSet<>(found)),
                matched, missing, checks, tips, words, c.projects());
    }

    private static String tipFor(String label, int words) {
        if (label.startsWith("Contact")) {
            return "Add a professional email address at the top of your resume.";
        }
        if (label.startsWith("GitHub")) {
            return "Link your GitHub, LinkedIn or portfolio so reviewers can see your work.";
        }
        if (label.startsWith("Measurable")) {
            return "Quantify results, e.g. \"reduced load time by 30%\" or \"served 200 users\".";
        }
        if (label.startsWith("Strong")) {
            return "Start bullet points with action verbs such as built, designed, led or improved.";
        }
        if (label.startsWith("Length")) {
            return words < 150 ? "Your resume is short - describe projects and responsibilities in more detail."
                    : "Your resume is long - keep the most relevant 1-2 pages.";
        }
        return "Add a clear \"" + label.replace(" section", "") + "\" section heading.";
    }
}
