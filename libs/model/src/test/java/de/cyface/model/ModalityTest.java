/*
 * Copyright 2026 Cyface GmbH
 *
 * This file is part of the Serialization.
 *
 * The Serialization is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The Serialization is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Serialization. If not, see <http://www.gnu.org/licenses/>.
 */
package de.cyface.model;

import static de.cyface.model.Modality.BICYCLE;
import static de.cyface.model.Modality.CARGO_BIKE;
import static de.cyface.model.Modality.EBIKE;
import static de.cyface.model.Modality.ECARGO_BIKE;
import static de.cyface.model.Modality.UNKNOWN;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for {@link Modality}, covering database identifier round-trips, new modality types, and
 * backwards-compatible read aliases.
 *
 * @author Klemens Muthmann
 * @version 1.0.0
 * @since 2.2.0
 */
public class ModalityTest {

    // ── database identifier round-trips ──────────────────────────────────────

    @ParameterizedTest(name = "{0} round-trips via forDatabaseIdentifier")
    @CsvSource({
        "BICYCLE,   BICYCLE",
        "EBIKE,     EBike",
        "CARGO_BIKE,CargoBike",
        "ECARGO_BIKE,ECargoBike",
        "CAR,       CAR",
        "MOTORBIKE, MOTORBIKE",
        "BUS,       BUS",
        "TRAIN,     TRAIN",
        "WALKING,   WALKING",
        "UNKNOWN,   UNKNOWN",
    })
    @DisplayName("forDatabaseIdentifier resolves every primary database identifier")
    void testForDatabaseIdentifier_primaryIdentifiers(final String enumName, final String dbId) {
        final Modality modality = Modality.valueOf(enumName.trim());
        assertThat(modality.getDatabaseIdentifier(), is(equalTo(dbId.trim())));
        assertThat(Modality.forDatabaseIdentifier(dbId.trim()), is(equalTo(modality)));
    }

    // ── new modality types ────────────────────────────────────────────────────

    @Test
    @DisplayName("CARGO_BIKE has database identifier 'CargoBike'")
    void testCargoBikeDatabaseIdentifier() {
        assertThat(CARGO_BIKE.getDatabaseIdentifier(), is(equalTo("CargoBike")));
    }

    @Test
    @DisplayName("ECARGO_BIKE has database identifier 'ECargoBike'")
    void testECargoBikeDatabaseIdentifier() {
        assertThat(ECARGO_BIKE.getDatabaseIdentifier(), is(equalTo("ECargoBike")));
    }

    // ── read aliases ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Legacy alias 'Bike' resolves to BICYCLE")
    void testAlias_bike() {
        assertThat(Modality.forDatabaseIdentifier("Bike"), is(equalTo(BICYCLE)));
    }

    @Test
    @DisplayName("Legacy alias 'Other' resolves to UNKNOWN")
    void testAlias_other() {
        assertThat(Modality.forDatabaseIdentifier("Other"), is(equalTo(UNKNOWN)));
    }

    @Test
    @DisplayName("Aliases do not affect getDatabaseIdentifier of the canonical constant")
    void testAlias_doesNotChangeCanonicalIdentifier() {
        assertThat(BICYCLE.getDatabaseIdentifier(), is(equalTo("BICYCLE")));
        assertThat(UNKNOWN.getDatabaseIdentifier(), is(equalTo("UNKNOWN")));
    }

    // ── EBIKE identifier ─────────────────────────────────────────────────────

    @Test
    @DisplayName("EBIKE database identifier is 'EBike' (differs from enum constant name)")
    void testEBikeDatabaseIdentifier() {
        assertThat(EBIKE.getDatabaseIdentifier(), is(equalTo("EBike")));
        assertThat(Modality.forDatabaseIdentifier("EBike"), is(equalTo(EBIKE)));
    }

    // ── unknown identifier ────────────────────────────────────────────────────

    @Test
    @DisplayName("forDatabaseIdentifier throws for an unknown identifier")
    void testForDatabaseIdentifier_unknownIdentifier() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Modality.forDatabaseIdentifier("Spaceship")
        );
    }
}