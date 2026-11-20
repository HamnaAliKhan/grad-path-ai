package ai.gradpath.reference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

/**
 * Illustrative career reference content: career paths, canonical skills with keyword aliases, skill categories and
 * learning tips. Requirements are EXAMPLES for guidance, not verified employer requirements.
 */
public final class CareerCatalog {

    public static final String DISCLAIMER = "Career requirements are illustrative examples for guidance, "
            + "not verified requirements from any employer.";

    private CareerCatalog() { }

    @SuppressWarnings("unchecked")
    private static <V> Map<String, V> ordered(Object... kv) {
        Map<String, V> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], (V) kv[i + 1]);
        }
        return Collections.unmodifiableMap(m);
    }

    public static final Map<String, Career> CAREERS = ordered(
        "Software Developer", new Career("Software Developer", "software-developer",
            "Design, build and maintain software applications and services.",
            List.of("Write clean, tested code", "Design APIs and data models", "Review code and fix bugs",
                    "Collaborate in agile teams"),
            ordered("Java", 3, "OOP", 3, "Data Structures", 3, "Git", 2, "SQL", 2, "REST APIs", 2, "Testing", 2,
                    "Problem Solving", 2, "Python", 1, "Agile", 1),
            List.of("Practise data-structure problems weekly", "Contribute to an open-source repository",
                    "Build one full-stack project end to end, with tests"),
            List.of("Task manager with a REST API and unit tests", "Library management system (OOP + SQL)",
                    "Command-line tool published on GitHub")),
        "Data Analyst", new Career("Data Analyst", "data-analyst",
            "Turn raw data into insights that guide business decisions.",
            List.of("Clean and query datasets", "Build dashboards and reports", "Run statistical analysis",
                    "Present findings to stakeholders"),
            ordered("SQL", 3, "Excel", 2, "Python", 2, "Pandas", 2, "Statistics", 3, "Data Visualization", 3,
                    "BI Tools", 2, "Communication", 2),
            List.of("Practise SQL joins and window functions", "Analyse a public, openly licensed dataset",
                    "Publish a dashboard with a short written summary"),
            List.of("Sales dashboard in Power BI or Tableau", "Exploratory analysis report on a public dataset",
                    "SQL analysis of a sample e-commerce database")),
        "Machine Learning Engineer", new Career("Machine Learning Engineer", "machine-learning-engineer",
            "Build, evaluate and deploy machine-learning models responsibly.",
            List.of("Prepare features and datasets", "Train and evaluate models", "Deploy models behind APIs",
                    "Monitor model quality and fairness"),
            ordered("Python", 3, "Machine Learning", 3, "scikit-learn", 2, "Pandas", 2, "Statistics", 3,
                    "Deep Learning", 2, "Git", 1, "SQL", 1, "Docker", 1, "Cloud Basics", 1),
            List.of("Complete an end-to-end ML project with a proper train/test split",
                    "Learn evaluation metrics (precision, recall, ROC-AUC, cross-validation)",
                    "Deploy a model behind a simple web API"),
            List.of("Churn prediction model with an evaluation report", "Image classifier with a small demo app",
                    "Recommendation system on an open movie dataset")),
        "Web Developer", new Career("Web Developer", "web-developer",
            "Create fast, accessible and engaging websites and web applications.",
            List.of("Build responsive interfaces", "Connect front ends to APIs", "Optimise performance and accessibility",
                    "Maintain web applications"),
            ordered("HTML/CSS", 3, "JavaScript", 3, "React", 2, "Node.js", 2, "REST APIs", 2, "Git", 2, "SQL", 1,
                    "Testing", 1),
            List.of("Build and deploy a portfolio site", "Learn one front-end framework properly",
                    "Practise responsive, accessible design on real devices"),
            List.of("Personal portfolio website", "Blog with sign-in and a database",
                    "Weather or news app using a public API")),
        "QA Engineer", new Career("QA Engineer", "qa-engineer",
            "Ensure software quality through planned and automated testing.",
            List.of("Design test cases and plans", "Automate regression tests", "Report and track defects",
                    "Work with developers on quality"),
            ordered("Manual Testing", 3, "Test Automation", 3, "Selenium", 2, "Bug Reporting", 2, "SQL", 1,
                    "Java", 1, "Agile", 2, "Jira", 1, "Communication", 2),
            List.of("Write test cases for a real website", "Automate a login flow with Selenium",
                    "Practise writing clear, reproducible bug reports"),
            List.of("Automated test suite for a demo web shop", "Test plan and bug reports for an open-source app",
                    "API tests with a REST client and JUnit"))
    );

    /** Canonical skill to lower-case keyword aliases (used for resume keyword matching). */
    public static final Map<String, List<String>> SKILL_ALIASES = ordered(
        "Python", List.of("python"), "Java", List.of("java", "spring boot", "spring"),
        "JavaScript", List.of("javascript", "js", "es6", "typescript"),
        "C++", List.of("c++"), "SQL", List.of("sql", "mysql", "postgresql", "postgres", "sqlite"),
        "Git", List.of("git", "github", "gitlab"), "OOP", List.of("oop", "object-oriented", "object oriented"),
        "Data Structures", List.of("data structures", "algorithms", "dsa"),
        "HTML/CSS", List.of("html", "css", "html5", "css3", "tailwind", "bootstrap"),
        "React", List.of("react", "reactjs", "react.js"), "Node.js", List.of("node.js", "nodejs", "express"),
        "REST APIs", List.of("rest", "api", "apis", "restful"),
        "Testing", List.of("unit test", "unit testing", "junit", "testing", "test-driven"),
        "Pandas", List.of("pandas", "numpy"),
        "Statistics", List.of("statistics", "statistical", "hypothesis testing", "regression"),
        "Excel", List.of("excel", "spreadsheets", "pivot"),
        "Data Visualization", List.of("data visualization", "plotly", "matplotlib", "visualization", "dashboard"),
        "BI Tools", List.of("power bi", "tableau", "looker", "qlik"),
        "Machine Learning", List.of("machine learning", "classification", "clustering"),
        "scikit-learn", List.of("scikit-learn", "sklearn"),
        "Deep Learning", List.of("deep learning", "neural network", "tensorflow", "pytorch", "keras"),
        "Docker", List.of("docker", "container"), "Cloud Basics", List.of("aws", "azure", "gcp", "cloud"),
        "Manual Testing", List.of("manual testing", "test cases", "test plan"),
        "Test Automation", List.of("test automation", "automation testing", "cypress", "playwright"),
        "Selenium", List.of("selenium"), "Bug Reporting", List.of("bug report", "defect", "bug tracking"),
        "Agile", List.of("agile", "scrum", "kanban", "sprint"), "Jira", List.of("jira"),
        "Communication", List.of("communication", "presentation", "presented", "public speaking"),
        "Problem Solving", List.of("problem solving", "problem-solving", "leetcode", "competitive programming"),
        "Linux", List.of("linux", "bash", "shell"),
        "Teamwork", List.of("teamwork", "collaborated", "collaboration", "team lead", "led a team")
    );

    public static final Map<String, List<String>> SKILL_CATEGORIES = ordered(
        "Programming", List.of("Java", "Python", "JavaScript", "C++", "OOP", "Data Structures", "Problem Solving"),
        "Data & Analytics", List.of("SQL", "Excel", "Pandas", "Statistics", "Data Visualization", "BI Tools"),
        "AI / ML", List.of("Machine Learning", "scikit-learn", "Deep Learning"),
        "Web & APIs", List.of("HTML/CSS", "React", "Node.js", "REST APIs"),
        "Quality & Tools", List.of("Git", "Testing", "Manual Testing", "Test Automation", "Selenium", "Bug Reporting",
                "Docker", "Cloud Basics", "Linux", "Jira"),
        "Professional", List.of("Communication", "Teamwork", "Agile")
    );

    public static final Map<String, String> LEARNING_TIPS = ordered(
        "Java", "Learn classes, collections and exceptions, then build a small Spring Boot app with JUnit tests.",
        "Python", "Build small programs, then a project that uses files, an API and tests.",
        "JavaScript", "Practise DOM manipulation and fetch(), then build a small interactive page.",
        "OOP", "Model a real domain (library, shop) with classes, inheritance and interfaces; draw its class diagram.",
        "Data Structures", "Implement lists, stacks, maps and trees yourself, then solve 2-3 problems a week.",
        "Problem Solving", "Solve one problem a day and explain your approach out loud.",
        "SQL", "Learn joins, GROUP BY and subqueries; practise on a sample database.",
        "Git", "Learn commits, branches and pull requests; push a project to GitHub.",
        "REST APIs", "Design a small CRUD API with proper status codes and document it.",
        "Testing", "Write unit tests for an existing project; aim for the important paths first.",
        "Statistics", "Cover distributions, hypothesis tests and regression basics.",
        "Excel", "Practise pivot tables, lookups and charts on a real dataset.",
        "Pandas", "Clean and summarise a messy dataset end to end.",
        "Data Visualization", "Recreate a few published charts and tell a clear story with them.",
        "BI Tools", "Build one dashboard in Power BI or Tableau from a public dataset.",
        "Machine Learning", "Complete an end-to-end supervised-learning project with proper evaluation.",
        "scikit-learn", "Use pipelines, cross-validation and a held-out test set in one project.",
        "Deep Learning", "Train a small neural network on an image or text dataset and report its limits.",
        "Docker", "Containerise one of your projects and run it with a single command.",
        "Cloud Basics", "Deploy a small app on a free cloud tier and learn its basic services.",
        "HTML/CSS", "Build a responsive page with semantic HTML and flexbox/grid.",
        "React", "Build a small app with components, state and an API call.",
        "Node.js", "Build a small Express API with one database table.",
        "Manual Testing", "Write structured test cases for a website you use daily.",
        "Test Automation", "Automate three regression checks for a demo app.",
        "Selenium", "Automate a login and search flow with Selenium WebDriver.",
        "Bug Reporting", "Practise reports with steps to reproduce, expected vs actual result and evidence.",
        "Agile", "Run a two-week personal sprint with a small backlog and review.",
        "Jira", "Track a personal project's tasks and bugs in a free Jira board.",
        "Communication", "Record a two-minute project pitch and ask for feedback.",
        "Teamwork", "Join a group project or hackathon and document your role.",
        "Linux", "Use the command line daily for files, processes and scripts.",
        "C++", "Practise pointers, STL containers and one small console project."
    );

    public static final Map<String, String> DEPARTMENT_DEFAULT_CAREER = ordered(
        "Computer Science", "Software Developer", "Software Engineering", "QA Engineer",
        "Data Science", "Machine Learning Engineer", "Information Technology", "Web Developer",
        "Electrical Engineering", "Software Developer", "Business Analytics", "Data Analyst"
    );

    public static final List<String> DEGREES = List.of("BS", "BE / BEng", "BSc", "BBA", "MS / MSc", "MBA", "Other");
    public static final List<String> DEPARTMENTS = List.of("Computer Science", "Software Engineering", "Data Science",
            "Information Technology", "Electrical Engineering", "Business Analytics", "Other");
    public static final List<String> JOB_LEVELS = List.of("Internship", "Entry Level", "Junior", "Mid Level");
    public static final List<String> AVAILABILITY = List.of("Immediately", "Within 1 month", "After graduation",
            "Part-time only");

    /** Every selectable skill: all aliased skills plus all career skills, sorted. */
    public static final List<String> SKILL_CATALOG = buildCatalog();

    private static List<String> buildCatalog() {
        TreeSet<String> all = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        all.addAll(SKILL_ALIASES.keySet());
        CAREERS.values().forEach(c -> all.addAll(c.skills().keySet()));
        return List.copyOf(new ArrayList<>(all));
    }

    public static List<String> careerNames() {
        return List.copyOf(CAREERS.keySet());
    }

    public static Optional<Career> career(String name) {
        return Optional.ofNullable(name == null ? null : CAREERS.get(name));
    }

    public static Optional<Career> bySlug(String slug) {
        return CAREERS.values().stream().filter(c -> c.slug().equals(slug)).findFirst();
    }

    /** A valid career name: the given one if known, else the department default, else Software Developer. */
    public static String resolveCareer(String requested, String department) {
        if (requested != null && CAREERS.containsKey(requested)) {
            return requested;
        }
        return DEPARTMENT_DEFAULT_CAREER.getOrDefault(department == null ? "" : department, "Software Developer");
    }

    public static String tipFor(String skill, boolean learning) {
        return LEARNING_TIPS.getOrDefault(skill, learning
                ? "Finish your " + skill + " learning path and apply it in a mini-project."
                : "Start with a beginner course in " + skill + ", then build one small project.");
    }

    /** Keyword aliases for a skill (the skill name itself when it has none). */
    public static List<String> aliases(String skill) {
        return SKILL_ALIASES.getOrDefault(skill, List.of(skill.toLowerCase()));
    }
}
