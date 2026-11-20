package ai.gradpath.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ai.gradpath.config.DotenvEnvironmentPostProcessor;
import ai.gradpath.domain.Student;
import ai.gradpath.readiness.ReadinessCategory;
import ai.gradpath.security.LoginThrottle;
import ai.gradpath.security.PasswordService;
import ai.gradpath.service.assistant.AssistantContext;
import ai.gradpath.service.assistant.AssistantReply;
import ai.gradpath.service.assistant.ChatTurn;
import ai.gradpath.service.assistant.HttpTransport;
import ai.gradpath.service.assistant.LlmAssistant;
import ai.gradpath.service.assistant.PiiRedactor;
import ai.gradpath.service.assistant.RuleBasedAssistant;
import ai.gradpath.validation.Validators;

/** Fast unit tests (no Spring context): validation, passwords, skill matching, fit, resume analysis, assistant. */
class UnitServicesTest {

    static Student student(String skills, String learning) {
        Student s = new Student();
        s.setName("Test Student");
        s.setSkills(skills);
        s.setLearningSkills(learning);
        return s;
    }

    /** A small text PDF built with PDFBox. */
    static byte[] pdf(String... lines) throws IOException {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                cs.setLeading(12);
                cs.newLineAtOffset(40, 740);
                for (String l : lines) {
                    cs.showText(l);
                    cs.newLine();
                }
                cs.endText();
            }
            doc.save(out);
            return out.toByteArray();
        }
    }

    static final String[] RESUME = {
        "Sara Khan - sara@example.com - github.com/sara",
        "Education: BS Computer Science, Test University, CGPA 3.6",
        "Skills: Java, SQL, Git, OOP, REST APIs, JUnit testing, Agile, Data Structures, problem solving",
        "Experience: Software intern - developed and tested a Spring Boot REST API and improved latency by 30%",
        "Projects: built a library system with PostgreSQL; designed a task manager; led a team of 4 students",
        "Implemented automated unit tests, deployed the app and managed releases for 200 users",
    };

    @Nested
    class ValidationTests {
        @ParameterizedTest
        @CsvSource({"name@uni.edu,true", "a.b+c@x.co,true", "bad,false", "a@b,false", "'',false"})
        void email(String email, boolean valid) {
            assertThat(Validators.checkEmail(email) == null).isEqualTo(valid);
        }

        @Test
        void password() {
            assertThat(Validators.checkPassword("short1", null)).contains("at least 8");
            assertThat(Validators.checkPassword("onlyletters", null)).contains("letter and one number");
            assertThat(Validators.checkPassword("Passw0rd1", "Passw0rd2")).isEqualTo("Passwords do not match.");
            assertThat(Validators.checkPassword("Passw0rd1", "Passw0rd1")).isNull();
            assertThat(Validators.checkPassword("a1".repeat(40), null)).contains("at most 72");
        }

        @Test
        void phoneNameUrl() {
            assertThat(Validators.checkPhone("+92 300-111 2233", true)).isNull();
            assertThat(Validators.checkPhone("123", true)).isNotNull();
            assertThat(Validators.checkPhone("", false)).isNull();
            assertThat(Validators.checkName("Mary-Jane O'Neil")).isNull();
            assertThat(Validators.checkName("<script>")).isNotNull();
            assertThat(Validators.checkUrl("https://github.com/me")).isNull();
            assertThat(Validators.checkUrl("javascript:alert(1)")).isNotNull();
            assertThat(Validators.checkUrl("ftp://x.com")).isNotNull();
        }
    }

    @Nested
    class Passwords {
        private final PasswordService passwords = new PasswordService();

        @Test
        void bcryptIsSaltedAndVerifies() {
            String a = passwords.hash("Passw0rd1");
            String b = passwords.hash("Passw0rd1");
            assertThat(a).startsWith("$2").isNotEqualTo(b);
            assertThat(passwords.matches("Passw0rd1", a)).isTrue();
            assertThat(passwords.matches("wrong", a)).isFalse();
            assertThat(passwords.needsUpgrade(a)).isFalse();
        }

        @Test
        void legacyPbkdf2HashesStillVerifyAndNeedUpgrade() throws Exception {
            byte[] salt = new byte[16];
            javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec("Demo@1234".toCharArray(), salt, 200_000, 256);
            byte[] digest = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            String legacy = java.util.HexFormat.of().formatHex(salt) + "$" + java.util.HexFormat.of().formatHex(digest);
            assertThat(passwords.matches("Demo@1234", legacy)).isTrue();
            assertThat(passwords.matches("demo@1234", legacy)).isFalse();
            assertThat(passwords.needsUpgrade(legacy)).isTrue();
        }

        @Test
        void throttleLocksAfterFiveFailures() {
            long[] now = {1_000_000};
            LoginThrottle t = new LoginThrottle(() -> now[0]);
            for (int i = 0; i < 4; i++) {
                t.recordFailure("a@x.com", "1.1.1.1");
            }
            assertThat(t.secondsLocked("a@x.com", "1.1.1.1")).isZero();
            t.recordFailure("a@x.com", "1.1.1.1");
            assertThat(t.secondsLocked("a@x.com", "1.1.1.1")).isPositive();
            now[0] += LoginThrottle.EMAIL_LOCK_MILLIS + 1;
            assertThat(t.secondsLocked("a@x.com", "1.1.1.1")).isZero();
        }
    }

    @Nested
    class SkillsAndFit {
        private final SkillGapService gap = new SkillGapService();

        @Test
        void jobMatchCreditsLearningSkillsByHalf() {
            Student s = student("Java,SQL", "Git");
            SkillGapService.JobMatch m = gap.matchJob(s, List.of("Java", "SQL", "Git", "OOP"));
            assertThat(m.percent()).isEqualTo(63);      // (2 + 0.5) / 4 = 62.5%
            assertThat(m.matched()).containsExactly("Java", "SQL");
            assertThat(m.learning()).containsExactly("Git");
            assertThat(m.missing()).containsExactly("OOP");
            assertThat(gap.matchJob(s, List.of()).percent()).isZero();
        }

        @Test
        void careerAnalysisWeightsImportance() {
            SkillGapService.Analysis a = gap.analyze(student("Java,OOP,Data Structures", "Git"), "Software Developer");
            // Software Developer importance total = 3+3+3+2+2+2+2+2+1+1 = 21; earned = 9 + 1 = 10 -> 48%
            assertThat(a.match()).isEqualTo(48);
            assertThat(a.counts()).containsEntry("Matched", 3).containsEntry("Learning", 1).containsEntry("Missing", 6);
            assertThat(a.priorities()).hasSize(3);
            assertThat(a.priorities().get(0).importance()).isEqualTo(2);
        }

        @ParameterizedTest(name = "skill {0}, resume {1}, {2} -> {3} ({4})")
        @CsvSource({"100,100,JOB_READY,100,Strong fit", "100,0,DEVELOPING,67,Good fit", "50,40,NEEDS_IMPROVEMENT,43,Partial fit",
                    "0,0,NEEDS_IMPROVEMENT,5,Low fit", "60,60,DEVELOPING,60,Good fit", "75,75,JOB_READY,80,Strong fit"})
        void fitFormulaAndLabels(int skill, int resume, ReadinessCategory cat, int fit, String label) {
            int f = FitCalculator.fit(skill, resume, cat);
            assertThat(f).isEqualTo(fit);
            assertThat(FitCalculator.FitLabel.of(f).label()).isEqualTo(label);
        }

        @ParameterizedTest
        @CsvSource({"75,Strong fit", "74,Good fit", "55,Good fit", "54,Partial fit", "35,Partial fit", "34,Low fit"})
        void fitLabelBoundaries(int fit, String label) {
            assertThat(FitCalculator.FitLabel.of(fit).label()).isEqualTo(label);
        }
    }

    @Nested
    class Resume {
        private final ResumeService resumes = new ResumeService();

        @Test
        void extractsTextFromRealPdfAndAnalyses() throws IOException {
            Result<String> text = resumes.extractText(pdf(RESUME));
            assertThat(text.isOk()).as(text.error()).isTrue();
            ResumeService.Analysis a = resumes.analyze(text.value(), "Software Developer");
            assertThat(a.matched()).contains("Java", "SQL", "Git", "OOP", "REST APIs", "Testing", "Agile");
            assertThat(a.missing()).doesNotContain("Java");
            assertThat(a.checks()).filteredOn(ResumeService.Check::ok).extracting(ResumeService.Check::label)
                    .contains("Contact email present", "GitHub, LinkedIn or portfolio link", "Measurable results (numbers or %)");
            assertThat(a.score()).isBetween(0, 100);
            assertThat(a.projectIdeas()).isNotEmpty();
        }

        @Test
        void rejectsNonPdfEmptyAndUnreadable() throws IOException {
            assertThat(resumes.extractText("hello".getBytes()).error()).contains("not a PDF");
            assertThat(resumes.extractText(new byte[0]).error()).contains("upload");
            assertThat(resumes.extractText("%PDF-1.4 garbage".getBytes()).error()).containsAnyOf("could not be read", "at least 40 words");
            assertThat(resumes.extractText(pdf("Too short")).error()).contains("at least 40 words");
        }

        @Test
        void keywordsDoNotMatchInsideLargerWords() {
            assertThat(resumes.mentions("I know JavaScript", "Java")).isFalse();
            assertThat(resumes.mentions("I know Java and JavaScript", "Java")).isTrue();
            assertThat(resumes.skillCoverage("java sql", List.of("Java", "SQL", "Git", "OOP"))).isEqualTo(50);
        }
    }

    @Nested
    class Assistant {
        private final AssistantContext ctx = new AssistantContext("Software Developer", 58, "Developing", false,
                List.of("Projects"), List.of("Testing"), 60, 1, 25);

        @Test
        void ruleBasedIsDeterministicAndPersonalised() {
            RuleBasedAssistant bot = new RuleBasedAssistant();
            AssistantReply r1 = bot.reply(List.of(new ChatTurn("user", "How do I improve my readiness?")), ctx, 1);
            AssistantReply r2 = bot.reply(List.of(new ChatTurn("user", "How do I improve my readiness?")), ctx, 1);
            assertThat(r1).isEqualTo(r2);
            assertThat(r1.text()).contains("58/100").contains("Projects");
            assertThat(bot.reply(List.of(new ChatTurn("user", "Which skills should I learn?")), ctx, 1).text()).contains("Testing");
            assertThat(bot.reply(List.of(new ChatTurn("user", "interview tips")), ctx, 1).text()).contains("interviewer");
            assertThat(bot.reply(List.of(new ChatTurn("user", "zzz")), ctx, 1).text()).contains("I can help");
        }

        @Test
        void redactorRemovesContactDetailsAndName() {
            String out = PiiRedactor.redact("I am Sara Khan, email sara@uni.edu, call +92 300 1112233, see https://x.io/me",
                    List.of("Sara", "Khan"));
            assertThat(out).doesNotContain("Sara").doesNotContain("Khan").doesNotContain("sara@uni.edu")
                    .doesNotContain("1112233").doesNotContain("x.io");
        }

        @Test
        void llmFailureFallsBackClearlyAndSendsNoPii() {
            StringBuilder sent = new StringBuilder();
            HttpTransport failing = (url, headers, body) -> {
                sent.append(body);
                return new HttpTransport.Response(429, "{}");
            };
            LlmAssistant llm = new LlmAssistant("groq", "key", "model", failing, new ConcurrentHashMap<>());
            String question = PiiRedactor.redact("I'm Sara, how do I improve my score?", List.of("Sara"));
            AssistantReply r = llm.reply(List.of(new ChatTurn("user", question)), ctx, 7);
            assertThat(r.source()).isEqualTo(RuleBasedAssistant.LABEL);
            assertThat(r.notice()).contains("unavailable").contains("rate limit");
            assertThat(sent.toString()).doesNotContain("Sara").contains("Anonymous student context");
        }

        @Test
        void llmSuccessIsUsed() {
            HttpTransport ok = (url, headers, body) -> new HttpTransport.Response(200,
                    "{\"choices\":[{\"message\":{\"content\":\"Build two projects.\"}}]}");
            LlmAssistant llm = new LlmAssistant("groq", "key", "model", ok, new ConcurrentHashMap<>());
            AssistantReply r = llm.reply(List.of(new ChatTurn("user", "help")), ctx, 7);
            assertThat(r.text()).isEqualTo("Build two projects.");
            assertThat(r.source()).isEqualTo("Groq AI");
        }
    }

    @Test
    void dotenvParser() {
        Map<String, String> m = DotenvEnvironmentPostProcessor
                .parse(List.of("# comment", "A=1", "B=\"two words\"", "export C=3 # note", "bad line", "D="));
        assertThat(m).containsEntry("A", "1").containsEntry("B", "two words").containsEntry("C", "3").containsEntry("D", "");
        assertThat(m).doesNotContainKey("bad line");
    }

    @Test
    void csvEscapingPreventsFormulaInjection() {
        String csv = CsvWriter.write(List.of("a", "b"), List.of(List.of("=1+1", "x,y")));
        assertThat(csv).contains("'=1+1").contains("\"x,y\"");
    }
}
