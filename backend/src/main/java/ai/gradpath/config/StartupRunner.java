package ai.gradpath.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import ai.gradpath.service.AuthService;

/**
 * Runs once after Flyway migrations: optionally seeds missing demo data and switches the demo accounts on or off.
 * Never deletes existing user data.
 */
@Component
public class StartupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupRunner.class);

    private final DemoDataSeeder seeder;
    private final AuthService auth;
    private final GradPathProperties props;

    public StartupRunner(DemoDataSeeder seeder, AuthService auth, GradPathProperties props) {
        this.seeder = seeder;
        this.auth = auth;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (props.seedDemoData()) {
            seeder.seed(props.demoLoginEnabled());
        }
        auth.syncDemoAccounts(props.demoLoginEnabled());
        log.info("GradPath AI ready (demo login {}, officer registration {}, assistant provider '{}')",
                props.demoLoginEnabled() ? "enabled" : "disabled",
                props.officerSignupEnabled() ? "enabled" : "disabled",
                props.chat().provider());
    }
}
