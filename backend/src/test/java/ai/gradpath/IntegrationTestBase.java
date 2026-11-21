package ai.gradpath;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import ai.gradpath.domain.CareerOfficer;
import ai.gradpath.domain.Job;
import ai.gradpath.domain.Student;
import ai.gradpath.repository.JobRepository;
import ai.gradpath.repository.UserRepository;
import ai.gradpath.security.PasswordService;

/**
 * Spring context on the Neon <b>test branch</b> ({@link TestDatabase}); every test runs in a transaction that is
 * rolled back, so nothing is kept. Skipped when no test database is configured.
 */
@ExtendWith(TestDatabase.class)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public abstract class IntegrationTestBase {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("gradpath.database-url", TestDatabase::url);
    }

    @Autowired protected UserRepository users;
    @Autowired protected JobRepository jobs;
    @Autowired protected PasswordService passwords;

    protected static String uniqueEmail(String prefix) {
        return prefix + SEQ.incrementAndGet() + "-" + System.nanoTime() + "@test.edu";
    }

    /** An onboarded student who can sign in with password "Passw0rd1". */
    protected Student student(String name) {
        Student s = new Student();
        s.setName(name);
        s.setEmail(uniqueEmail("student"));
        s.setPasswordHash(passwords.hash("Passw0rd1"));
        s.setUniversity("Test University");
        s.setDegree("BS");
        s.setDepartment("Computer Science");
        s.setGraduationYear(2027);
        s.setCgpaScale(4.0);
        s.setCgpa(3.4);
        s.setSkills("Java,SQL,Git,OOP");
        s.setLearningSkills("Testing");
        s.setCertifications("Oracle Java Foundations");
        s.setProjects(3);
        s.setInternships(1);
        s.setCommunication(7);
        s.setExtracurricular(5);
        s.setTargetCareer("Software Developer");
        s.setPhone("+923001112233");
        s.setCity("Karachi");
        return users.saveAndFlush(s) instanceof Student saved ? saved : s;
    }

    protected CareerOfficer officer(String name) {
        CareerOfficer o = new CareerOfficer();
        o.setName(name);
        o.setEmail(uniqueEmail("officer"));
        o.setPasswordHash(passwords.hash("Passw0rd1"));
        o.setOffice("Career Office");
        return (CareerOfficer) users.saveAndFlush(o);
    }

    protected Job job(CareerOfficer owner, boolean open, boolean demo) {
        Job j = new Job("Graduate Java Developer", "Acme", "Software Developer", "Entry Level", "Java,SQL,Git,REST APIs",
                "Build and test backend services for a growing product team.", "Remote");
        j.setOpen(open);
        j.setDemo(demo);
        j.setCreatedBy(owner == null ? null : owner.getId());
        return jobs.saveAndFlush(j);
    }
}
