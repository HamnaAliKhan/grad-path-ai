package ai.gradpath.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * SINGLETON: one shared connection pool for the whole application.
 *
 * <p>Neon shows connection strings in libpq form ({@code postgresql://user:pass@host/db?sslmode=require}); they are
 * converted to JDBC here. Neon is the only supported database: start-up stops with a clear message when
 * {@code DATABASE_URL} is missing.
 * Credentials are never logged - only the host name.
 */
@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Bean
    public DataSource dataSource(GradPathProperties props) {
        String url = props.databaseUrl() == null ? "" : props.databaseUrl().strip();
        if (url.isEmpty()) {
            throw new IllegalStateException("DATABASE_URL is not set. Add your Neon connection string to the .env file "
                    + "(see .env.example) or set it as an environment variable.");
        }
        JdbcParts parts = toJdbc(url);
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(parts.jdbcUrl());
        if (parts.user() != null) {
            cfg.setUsername(parts.user());
            cfg.setPassword(parts.password());
        }
        log.info("Using Neon PostgreSQL database at host {}", parts.host());
        cfg.setPoolName("gradpath");
        cfg.setMaximumPoolSize(8);
        cfg.setConnectionTimeout(20_000);   // Neon may need a few seconds to wake up
        cfg.setMaxLifetime(240_000);        // Neon closes idle connections; recycle before that
        cfg.setKeepaliveTime(60_000);
        return new HikariDataSource(cfg);
    }

    /** Parts of a connection string converted for the JDBC driver. */
    record JdbcParts(String jdbcUrl, String user, String password, String host) { }

    static JdbcParts toJdbc(String databaseUrl) {
        String url = databaseUrl.strip();
        if (url.startsWith("jdbc:")) {
            String host = url.replaceFirst("^jdbc:[a-z0-9]+:(//)?", "").replaceFirst("[/;?].*$", "");
            return new JdbcParts(url, null, null, host);
        }
        URI uri = URI.create(url.replaceFirst("^postgres(ql)?(\\+[a-z0-9]+)?://", "postgresql://"));
        String user = null;
        String password = null;
        if (uri.getRawUserInfo() != null) {
            String[] info = uri.getRawUserInfo().split(":", 2);
            user = decode(info[0]);
            password = info.length > 1 ? decode(info[1]) : null;
        }
        int port = uri.getPort() > 0 ? uri.getPort() : 5432;
        List<String> params = new ArrayList<>();
        if (uri.getRawQuery() != null) {
            for (String p : uri.getRawQuery().split("&")) {
                // channel_binding is a libpq-only option; the JDBC driver negotiates SCRAM itself.
                if (!p.isBlank() && !p.startsWith("channel_binding=")) {
                    params.add(p);
                }
            }
        }
        String jdbc = "jdbc:postgresql://" + uri.getHost() + ":" + port + uri.getRawPath()
                + (params.isEmpty() ? "" : "?" + String.join("&", params));
        return new JdbcParts(jdbc, user, password, uri.getHost());
    }

    private static String decode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }
}
