package com.project.expensemanager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

// Test-only value for JWT secret
@SpringBootTest(properties = {
        "jwt.secret=OM23uVI0t2bUUqbtD5ommci1LkEsMQRVJJ6CvLs4iSvbCWPUInDrVi3YxX2W5YXc"
})
// Start a temporary Postgresql container
@Testcontainers
class ExpenseManagerApplicationTests {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Test
    void contextLoads() {
    }
}
