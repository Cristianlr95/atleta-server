package com.atleta.demo.migration;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for database configuration across different environments.
 * Validates that database configurations are properly set up for each profile.
 * 
 * Requirements tested:
 * - 1.1: Development environment configuration
 * - 1.2: Testing environment configuration  
 * - 1.3: Staging environment configuration
 * - 1.4: Production environment configuration
 */
class DatabaseConfigurationTest {

    @SpringBootTest
    @ActiveProfiles("test")
    static class TestProfileConfigurationTest {
        
        @Autowired
        private DataSource dataSource;

        @Test
        void shouldConfigureTestEnvironmentCorrectly() {
            // Given: Test profile is active
            assertNotNull(dataSource, "DataSource should be configured");
            
            // When: We check the datasource configuration
            assertTrue(dataSource instanceof HikariDataSource, 
                "Should use HikariCP as connection pool");
            
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            
            // Then: Test environment should have appropriate pool settings
            assertEquals(5, hikariDataSource.getMaximumPoolSize(), 
                "Test environment should have maximum 5 connections");
            assertEquals(2, hikariDataSource.getMinimumIdle(), 
                "Test environment should have minimum 2 idle connections");
            assertEquals(10000, hikariDataSource.getConnectionTimeout(), 
                "Test environment should have 10 second connection timeout");
        }

        @Test
        void shouldUsePostgreSQLDriver() {
            // Given: Test profile is active
            assertTrue(dataSource instanceof HikariDataSource);
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            
            // When: We check the JDBC URL
            String jdbcUrl = hikariDataSource.getJdbcUrl();
            
            // Then: Should use PostgreSQL
            assertNotNull(jdbcUrl, "JDBC URL should be configured");
            assertTrue(jdbcUrl.startsWith("jdbc:postgresql://"), 
                "Should use PostgreSQL JDBC driver");
        }

        @Test
        void shouldHaveCorrectDatabaseName() {
            // Given: Test profile is active
            assertTrue(dataSource instanceof HikariDataSource);
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            
            // When: We check the database name
            String jdbcUrl = hikariDataSource.getJdbcUrl();
            
            // Then: Should connect to test database
            assertNotNull(jdbcUrl);
            assertTrue(jdbcUrl.contains("atleta_test") || jdbcUrl.contains("test_db"), 
                "Should connect to test database");
        }

        @Test
        void shouldHaveTestCredentials() {
            // Given: Test profile is active
            assertTrue(dataSource instanceof HikariDataSource);
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            
            // When: We check the credentials
            String username = hikariDataSource.getUsername();
            
            // Then: Should use test credentials
            assertEquals("test", username, 
                "Should use 'test' as username for test environment");
        }
    }

    @SpringBootTest
    @ActiveProfiles("dev")
    static class DevProfileConfigurationTest {
        
        @Autowired
        private DataSource dataSource;

        @Test
        void shouldConfigureDevEnvironmentCorrectly() {
            // Given: Dev profile is active
            assertNotNull(dataSource, "DataSource should be configured");
            
            // When: We check the datasource configuration
            assertTrue(dataSource instanceof HikariDataSource, 
                "Should use HikariCP as connection pool");
            
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            
            // Then: Dev environment should have appropriate pool settings
            assertEquals(10, hikariDataSource.getMaximumPoolSize(), 
                "Dev environment should have maximum 10 connections");
            assertEquals(5, hikariDataSource.getMinimumIdle(), 
                "Dev environment should have minimum 5 idle connections");
            assertEquals(20000, hikariDataSource.getConnectionTimeout(), 
                "Dev environment should have 20 second connection timeout");
            assertEquals(300000, hikariDataSource.getIdleTimeout(), 
                "Dev environment should have 5 minute idle timeout");
            assertEquals(60000, hikariDataSource.getLeakDetectionThreshold(), 
                "Dev environment should have 1 minute leak detection");
        }

        @Test
        void shouldHaveDevDatabaseName() {
            // Given: Dev profile is active
            assertTrue(dataSource instanceof HikariDataSource);
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            
            // When: We check the database name
            String jdbcUrl = hikariDataSource.getJdbcUrl();
            
            // Then: Should connect to dev database
            assertNotNull(jdbcUrl);
            assertTrue(jdbcUrl.contains("atleta_dev"), 
                "Should connect to atleta_dev database");
        }
    }

    @SpringBootTest
    @ActiveProfiles("staging")
    static class StagingProfileConfigurationTest {
        
        @Autowired
        private DataSource dataSource;

        @Test
        void shouldConfigureStagingEnvironmentCorrectly() {
            // Given: Staging profile is active
            assertNotNull(dataSource, "DataSource should be configured");
            
            // When: We check the datasource configuration
            assertTrue(dataSource instanceof HikariDataSource, 
                "Should use HikariCP as connection pool");
            
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            
            // Then: Staging environment should have appropriate pool settings
            assertEquals(20, hikariDataSource.getMaximumPoolSize(), 
                "Staging environment should have maximum 20 connections");
            assertEquals(10, hikariDataSource.getMinimumIdle(), 
                "Staging environment should have minimum 10 idle connections");
            assertEquals(30000, hikariDataSource.getConnectionTimeout(), 
                "Staging environment should have 30 second connection timeout");
            assertEquals(600000, hikariDataSource.getIdleTimeout(), 
                "Staging environment should have 10 minute idle timeout");
            assertEquals(60000, hikariDataSource.getLeakDetectionThreshold(), 
                "Staging environment should have 1 minute leak detection");
        }

        @Test
        void shouldHaveConnectionTestQuery() {
            // Given: Staging profile is active
            assertTrue(dataSource instanceof HikariDataSource);
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            
            // When: We check the connection test query
            String testQuery = hikariDataSource.getConnectionTestQuery();
            
            // Then: Should have a connection test query
            assertEquals("SELECT 1", testQuery, 
                "Should have 'SELECT 1' as connection test query");
        }
    }

    @SpringBootTest
    @ActiveProfiles("prod")
    static class ProdProfileConfigurationTest {
        
        @Autowired
        private DataSource dataSource;

        @Test
        void shouldConfigureProdEnvironmentCorrectly() {
            // Given: Production profile is active
            assertNotNull(dataSource, "DataSource should be configured");
            
            // When: We check the datasource configuration
            assertTrue(dataSource instanceof HikariDataSource, 
                "Should use HikariCP as connection pool");
            
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            
            // Then: Production environment should have appropriate pool settings
            assertEquals(50, hikariDataSource.getMaximumPoolSize(), 
                "Production environment should have maximum 50 connections");
            assertEquals(20, hikariDataSource.getMinimumIdle(), 
                "Production environment should have minimum 20 idle connections");
            assertEquals(30000, hikariDataSource.getConnectionTimeout(), 
                "Production environment should have 30 second connection timeout");
            assertEquals(600000, hikariDataSource.getIdleTimeout(), 
                "Production environment should have 10 minute idle timeout");
            assertEquals(60000, hikariDataSource.getLeakDetectionThreshold(), 
                "Production environment should have 1 minute leak detection");
        }

        @Test
        void shouldHaveProductionPoolName() {
            // Given: Production profile is active
            assertTrue(dataSource instanceof HikariDataSource);
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            
            // When: We check the pool name
            String poolName = hikariDataSource.getPoolName();
            
            // Then: Should have production pool name
            assertEquals("HikariPool-Atleta-Production", poolName, 
                "Should have production-specific pool name");
        }

        @Test
        void shouldEnableMBeanRegistration() {
            // Given: Production profile is active
            assertTrue(dataSource instanceof HikariDataSource);
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            
            // When: We check MBean registration
            boolean mbeansRegistered = hikariDataSource.isRegisterMbeans();
            
            // Then: Should enable MBean registration for monitoring
            assertTrue(mbeansRegistered, 
                "Production should enable MBean registration for monitoring");
        }

        @Test
        void shouldRequireSSLInJdbcUrl() {
            // Given: Production profile is active
            assertTrue(dataSource instanceof HikariDataSource);
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            
            // When: We check the JDBC URL
            String jdbcUrl = hikariDataSource.getJdbcUrl();
            
            // Then: Should require SSL
            assertNotNull(jdbcUrl);
            assertTrue(jdbcUrl.contains("ssl=true") && jdbcUrl.contains("sslmode=require"), 
                "Production should require SSL connections");
        }
    }
}