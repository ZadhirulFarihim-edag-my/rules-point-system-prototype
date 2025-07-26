package com.edag.swd.my.gamification.config;

import com.edag.swd.my.gamification.repository.GroupRepository;
import com.edag.swd.my.gamification.repository.PersonRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.sql.Connection;

@Configuration
public class DatabaseInitializer {

    public DatabaseInitializer(Environment env) {
    }

    /**
     * This bean is a fallback mechanism in case the automatic SQL script execution fails.
     * It will check if the database is empty and execute the data.sql script manually if needed.
     */
    @Bean
    public CommandLineRunner initDatabase(GroupRepository groupRepository, PersonRepository personRepository, DataSource dataSource) {
        return args -> {
            // Check if database is already populated
            if (groupRepository.count() == 0) {
                System.out.println("Database is empty. Executing SQL initialization script manually...");

                try (Connection connection = dataSource.getConnection()) {
                    ScriptUtils.executeSqlScript(connection,
                            new org.springframework.core.io.ClassPathResource("data.sql"));
                    System.out.println("Database initialized successfully with SQL script");
                } catch (Exception e) {
                    System.err.println("Error initializing database with SQL script: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.out.println("Database already contains data. Skipping initialization.");
            }
        };
    }
}