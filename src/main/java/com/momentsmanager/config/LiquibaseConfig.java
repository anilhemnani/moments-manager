package com.momentsmanager.config;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Liquibase configuration to ensure database schema is created before JPA/Hibernate initialization.
 * This is critical for Spring Boot 4.x to prevent "Table not found" errors.
 */
@Configuration
public class LiquibaseConfig {

    @Value("${spring.liquibase.change-log}")
    private String changeLog;

    @Value("${spring.liquibase.drop-first:false}")
    private boolean dropFirst;

    /**
     * Creates Liquibase bean with explicit configuration.
     * This bean will be created before EntityManagerFactory, ensuring tables exist.
     *
     * @param dataSource the application datasource
     * @return configured SpringLiquibase instance
     */
    @Bean
    public SpringLiquibase liquibase(DataSource dataSource) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog(changeLog);
        liquibase.setDropFirst(dropFirst);
        liquibase.setShouldRun(true);
        return liquibase;
    }
}

