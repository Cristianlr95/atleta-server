package com.atleta.demo.dto.request;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UpdateTeamRequestTest {
    private final JsonMapper mapper = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    @Test
    void distinguishesMissingNullAndBlankNames() {
        assertInvalid("{}", "Falta el campo nombre");
        assertInvalid("{\"nombre\":null}", "El nombre no puede ser nulo");
        assertInvalid("{\"nombre\":\"\\u00a0\\u2003\"}", "El nombre no puede estar vacío");
    }

    @Test
    void acceptsUnicodeAndTrimsOnlyTheSurroundingWhitespace() throws Exception {
        var request = mapper.readValue("{\"nombre\":\"\\u00a0Atlética  Ñandú ⚽\\u2003\"}", UpdateTeamRequest.class);

        assertThat(request.nombre()).isEqualTo("Atlética  Ñandú ⚽");
    }

    @Test
    void rejectsUnknownFieldsAndNonTextNamesDespiteMapperDefaults() {
        assertInvalid("{\"nombre\":\"Nuevo FC\",\"archived\":false}", "Solo se admite el campo nombre");
        assertInvalid("{\"nombre\":42}", "El nombre debe ser texto");
    }

    @Test
    void rejectsControlCharactersAndInvalidSurrogates() {
        assertInvalid("{\"nombre\":\"A\\u0000B\"}", "texto Unicode válido");
        assertInvalid("{\"nombre\":\"A\\ud800B\"}", "texto Unicode válido");
    }

    private void assertInvalid(String input, String message) {
        assertThatThrownBy(() -> mapper.readValue(input, UpdateTeamRequest.class))
                .isInstanceOf(JsonProcessingException.class)
                .hasMessageContaining(message);
    }
}
