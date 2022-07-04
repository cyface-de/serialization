/*
 * Copyright 2022 Cyface GmbH
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

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests the inner workings of the {@link Json} class.
 *
 * @author Armin Schnabel
 * @since 2.2.1
 * @version 1.0.0
 */
public class JsonTest {

    /**
     * This test ensures that OSM tags which contain a quote do not break generate an unreadable Json String [DAT-1313].
     */
    @Test
    public void testJsonKeyValue_forValueWithQuotes() {
        // Arrange
        final var key = "name";
        // This tag value was found in the OSM way 207439390, OSM way state: 2022-05-14.
        //noinspection SpellCheckingInspection
        final var value = "Gewerbegebiet \"Bahnhofstraße\"";

        // Act
        final var result = Json.jsonKeyValue(key, value);

        // Assert
        //noinspection SpellCheckingInspection
        final var expected = "\"name\":\"Gewerbegebiet 'Bahnhofstraße'\"";
        assertThat(result.getStringValue(), is(equalTo(expected)));
    }
}
