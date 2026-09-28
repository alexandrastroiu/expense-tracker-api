package com.project.expensemanager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

// Test-only value for JWT secret
@SpringBootTest(properties = {
        "jwt.secret=OM23uVI0t2bUUqbtD5ommci1LkEsMQRVJJ6CvLs4iSvbCWPUInDrVi3YxX2W5YXc"
})
// Start a temporary Postgresql container, load the schema and run the SQL script for sample data
@Testcontainers
class ExpenseManagerApplicationTests {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17")
                    .withCopyFileToContainer(
                            MountableFile.forClasspathResource(
                                    "database/database_schema.sql"),
                            "/docker-entrypoint-initdb.d/01-schema.sql")
                    .withCopyFileToContainer(
                            MountableFile.forClasspathResource(
                                    "database/sample_data.sql"),
                            "/docker-entrypoint-initdb.d/02-data.sql");

    @Test
    void contextLoads() {
    }
}
