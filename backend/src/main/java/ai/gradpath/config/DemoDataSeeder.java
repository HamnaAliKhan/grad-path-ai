package ai.gradpath.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ai.gradpath.domain.ActivityLog;
import ai.gradpath.domain.CareerGoal;
import ai.gradpath.domain.CareerOfficer;
import ai.gradpath.domain.GoalTask;
import ai.gradpath.domain.Job;
import ai.gradpath.domain.JobApplication;
import ai.gradpath.domain.Message;
import ai.gradpath.domain.Student;
import ai.gradpath.domain.User;
import ai.gradpath.reference.CareerCatalog;
import ai.gradpath.repository.ActivityRepository;
import ai.gradpath.repository.ApplicationRepository;
import ai.gradpath.repository.JobRepository;
import ai.gradpath.repository.MessageRepository;
import ai.gradpath.repository.StudentRepository;
import ai.gradpath.repository.UserRepository;
import ai.gradpath.service.ApplicationService;

/**
 * Inserts clearly labelled FICTIONAL demonstration records for local evaluation (SEED_DEMO_DATA=true).
 * It only adds records that are missing and never updates or deletes existing data. Demo students live at
 * "Demo University (fictional)", use @demo.gradpath.ai addresses and are flagged is_demo; demo listings are read-only.
 */
@Component
public class DemoDataSeeder {

    private static final List<String> FIRST = List.of("Ayesha", "Bilal", "Hira", "Usman", "Sana", "Hamza", "Zainab",
            "Daniyal", "Maryam", "Ali", "Fatima", "Omar", "Noor", "Saad", "Iqra", "Talha", "Areeba", "Ahmed", "Mahnoor",
            "Faizan", "Laiba", "Rayyan", "Anum", "Shahzaib");
    private static final List<String> LAST = List.of("Khan", "Malik", "Siddiqui", "Raza", "Qureshi", "Ansari", "Sheikh",
            "Farooqui", "Baig", "Mirza");
    private static final List<String> CERTS = List.of("Google Data Analytics", "AWS Cloud Practitioner",
            "Meta Front-End Basics", "Oracle Java Foundations", "Scrum Fundamentals", "ISTQB Foundation");

    private static final String[][] JOBS = {
        {"Junior Java Developer", "NovaStack Labs (fictional)", "Software Developer", "Entry Level", "Java,OOP,Git,SQL",
         "Build internal tools and REST services with a small, friendly engineering team.", "Karachi (Hybrid)"},
        {"Software Engineering Intern", "Brightpeak Systems (fictional)", "Software Developer", "Internship", "Java,Data Structures,Git",
         "Summer internship working on feature development and unit tests.", "Remote"},
        {"Data Analyst Trainee", "Lumen Analytics (fictional)", "Data Analyst", "Entry Level", "SQL,Excel,Data Visualization,Statistics",
         "Prepare weekly KPI dashboards and ad-hoc analyses for the sales team.", "Lahore (On-site)"},
        {"BI Reporting Intern", "Orbit Retail Group (fictional)", "Data Analyst", "Internship", "SQL,BI Tools,Excel",
         "Support the BI team with data cleaning and Power BI reports.", "Karachi (Hybrid)"},
        {"Junior ML Engineer", "Cortexa AI (fictional)", "Machine Learning Engineer", "Junior", "Python,Machine Learning,scikit-learn,Pandas",
         "Train and evaluate classical ML models and ship them as internal APIs.", "Remote"},
        {"Data Science Intern", "Helix Insights (fictional)", "Machine Learning Engineer", "Internship", "Python,Pandas,Statistics",
         "Assist with feature engineering and experiment tracking.", "Islamabad (Hybrid)"},
        {"Front-End Developer", "Pixelforge Studio (fictional)", "Web Developer", "Junior", "HTML/CSS,JavaScript,React,Git",
         "Craft responsive interfaces for client web apps.", "Karachi (On-site)"},
        {"Full-Stack Web Intern", "Cloudmint (fictional)", "Web Developer", "Internship", "JavaScript,Node.js,REST APIs,SQL",
         "Work across the stack on a SaaS product for small businesses.", "Remote"},
        {"QA Automation Trainee", "Testwave Technologies (fictional)", "QA Engineer", "Entry Level", "Test Automation,Selenium,Java,Bug Reporting",
         "Write automated regression suites and triage defects.", "Lahore (Hybrid)"},
        {"Manual QA Intern", "Safeline Software (fictional)", "QA Engineer", "Internship", "Manual Testing,Bug Reporting,Jira,Agile",
         "Execute test plans and document defects clearly.", "Karachi (On-site)"},
    };

    private final UserRepository users;
    private final StudentRepository students;
    private final JobRepository jobs;
    private final ApplicationRepository applications;
    private final MessageRepository messages;
    private final ActivityRepository activity;
    private final ApplicationService applicationService;
    private final ai.gradpath.security.PasswordService passwords;

    public DemoDataSeeder(UserRepository users, StudentRepository students, JobRepository jobs,
                          ApplicationRepository applications, MessageRepository messages, ActivityRepository activity,
                          ApplicationService applicationService, ai.gradpath.security.PasswordService passwords) {
        this.users = users;
        this.students = students;
        this.jobs = jobs;
        this.applications = applications;
        this.messages = messages;
        this.activity = activity;
        this.applicationService = applicationService;
        this.passwords = passwords;
    }

    /** Add missing demo accounts, listings, applications and a welcome message. */
    @Transactional
    public void seed(boolean withPasswords) {
        if (users.findByEmail(GradPathProperties.DEMO_OFFICER_EMAIL).isEmpty()) {
            CareerOfficer officer = new CareerOfficer();
            officer.setEmail(GradPathProperties.DEMO_OFFICER_EMAIL);
            officer.setName("Jordan Lee");
            officer.setOffice("Demo University Career Office (fictional)");
            officer.setDemo(true);
            officer.setPasswordHash(withPasswords ? passwords.hash(GradPathProperties.DEMO_OFFICER_PASSWORD) : null);
            users.save(officer);
        }
        if (users.findByEmail(GradPathProperties.DEMO_STUDENT_EMAIL).isEmpty()) {
            seedStudents(new Random(42));
            seedDemoStudent(withPasswords);
        }
        if (jobs.findAll().stream().noneMatch(Job::isDemo)) {
            for (String[] j : JOBS) {
                Job job = new Job(j[0], j[1], j[2], j[3], j[4], j[5], j[6]);
                job.setDemo(true);
                job.setOpen(true);
                jobs.save(job);
            }
        }
        jobs.flush();
        seedApplicationsAndMessage();
    }

    private void seedStudents(Random rng) {
        List<String> depts = new ArrayList<>(CareerCatalog.DEPARTMENT_DEFAULT_CAREER.keySet());
        for (int i = 0; i < 24; i++) {
            String dept = depts.get(i % depts.size());
            String career = CareerCatalog.DEPARTMENT_DEFAULT_CAREER.get(dept);
            List<String> req = new ArrayList<>(CareerCatalog.CAREERS.get(career).skillNames());
            Collections.shuffle(req, rng);
            int k = 2 + rng.nextInt(req.size() - 3);
            int nLearning = 1 + rng.nextInt(2);
            List<String> certs = new ArrayList<>(CERTS);
            Collections.shuffle(certs, rng);
            Student s = new Student();
            s.setEmail(FIRST.get(i).toLowerCase() + "." + i + "@demo.gradpath.ai");
            s.setName(FIRST.get(i) + " " + LAST.get(rng.nextInt(LAST.size())));
            s.setPasswordHash(null);                // demo records cannot sign in
            s.setUniversity("Demo University (fictional)");
            s.setDegree("BS");
            s.setDepartment(dept);
            s.setGraduationYear(rng.nextBoolean() ? 2026 : 2027);
            s.setCgpaScale(4.0);
            s.setCgpa(Math.round((2.1 + rng.nextDouble() * 1.85) * 100) / 100.0);
            s.setSkills(String.join(",", req.subList(0, k)));
            s.setLearningSkills(String.join(",", req.subList(k, Math.min(req.size(), k + nLearning))));
            s.setCertifications(String.join(",", certs.subList(0, rng.nextInt(4))));
            s.setProjects(rng.nextInt(7));
            double r = rng.nextDouble() * 10;
            s.setInternships(r < 5 ? 0 : r < 8.5 ? 1 : 2);
            s.setWorkExperienceMonths(List.of(0, 0, 3, 6, 12).get(rng.nextInt(5)));
            s.setCommunication(3 + rng.nextInt(8));
            s.setExtracurricular(1 + rng.nextInt(10));
            s.setTargetCareer(career);
            s.setDemo(true);
            s.setCity("Karachi");
            s.setPhone("+920000000" + String.format("%03d", i));
            students.save(s);
        }
    }

    private void seedDemoStudent(boolean withPasswords) {
        Student alex = new Student();
        alex.setEmail(GradPathProperties.DEMO_STUDENT_EMAIL);
        alex.setName("Alex Morgan");
        alex.setPasswordHash(withPasswords ? passwords.hash(GradPathProperties.DEMO_STUDENT_PASSWORD) : null);
        alex.setUniversity("Demo University (fictional)");
        alex.setDegree("BS");
        alex.setDepartment("Computer Science");
        alex.setGraduationYear(2027);
        alex.setCgpaScale(4.0);
        alex.setCgpa(3.35);
        alex.setSkills("Java,SQL,Git,OOP,Problem Solving");
        alex.setLearningSkills("Data Structures,REST APIs,Testing");
        alex.setCertifications("Oracle Java Foundations");
        alex.setProjects(3);
        alex.setInternships(1);
        alex.setWorkExperienceMonths(4);
        alex.setCommunication(7);
        alex.setExtracurricular(6);
        alex.setTargetCareer("Software Developer");
        alex.setDemo(true);
        alex.setPhone("+920000000999");
        alex.setCity("Karachi");
        CareerGoal goal = new CareerGoal("Become job-ready for Software Developer roles", "Sample goal for the demo account.");
        goal.addTask(new GoalTask("Build a portfolio project with tests", true));
        goal.addTask(new GoalTask("Learn REST API design", false));
        goal.addTask(new GoalTask("Practise 30 data-structure problems", false));
        goal.addTask(new GoalTask("Prepare for technical interviews", false));
        alex.addGoal(goal);
        students.saveAndFlush(alex);
        activity.save(new ActivityLog(alex.getId(), alex.getName(), "Profile created", "Demo profile (fictional)"));
        activity.save(new ActivityLog(alex.getId(), alex.getName(), "Goal created", goal.getTitle()));
    }

    private void seedApplicationsAndMessage() {
        List<Job> demoJobs = jobs.findAll().stream().filter(Job::isDemo).toList();
        if (applications.count() == 0 && !demoJobs.isEmpty()) {
            Random rng = new Random(7);
            for (Student st : students.demoStudents().stream().limit(14).toList()) {
                List<Job> pool = demoJobs.stream().filter(j -> j.getCategory().equals(st.getTargetCareer())).toList();
                Job job = (pool.isEmpty() ? demoJobs : pool).get(rng.nextInt(pool.isEmpty() ? demoJobs.size() : pool.size()));
                String text = ("Fictional demo resume. " + st.getDegree() + " in " + st.getDepartment() + ". Skills: "
                        + String.join(", ", st.skillList()) + ". Built " + st.getProjects() + " projects and completed "
                        + st.getInternships() + " internship(s). Developed and tested applications, improved results by 20% "
                        + "and collaborated in agile teams. ").repeat(2);
                JobApplication app = new JobApplication(job, st);
                app.setPhone(st.getPhone());
                app.setPortfolioUrl("https://example.com/demo-portfolio");
                app.setAvailability(rng.nextBoolean() ? "Immediately" : "Within 1 month");
                app.setCoverNote("Fictional demo application created for the project demonstration. Not a real candidate.");
                app.setResumeText(text);
                app.setResumeFilename("demo-resume.pdf");
                app.setStatus(List.of("Submitted", "Submitted", "Under review", "Shortlisted").get(rng.nextInt(4)));
                ApplicationService.Fit fit = applicationService.computeFit(st, job, text);
                app.applyFit(fit.skillMatch(), fit.resumeMatch(), fit.readiness().category().label(),
                        fit.readiness().score(), fit.fitScore());
                applications.save(app);
            }
        }
        User officer = users.findByEmail(GradPathProperties.DEMO_OFFICER_EMAIL).orElse(null);
        User alex = users.findByEmail(GradPathProperties.DEMO_STUDENT_EMAIL).orElse(null);
        if (officer != null && alex != null && messages.between(officer.getId(), alex.getId()).isEmpty()) {
            messages.save(new Message(officer.getId(), alex.getId(), "Welcome to GradPath AI! This is a demo message "
                    + "from the (fictional) Career Office. Reply here to try messaging.", null));
        }
    }
}
