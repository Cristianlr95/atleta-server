package com.atleta.demo.migration;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.math.BigDecimal;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RatingMaximumMigrationTest {
    @Test
    void capsExistingRatingsAndRejectsNewValuesAboveOneHundred() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:rating_max;MODE=PostgreSQL")) {
            try (var statement = connection.createStatement()) {
                statement.execute("CREATE TABLE player_ratings (id INT PRIMARY KEY, current_rating DECIMAL(5,2) NOT NULL)");
                statement.execute("INSERT INTO player_ratings VALUES (1, 120.50), (2, 99.50), (3, 100.00)");
            }
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V028__cap_current_ratings_at_100.sql"));
            try (var statement = connection.createStatement();
                 var rows = statement.executeQuery("SELECT current_rating FROM player_ratings ORDER BY id")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getBigDecimal(1)).isEqualByComparingTo(BigDecimal.valueOf(100));
                assertThat(rows.next()).isTrue();
                assertThat(rows.getBigDecimal(1)).isEqualByComparingTo(new BigDecimal("99.50"));
                assertThat(rows.next()).isTrue();
                assertThat(rows.getBigDecimal(1)).isEqualByComparingTo(BigDecimal.valueOf(100));
            }
            assertThatThrownBy(() -> {
                try (var statement = connection.createStatement()) {
                    statement.execute("UPDATE player_ratings SET current_rating = 100.01 WHERE id = 2");
                }
            }).isInstanceOf(SQLException.class);
        }
    }
}
