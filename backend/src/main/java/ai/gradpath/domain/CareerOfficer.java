package ai.gradpath.domain;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

/**
 * Career Office staff account.
 *
 * <p>AGGREGATION: an officer has job listings, but listings survive if the officer account is removed
 * (jobs.created_by is set to NULL by the database).
 */
@Entity
@Table(name = "career_officers")
@DiscriminatorValue("officer")
@PrimaryKeyJoinColumn(name = "id")
public class CareerOfficer extends User {

    @Column(length = 120, nullable = false)
    private String office = "University Career Office";

    @Column(name = "is_demo", nullable = false)
    private boolean demo;

    @OneToMany(mappedBy = "creator")
    private List<Job> jobs = new ArrayList<>();

    @Override
    public Role getRole() {
        return Role.OFFICER;
    }

    @Override
    public boolean isDemo() {
        return demo;
    }

    public void setDemo(boolean demo) { this.demo = demo; }
    public String getOffice() { return office; }
    public void setOffice(String office) { this.office = office; }
    public List<Job> getJobs() { return jobs; }
}
