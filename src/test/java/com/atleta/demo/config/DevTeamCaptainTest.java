package com.atleta.demo.config;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "app.dev-seed.password=cap02-test-password")
@ActiveProfiles("test")
@Transactional
class DevTeamCaptainTest {
    @Autowired AutowireCapableBeanFactory beans;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entities;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void rejectsReseedingWhenTheDesignatedPlayerIsNoLongerCaptain(boolean hasSuccessor) {
        var seed = beans.createBean(DevDataInitializer.class);
        seed.run(null);
        entities.flush();
        jdbc.update("UPDATE teams SET archived=TRUE WHERE nombre='Atleta Dev FC'");
        jdbc.update("UPDATE team_members SET activo=FALSE WHERE rol='CAPITAN' AND team_id IN "
                + "(SELECT id FROM teams WHERE nombre='Atleta Dev FC')");
        if (hasSuccessor) {
            jdbc.update("""
                    UPDATE team_members SET rol='CAPITAN' WHERE id = (
                        SELECT min(m.id) FROM team_members m JOIN teams t ON t.id=m.team_id
                        WHERE t.nombre='Atleta Dev FC' AND m.activo AND m.rol='JUGADOR'
                    )
                    """);
        }
        entities.clear();
        assertThatThrownBy(() -> seed.run(null)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("capitanía");
        assertThat(jdbc.queryForObject("SELECT archived FROM teams WHERE nombre='Atleta Dev FC'", Boolean.class)).isTrue();
    }
}
