package io.settleup.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@Slf4j
public class DataSourceConfig {

    @Value("${DB_URL:${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/settleup}}")
    private String rawDbUrl;

    @Value("${DB_USERNAME:${SPRING_DATASOURCE_USERNAME:settleup}}")
    private String username;

    @Value("${DB_PASSWORD:${SPRING_DATASOURCE_PASSWORD:settleup_secret}}")
    private String password;

    @Bean
    @Primary
    public DataSourceProperties dataSourceProperties() {
        DataSourceProperties properties = new DataSourceProperties();
        String url = rawDbUrl;

        // Automatically convert Render/Railway connectionString format to valid JDBC syntax
        if (url.startsWith("postgres://")) {
            url = "jdbc:postgresql://" + url.substring("postgres://".length());
            log.info("Converted postgres:// connection string to JDBC format: {}", url.replaceAll(":.*@", ":***@"));
        } else if (url.startsWith("postgresql://") && !url.startsWith("jdbc:postgresql://")) {
            url = "jdbc:postgresql://" + url.substring("postgresql://".length());
            log.info("Converted postgresql:// connection string to JDBC format: {}", url.replaceAll(":.*@", ":***@"));
        }

        properties.setUrl(url);
        properties.setUsername(username);
        properties.setPassword(password);
        properties.setDriverClassName("org.postgresql.Driver");
        return properties;
    }
}
