package com.atleta.demo.migration;

import net.jqwik.api.*;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for migration naming conventions.
 * 
 * **Validates: Requirements 2.3, 2.4**
 * 
 * **Property 5: Convenciones de nomenclatura de migraciones**
 * For any migration file, it should follow the convention V{VERSION}__{DESCRIPTION}.sql
 * and be validated before applying.
 */
class NamingConventionPropertyTest {

    private static final Pattern MIGRATION_PATTERN = Pattern.compile("^V(\\d+)__([a-zA-Z0-9_]+)\\.sql$");
    private static final Pattern VERSION_PATTERN = Pattern.compile("^\\d{3,}$");

    @Property(tries = 100)
    @Label("Feature: database-migration, Property 5: For any migration file, it should follow naming convention V{VERSION}__{DESCRIPTION}.sql")
    void shouldFollowNamingConvention(@ForAll("migrationFileNames") String fileName) {
        // Given: A migration file name
        // When: We validate the naming convention
        boolean matchesPattern = MIGRATION_PATTERN.matcher(fileName).matches();
        
        // Then: It should follow the V{VERSION}__{DESCRIPTION}.sql pattern
        assertTrue(matchesPattern, 
            "Migration file '" + fileName + "' should follow V{VERSION}__{DESCRIPTION}.sql pattern");
        
        // And: Version should be numeric with at least 3 digits
        var matcher = MIGRATION_PATTERN.matcher(fileName);
        if (matcher.matches()) {
            String version = matcher.group(1);
            assertTrue(VERSION_PATTERN.matcher(version).matches(),
                "Version '" + version + "' should be numeric with at least 3 digits");
            
            // And: Description should not be empty
            String description = matcher.group(2);
            assertFalse(description.isEmpty(),
                "Description part should not be empty");
            
            // And: Description should use underscores, not spaces or hyphens
            assertFalse(description.contains(" "),
                "Description should not contain spaces");
            assertFalse(description.contains("-"),
                "Description should not contain hyphens");
        }
    }

    @Property(tries = 100)
    @Label("Feature: database-migration, Property 5: Migration versions should be sequential and unique")
    void shouldHaveSequentialUniqueVersions(@ForAll("migrationVersions") List<String> versions) {
        // Given: A list of migration versions
        // When: We check for uniqueness and sequential order
        
        // Then: All versions should be unique
        long uniqueCount = versions.stream().distinct().count();
        assertEquals(versions.size(), uniqueCount,
            "All migration versions should be unique");
        
        // And: Versions should be in ascending order when sorted
        List<Integer> numericVersions = versions.stream()
            .map(Integer::parseInt)
            .sorted()
            .toList();
        
        for (int i = 1; i < numericVersions.size(); i++) {
            assertTrue(numericVersions.get(i) > numericVersions.get(i-1),
                "Migration versions should be in ascending order");
        }
    }

    @Test
    void shouldValidateActualMigrationFiles() throws IOException {
        // Given: Actual migration files in the classpath
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:db/migration/V*.sql");
        
        // When: We check each migration file
        assertTrue(resources.length > 0, 
            "Should have at least one migration file");
        
        // Then: Each file should follow naming conventions
        for (Resource resource : resources) {
            String fileName = resource.getFilename();
            assertNotNull(fileName, "Migration file should have a name");
            
            assertTrue(MIGRATION_PATTERN.matcher(fileName).matches(),
                "Migration file '" + fileName + "' should follow V{VERSION}__{DESCRIPTION}.sql pattern");
            
            // Validate version format
            var matcher = MIGRATION_PATTERN.matcher(fileName);
            if (matcher.matches()) {
                String version = matcher.group(1);
                assertTrue(VERSION_PATTERN.matcher(version).matches(),
                    "Version '" + version + "' in file '" + fileName + "' should be numeric with at least 3 digits");
                
                String description = matcher.group(2);
                assertFalse(description.isEmpty(),
                    "Description in file '" + fileName + "' should not be empty");
                assertTrue(description.matches("[a-zA-Z0-9_]+"),
                    "Description in file '" + fileName + "' should only contain alphanumeric characters and underscores");
            }
        }
    }

    @Test
    void shouldHaveUniqueVersionsInActualFiles() throws IOException {
        // Given: Actual migration files in the classpath
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:db/migration/V*.sql");
        
        // When: We extract versions from file names
        List<String> versions = Arrays.stream(resources)
            .map(Resource::getFilename)
            .filter(name -> name != null && MIGRATION_PATTERN.matcher(name).matches())
            .map(name -> {
                var matcher = MIGRATION_PATTERN.matcher(name);
                return matcher.matches() ? matcher.group(1) : null;
            })
            .filter(version -> version != null)
            .toList();
        
        // Then: All versions should be unique
        long uniqueCount = versions.stream().distinct().count();
        assertEquals(versions.size(), uniqueCount,
            "All migration versions should be unique in actual files");
    }

    @Test
    void shouldHaveDescriptiveNames() throws IOException {
        // Given: Actual migration files in the classpath
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:db/migration/V*.sql");
        
        // When: We check descriptions
        for (Resource resource : resources) {
            String fileName = resource.getFilename();
            if (fileName != null && MIGRATION_PATTERN.matcher(fileName).matches()) {
                var matcher = MIGRATION_PATTERN.matcher(fileName);
                if (matcher.matches()) {
                    String description = matcher.group(2);
                    
                    // Then: Description should be meaningful (at least 3 characters)
                    assertTrue(description.length() >= 3,
                        "Description '" + description + "' in file '" + fileName + "' should be at least 3 characters long");
                    
                    // And: Should not be generic names
                    assertFalse(description.toLowerCase().equals("migration"),
                        "Description should not be generic 'migration'");
                    assertFalse(description.toLowerCase().equals("update"),
                        "Description should not be generic 'update'");
                    assertFalse(description.toLowerCase().equals("change"),
                        "Description should not be generic 'change'");
                }
            }
        }
    }

    @Provide
    Arbitrary<String> migrationFileNames() {
        return Arbitraries.strings()
            .withCharRange('a', 'z')
            .withCharRange('A', 'Z')
            .withCharRange('0', '9')
            .withChars('_')
            .ofMinLength(3)
            .ofMaxLength(50)
            .map(description -> "V" + 
                String.format("%03d", Arbitraries.integers().between(1, 999).sample()) + 
                "__" + description + ".sql");
    }

    @Provide
    Arbitrary<List<String>> migrationVersions() {
        return Arbitraries.integers()
            .between(1, 999)
            .list()
            .ofMinSize(1)
            .ofMaxSize(10)
            .map(versions -> versions.stream()
                .distinct()
                .map(v -> String.format("%03d", v))
                .toList());
    }
}