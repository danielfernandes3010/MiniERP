package br.com.gestao.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SqliteMigrationRunner implements ApplicationRunner {
    private static final Pattern MIGRATION_NAME = Pattern.compile("V(\\d+)__(.+)\\.sql");
    private final DataSource dataSource;

    public SqliteMigrationRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS schema_version (
                      version INTEGER PRIMARY KEY,
                      description TEXT NOT NULL,
                      installed_on TEXT NOT NULL
                    )
                    """);
        }

        Resource[] migrations = new PathMatchingResourcePatternResolver()
                .getResources("classpath:db/migration/V*__*.sql");
        Arrays.sort(migrations, (left, right) -> filename(left).compareTo(filename(right)));
        for (Resource nullableMigration : migrations) {
            Resource migration = Objects.requireNonNull(nullableMigration, "Migração SQL não pode ser nula.");
            String filename = filename(migration);
            Matcher matcher = MIGRATION_NAME.matcher(filename);
            if (!matcher.matches()) throw new IllegalStateException("Nome de migração inválido: " + filename);
            int version = Integer.parseInt(matcher.group(1));
            String description = matcher.group(2).replace('_', ' ');
            applyIfNeeded(migration, version, description);
        }
    }

    private String filename(Resource resource) {
        return Objects.requireNonNull(resource.getFilename(), "Nome de migração SQL não pode ser nulo.");
    }

    private void applyIfNeeded(@NonNull Resource migration, int version, String description) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement query = connection.prepareStatement("SELECT 1 FROM schema_version WHERE version = ?")) {
                    query.setInt(1, version);
                    try (ResultSet result = query.executeQuery()) {
                        if (result.next()) {
                            connection.commit();
                            return;
                        }
                    }
                }
                ScriptUtils.executeSqlScript(connection, migration);
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO schema_version(version, description, installed_on) VALUES (?, ?, ?)")) {
                    insert.setInt(1, version);
                    insert.setString(2, description);
                    insert.setString(3, OffsetDateTime.now().toString());
                    insert.executeUpdate();
                }
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        }
    }
}
