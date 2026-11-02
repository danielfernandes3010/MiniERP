package br.com.gestao.config;

import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@Configuration
public class DatabaseConfig {
    @Bean
    HibernatePropertiesCustomizer sqliteConnectionPragmas(DataSource dataSource) {
        return properties -> {
            try (Connection connection = dataSource.getConnection();
                 Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
                statement.execute("PRAGMA busy_timeout = 10000");
                statement.execute("PRAGMA journal_mode = WAL");
            } catch (java.sql.SQLException exception) {
                throw new IllegalStateException("Não foi possível configurar o SQLite", exception);
            }
        };
    }
}
