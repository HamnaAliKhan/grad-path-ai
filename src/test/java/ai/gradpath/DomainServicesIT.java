package ai.gradpath;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;

import ai.gradpath.domain.CareerGoal;
import ai.gradpath.domain.CareerOfficer;
import ai.gradpath.domain.Job;
import ai.gradpath.domain.JobApplication;
import ai.gradpath.domain.Student;
import ai.gradpath.repository.ActivityRepository;
import ai.gradpath.service.ApplicationService;
import ai.gradpath.service.ApplicationService.ApplicationForm;
import ai.gradpath.service.GoalService;
import ai.gradpath.service.JobService;
import ai.gradpath.service.MessageService;
import ai.gradpath.service.NotFoundException;
import ai.gradpath.service.ProfileForm;
import ai.gradpath.service.ReadinessService;
import ai.gradpath.service.Result;
import ai.gradpath.service.StudentService;
import ai.gradpath.service.UnitServicesTestAccess;

/** Applications, job ownership, goals, messaging, profile validation and readiness history against the database. */
class DomainServicesIT extends IntegrationTestBase {

    @Autowired ApplicationService applications;
    @Autowired JobService jobService;
    @Autowired GoalService goals;
    @Autowired MessageService messages;
    @Autowired StudentService students;
    @Autowired ReadinessService readiness;
    @Autowired ActivityRepository activity;

    private static ApplicationForm validForm() {
        ApplicationForm f = new ApplicationForm();
        f.setPhone("+923001112233");
        f.setAvailability("Immediately");
        f.setPortfolioUrl("https://github.com/someone");
        f.setCoverNote("I would love to join because I build and test Java services and enjoy learning quickly.");
        f.setConsent(true);
        return f;
    }

    // ---------- applications ----------

    @Test
    @WithMockUser(roles = "STUDENT")
    void applyStoresFitResumeAndActivity() throws IOException {
        Student s = student("Apply Student");
        Job j = job(officer("Owner One"), true, false);
        Result<JobApplication> r = applications.apply(s, j.getId(), validForm(), UnitServicesTestAccess.resumePdf(), "cv.pdf");
        assertThat(r.isOk()).as(String.join("; ", r.errors())).isTrue();
        JobApplication a = r.value();
        assertThat(a.getStatus()).isEqualTo("Submitted");
        assertThat(a.getSkillMatch()).isEqualTo(75);          // Java, SQL, Git of 4
        assertThat(a.getResumeMatch()).isEqualTo(100);
        assertThat(a.getFitScore()).isBetween(0, 100);
        assertThat(a.getResumeFilename()).isEqualTo("cv.pdf");
        assertThat(activity.recent(s.getId(), 5)).anyMatch(l -> l.getAction().equals("Applied for a job"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void duplicateAndClosedApplicationsAreRejected() throws IOException {
        Student s = student("Dup Student");
        CareerOfficer o = officer("Owner Two");
        Job open = job(o, true, false);
        Job closed = job(o, false, false);
        byte[] pdf = UnitServicesTestAccess.resumePdf();
        assertThat(applications.apply(s, open.getId(), validForm(), pdf, "a.pdf").isOk()).isTrue();
        assertThat(applications.apply(s, open.getId(), validForm(), pdf, "a.pdf").error()).contains("already applied");
        assertThat(applications.apply(s, closed.getId(), validForm(), pdf, "a.pdf").error()).contains("closed");
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void applicationValidation() {
        Student s = student("Invalid Student");
        Job j = job(officer("Owner Three"), true, false);
        ApplicationForm bad = new ApplicationForm();
        bad.setPhone("12");
        bad.setCoverNote("too short");
        Result<JobApplication> r = applications.apply(s, j.getId(), bad, "not a pdf".getBytes(), "x.pdf");
        assertThat(r.errors()).hasSize(6)
                .anyMatch(e -> e.contains("phone")).anyMatch(e -> e.contains("availability"))
                .anyMatch(e -> e.contains("portfolio")).anyMatch(e -> e.contains("Cover note"))
                .anyMatch(e -> e.contains("confirm")).anyMatch(e -> e.contains("not a PDF"));
    }

    @Test
    @WithMockUser(roles = "OFFICER")
    void statusMustBeOneOfTheSixValues() throws IOException {
        Student s = student("Status Student");
        CareerOfficer o = officer("Reviewer");
        Job j = job(o, true, false);
        JobApplication a = applyAs(s, j);
        assertThat(applications.updateStatus(o, a.getId(), "Interview").isOk()).isTrue();
        assertThat(applications.updateStatus(o, a.getId(), "Hired").isOk()).isFalse();
        assertThat(applications.requireForOfficer(a.getId()).getStatus()).isEqualTo("Interview");
        assertThat(applications.resumeForOfficer(a.getId())).startsWith((byte) '%', (byte) 'P');
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCannotSeeOtherStudentsApplications() throws IOException {
        Student owner = student("Owner Student");
        Student other = student("Other Student");
        JobApplication a = applyAs(owner, job(officer("Owner Four"), true, false));
        assertThatThrownBy(() -> applications.requireForStudent(other.getId(), a.getId())).isInstanceOf(NotFoundException.class);
        assertThat(applications.resumeForStudent(owner.getId(), a.getId())).isNotEmpty();
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void officerOnlyOperationsAreDeniedToStudents() {
        assertThatThrownBy(() -> applications.all()).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> jobService.create(new JobService.JobForm(), officer("X Officer")))
                .isInstanceOf(AccessDeniedException.class);
    }

    private JobApplication applyAs(Student s, Job j) throws IOException {
        var ctx = org.springframework.security.core.context.SecurityContextHolder.getContext();
        var previous = ctx.getAuthentication();
        ctx.setAuthentication(new org.springframework.security.authentication.TestingAuthenticationToken("s", null, "ROLE_STUDENT"));
        try {
            return applications.apply(s, j.getId(), validForm(), UnitServicesTestAccess.resumePdf(), "cv.pdf").value();
        } finally {
            ctx.setAuthentication(previous);
        }
    }

    // ---------- job ownership ----------

    @Test
    @WithMockUser(roles = "OFFICER")
    void officersManageOnlyTheirOwnNonDemoListings() {
        CareerOfficer owner = officer("Owner Officer");
        CareerOfficer other = officer("Other Officer");
        JobService.JobForm form = new JobService.JobForm();
        form.setTitle("QA Intern");
        form.setCompany("Testwave");
        form.setCategory("QA Engineer");
        form.setLevel("Internship");
        form.setLocation("Remote");
        form.setSkills(List.of("Manual Testing", "Jira"));
        form.setDescription("Execute test plans and write clear bug reports.");
        Job created = jobService.create(form, owner).value();
        assertThat(created.getCreatedBy()).isEqualTo(owner.getId());
        assertThat(created.isOpen()).isTrue();

        assertThatThrownBy(() -> jobService.setOpen(created.getId(), false, other)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> jobService.delete(created.getId(), other)).isInstanceOf(AccessDeniedException.class);
        assertThat(jobService.setOpen(created.getId(), false, owner).isOpen()).isFalse();

        Job demo = job(null, true, true);
        assertThatThrownBy(() -> jobService.update(demo.getId(), form, owner)).isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("read-only");
        assertThat(jobService.delete(created.getId(), owner)).isEqualTo("QA Intern");
        assertThat(jobs.findById(created.getId())).isEmpty();
    }

    @Test
    @WithMockUser(roles = "OFFICER")
    void listingValidation() {
        JobService.JobForm f = new JobService.JobForm();
        f.setCategory("Astronaut");
        assertThat(jobService.validate(f)).anyMatch(e -> e.contains("Job title")).anyMatch(e -> e.contains("career category"))
                .anyMatch(e -> e.contains("experience level")).anyMatch(e -> e.contains("required skill"))
                .anyMatch(e -> e.contains("Description"));
    }

    // ---------- goals ----------

    @Test
    @WithMockUser(roles = "STUDENT")
    void goalProgressIsCalculatedFromTasks() {
        Student s = student("Goal Student");
        CareerGoal g = goals.create(s.getId(), "Land an internship", "Summer").value();
        var t1 = goals.addTask(s.getId(), g.getId(), "Update CV").value();
        goals.addTask(s.getId(), g.getId(), "Apply to 5 jobs");
        goals.addTask(s.getId(), g.getId(), "Mock interview");
        goals.addTask(s.getId(), g.getId(), "Follow up");
        assertThat(g.progress()).isZero();
        goals.setDone(s.getId(), t1.getId(), true);
        assertThat(g.progress()).isEqualTo(25);
        GoalService.Summary sum = goals.summary(goals.list(s.getId()));
        assertThat(sum.total()).isEqualTo(4);
        assertThat(sum.done()).isEqualTo(1);
        assertThat(sum.progress()).isEqualTo(25);
        assertThat(sum.next()).isEqualTo("Apply to 5 jobs");
        goals.deleteTask(s.getId(), t1.getId());
        assertThat(goals.summary(goals.list(s.getId())).progress()).isZero();
        assertThat(goals.update(s.getId(), g.getId(), "Land a summer internship", "").isOk()).isTrue();
        assertThat(goals.create(s.getId(), "ab", "").isOk()).isFalse();
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void goalsOfOtherStudentsAreNotAccessible() {
        Student owner = student("Goal Owner");
        Student intruder = student("Goal Intruder");
        CareerGoal g = goals.create(owner.getId(), "Private goal", "").value();
        assertThatThrownBy(() -> goals.delete(intruder.getId(), g.getId())).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> goals.addTask(intruder.getId(), g.getId(), "x")).isInstanceOf(NotFoundException.class);
        assertThat(goals.delete(owner.getId(), g.getId())).isEqualTo("Private goal");
        assertThat(goals.list(owner.getId())).isEmpty();
    }

    // ---------- messaging ----------

    @Test
    void messagingBetweenStudentAndOfficerWithReadStatus() {
        Student s = student("Msg Student");
        CareerOfficer o = officer("Msg Officer");
        assertThat(messages.send(s, o.getId(), "Hello, I have a question.").isOk()).isTrue();
        assertThat(messages.unreadCount(o.getId())).isEqualTo(1);
        assertThat(messages.conversations(o.getId())).singleElement().satisfies(c -> assertThat(c.unread()).isEqualTo(1));
        assertThat(messages.thread(o.getId(), s.getId())).hasSize(1);
        assertThat(messages.unreadCount(o.getId())).isZero();
        assertThat(messages.send(s, student("Peer").getId(), "hi").isOk()).isFalse();   // student -> student
        assertThat(messages.send(s, o.getId(), " ").isOk()).isFalse();
        Student demo = student("Demo Kid");
        demo.setDemo(true);
        users.saveAndFlush(demo);
        assertThat(messages.send(o, demo.getId(), "hi").isOk()).isFalse();             // real officer -> demo student
    }

    // ---------- profile + readiness history ----------

    @Test
    @WithMockUser(roles = "STUDENT")
    void profileValidationAndReadinessHistory() {
        Student s = student("Profile Student");
        ProfileForm bad = ProfileForm.from(s);
        bad.setCgpa(4.5);
        bad.setCommunication(11);
        bad.setSkills(List.of("Underwater Basket Weaving"));
        assertThat(students.updateProfile(s.getId(), bad, false).errors())
                .anyMatch(e -> e.contains("CGPA")).anyMatch(e -> e.contains("Communication"))
                .anyMatch(e -> e.contains("skills from the list"));

        ProfileForm good = ProfileForm.from(s);
        good.setCgpaScale(10.0);
        good.setCgpa(8.6);
        assertThat(students.updateProfile(s.getId(), good, false).isOk()).isTrue();
        assertThat(readiness.history(s.getId())).hasSize(1);
        assertThat(readiness.history(s.getId()).get(0).getRuleEngineVersion()).isEqualTo("rules-1.0");
        assertThat(readiness.history(s.getId()).get(0).isLegacy()).isFalse();
        students.updateProfile(s.getId(), good, false);            // unchanged score -> no new history row
        assertThat(readiness.history(s.getId())).hasSize(1);
        good.setProjects(7);
        students.updateProfile(s.getId(), good, false);
        assertThat(readiness.history(s.getId())).hasSize(2);
    }
}
