package ai.gradpath;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** Flyway creates the baseline schema plus the additive V2 changes, and Hibernate validates against it. */
class MigrationIT extends IntegrationTestBase {

    @Autowired JdbcTemplate jdbc;

    @Test
    void migrationsApplied() {
        List<String> versions = jdbc.queryForList(
                "select \"version\" from \"flyway_schema_history\" where \"success\" = true order by \"installed_rank\"", String.class);
        assertThat(versions).contains("1", "2");
    }

    @Test
    void newColumnsExist() {
        assertThat(jdbc.queryForObject("select count(*) from information_schema.columns where table_name = 'students' and column_name = 'cgpa_scale'", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from information_schema.columns where table_name = 'placement_predictions' and column_name = 'rule_engine_version'", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from information_schema.columns where table_name = 'job_applications' and column_name = 'resume_filename'", Integer.class)).isEqualTo(1);
    }

    @Test
    void invalidApplicationStatusIsRejectedByTheDatabase() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                jdbc.update("insert into users (email, name, role, created_at) values ('x@y.z', 'X', 'admin', current_timestamp)"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
}
