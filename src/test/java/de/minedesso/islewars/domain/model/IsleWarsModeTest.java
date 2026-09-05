package de.minedesso.islewars.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IsleWarsModeTest {
    @ParameterizedTest
    @CsvSource({
            "4x1, FOUR_BY_ONE",
            "4x2, FOUR_BY_TWO",
            "4x3, FOUR_BY_THREE"
    })
    void mapsSupportedApiModes(String apiValue, IsleWarsMode expected) {
        assertEquals(expected, IsleWarsMode.fromApiValue(apiValue));
    }

    @Test
    void rejectsUnknownMode() {
        assertThrows(IllegalArgumentException.class, () -> IsleWarsMode.fromApiValue("2x4"));
    }
}
