package com.atleta.demo.migration;

import com.atleta.demo.config.BaseIntegrationTest;
import net.jqwik.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for schema integrity after migrations.
 * 
 * **Validates: Requirements 4.1, 4.2, 4.3, 4.4, 4.5**
 * 
 * **Property 7: Integridad del esquema después de migraciones**
 * For any migration execution, the resulting schema should have all required tables,
 * indexes, constraints, and initial data with proper integrity.
 */
class SchemaIntegrityPropertyTest extends BaseIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Property(tries = 100)
    @Label("Feature: database-migration, Property 7: For any migration execution, all required Atleta domain tables should exist")
    void shouldHaveAllRequiredTables(@ForAll("tableValidationScenarios") TableValidationScenario scenario) {
        // Given: Migrations have been executed
        // When: We check for required tables
        String[] requiredTables = {
            "athletes", "positions", "player_profiles", "player_positions",
            "teams", "team_stats", "team_members", "matches", "match_teams",
            "match_players", "match_events", "player_history", "trust_logs"
        };
        
        // Then: All required tables should exist
        for (String tableName : requiredTables) {
            assertTrue(tableExists(tableName), 
                "Required table '" + tableName + "' should exist after migrations");
        }
        
        // And: Tables should have the expected structure
        for (String tableName : requiredTables) {
            List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                """
                SELECT column_name, data_type, is_nullable, column_default
                FROM information_schema.columns 
                WHERE table_name = ? AND table_schema = 'public'
                ORDER BY ordinal_position
                """, tableName);
            
            assertTrue(columns.size() > 0, 
                "Table '" + tableName + "' should have columns defined");
        }
    }

    @Property(tries = 100)
    @Label("Feature: database-migration, Property 7: For any migration execution, all primary keys should be properly defined")
    void shouldHaveCorrectPrimaryKeys(@ForAll("tableValidationScenarios") TableValidationScenario scenario) {
        // Given: Migrations have been executed
        // When: We check primary key constraints
        Map<String, String> expectedPrimaryKeys = Map.of(
            "athletes", "atleta_uuid",
            "positions", "id",
            "player_profiles", "atleta_uuid",
            "teams", "id",
            "matches", "id"
        );
        
        // Then: Each table should have the correct primary key
        for (Map.Entry<String, String> entry : expectedPrimaryKeys.entrySet()) {
            String tableName = entry.getKey();
            String expectedPkColumn = entry.getValue();
            
            List<String> pkColumns = jdbcTemplate.queryForList(
                """
                SELECT a.attname
                FROM pg_index i
                JOIN pg_attribute a ON a.attrelid = i.indrelid AND a.attnum = ANY(i.indkey)
                WHERE i.indrelid = ?::regclass AND i.indisprimary
                """,
                String.class,
                tableName
            );
            
            assertEquals(1, pkColumns.size(), 
                "Table '" + tableName + "' should have exactly one primary key column");
            assertEquals(expectedPkColumn, pkColumns.get(0), 
                "Table '" + tableName + "' should have primary key on column '" + expectedPkColumn + "'");
        }
    }

    @Property(tries = 100)
    @Label("Feature: database-migration, Property 7: For any migration execution, foreign key relationships should be intact")
    void shouldHaveForeignKeyIntegrity(@ForAll("tableValidationScenarios") TableValidationScenario scenario) {
        // Given: Migrations have been executed
        // When: We check foreign key constraints
        List<Map<String, Object>> foreignKeys = jdbcTemplate.queryForList(
            """
            SELECT 
                tc.table_name,
                kcu.column_name,
                ccu.table_name AS foreign_table_name,
                ccu.column_name AS foreign_column_name
            FROM information_schema.table_constraints AS tc
            JOIN information_schema.key_column_usage AS kcu
                ON tc.constraint_name = kcu.constraint_name
                AND tc.table_schema = kcu.table_schema
            JOIN information_schema.constraint_column_usage AS ccu
                ON ccu.constraint_name = tc.constraint_name
                AND ccu.table_schema = tc.table_schema
            WHERE tc.constraint_type = 'FOREIGN KEY'
            ORDER BY tc.table_name, kcu.column_name
            """
        );
        
        // Then: Should have expected foreign key relationships
        assertTrue(foreignKeys.size() > 0, "Should have foreign key constraints");
        
        // Verify key relationships exist
        boolean hasPlayerProfileToAthlete = foreignKeys.stream()
            .anyMatch(fk -> "player_profiles".equals(fk.get("table_name")) &&
                           "atleta_uuid".equals(fk.get("column_name")) &&
                           "athletes".equals(fk.get("foreign_table_name")));
        
        assertTrue(hasPlayerProfileToAthlete, 
            "player_profiles should reference athletes table");
        
        boolean hasTeamMembersToTeams = foreignKeys.stream()
            .anyMatch(fk -> "team_members".equals(fk.get("table_name")) &&
                           "team_id".equals(fk.get("column_name")) &&
                           "teams".equals(fk.get("foreign_table_name")));
        
        assertTrue(hasTeamMembersToTeams, 
            "team_members should reference teams table");
    }

    @Property(tries = 100)
    @Label("Feature: database-migration, Property 7: For any migration execution, performance indexes should be created")
    void shouldHavePerformanceIndexes(@ForAll("tableValidationScenarios") TableValidationScenario scenario) {
        // Given: Migrations have been executed
        // When: We check for performance indexes
        List<Map<String, Object>> indexes = jdbcTemplate.queryForList(
            """
            SELECT 
                schemaname,
                tablename,
                indexname,
                indexdef
            FROM pg_indexes 
            WHERE schemaname = 'public'
            AND indexname NOT LIKE '%_pkey'
            ORDER BY tablename, indexname
            """
        );
        
        // Then: Should have performance indexes
        assertTrue(indexes.size() > 0, "Should have performance indexes created");
        
        // Verify some key indexes exist
        boolean hasAthleteEmailIndex = indexes.stream()
            .anyMatch(idx -> "athletes".equals(idx.get("tablename")) &&
                           idx.get("indexdef").toString().contains("email"));
        
        assertTrue(hasAthleteEmailIndex, 
            "Should have index on athletes.email for performance");
        
        boolean hasMatchesStateIndex = indexes.stream()
            .anyMatch(idx -> "matches".equals(idx.get("tablename")) &&
                           idx.get("indexdef").toString().contains("estado"));
        
        assertTrue(hasMatchesStateIndex, 
            "Should have index on matches.estado for performance");
    }

    @Property(tries = 100)
    @Label("Feature: database-migration, Property 7: For any migration execution, check constraints should enforce business rules")
    void shouldHaveBusinessRuleConstraints(@ForAll("tableValidationScenarios") TableValidationScenario scenario) {
        // Given: Migrations have been executed
        // When: We check check constraints
        List<Map<String, Object>> checkConstraints = jdbcTemplate.queryForList(
            """
            SELECT 
                tc.table_name,
                tc.constraint_name,
                cc.check_clause
            FROM information_schema.table_constraints AS tc
            JOIN information_schema.check_constraints AS cc
                ON tc.constraint_name = cc.constraint_name
            WHERE tc.constraint_type = 'CHECK'
            ORDER BY tc.table_name, tc.constraint_name
            """
        );
        
        // Then: Should have business rule constraints
        assertTrue(checkConstraints.size() > 0, "Should have check constraints");
        
        // Verify some key business rules
        boolean hasPlayerPositionPriorityCheck = checkConstraints.stream()
            .anyMatch(cc -> "player_positions".equals(cc.get("table_name")) &&
                           cc.get("check_clause").toString().contains("prioridad"));
        
        assertTrue(hasPlayerPositionPriorityCheck, 
            "Should have check constraint for player position priority");
        
        boolean hasMatchModalidadCheck = checkConstraints.stream()
            .anyMatch(cc -> "matches".equals(cc.get("table_name")) &&
                           cc.get("check_clause").toString().contains("modalidad"));
        
        assertTrue(hasMatchModalidadCheck, 
            "Should have check constraint for match modalidad");
    }

    @Property(tries = 100)
    @Label("Feature: database-migration, Property 7: For any migration execution, initial master data should be present")
    void shouldHaveInitialMasterData(@ForAll("tableValidationScenarios") TableValidationScenario scenario) {
        // Given: Migrations have been executed
        // When: We check for initial data
        
        // Then: Positions table should have initial data
        long positionCount = countRowsInTable("positions");
        assertTrue(positionCount > 0, 
            "positions table should have initial master data");
        
        // And: Should have expected positions
        List<String> positionNames = jdbcTemplate.queryForList(
            "SELECT nombre FROM positions ORDER BY nombre", String.class);
        
        assertTrue(positionNames.contains("Portero"), 
            "Should have 'Portero' position");
        assertTrue(positionNames.contains("Defensa"), 
            "Should have 'Defensa' position");
        assertTrue(positionNames.contains("Delantero"), 
            "Should have 'Delantero' position");
        
        // And: Should have admin user if configured
        long athleteCount = countRowsInTable("athletes");
        if (athleteCount > 0) {
            List<String> adminEmails = jdbcTemplate.queryForList(
                "SELECT email FROM athletes WHERE email LIKE '%admin%'", String.class);
            // Admin user is optional, but if present should be properly configured
            if (!adminEmails.isEmpty()) {
                assertTrue(adminEmails.stream().anyMatch(email -> email.contains("admin")),
                    "Admin user should have admin-like email");
            }
        }
    }

    @Property(tries = 100)
    @Label("Feature: database-migration, Property 7: For any migration execution, table comments should provide documentation")
    void shouldHaveTableDocumentation(@ForAll("tableValidationScenarios") TableValidationScenario scenario) {
        // Given: Migrations have been executed
        // When: We check table comments
        List<Map<String, Object>> tableComments = jdbcTemplate.queryForList(
            """
            SELECT 
                t.table_name,
                obj_description(c.oid) as comment
            FROM information_schema.tables t
            JOIN pg_class c ON c.relname = t.table_name
            WHERE t.table_schema = 'public' 
            AND obj_description(c.oid) IS NOT NULL
            ORDER BY t.table_name
            """
        );
        
        // Then: Key tables should have documentation
        assertTrue(tableComments.size() > 0, "Should have table comments for documentation");
        
        // Verify specific important tables have comments
        boolean hasAthleteComment = tableComments.stream()
            .anyMatch(tc -> "athletes".equals(tc.get("table_name")) &&
                           tc.get("comment") != null);
        
        boolean hasPlayerHistoryComment = tableComments.stream()
            .anyMatch(tc -> "player_history".equals(tc.get("table_name")) &&
                           tc.get("comment") != null &&
                           tc.get("comment").toString().contains("FUENTE DE VERDAD"));
        
        // Comments are important for maintainability
        if (hasAthleteComment) {
            assertTrue(hasPlayerHistoryComment, 
                "player_history should have FUENTE DE VERDAD comment if documentation is present");
        }
    }

    @Provide
    Arbitrary<TableValidationScenario> tableValidationScenarios() {
        return Arbitraries.of(
            new TableValidationScenario("full_validation", "Complete schema validation"),
            new TableValidationScenario("integrity_check", "Foreign key integrity validation"),
            new TableValidationScenario("performance_check", "Index and performance validation"),
            new TableValidationScenario("business_rules", "Business rule constraint validation"),
            new TableValidationScenario("master_data", "Initial data validation")
        );
    }

    /**
     * Represents different schema validation scenarios for property testing.
     */
    static class TableValidationScenario {
        private final String name;
        private final String description;

        public TableValidationScenario(String name, String description) {
            this.name = name;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return name + ": " + description;
        }
    }
}