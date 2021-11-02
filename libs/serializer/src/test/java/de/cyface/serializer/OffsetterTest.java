/*
 * Copyright 2021 Cyface GmbH
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
package de.cyface.serializer;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the inner workings of the {@link Offsetter}.
 *
 * @author Armin Schnabel
 * @version 1.0.0
 * @since 5.0.0
 */
public class OffsetterTest {

    private Offsetter oocut;

    @BeforeEach
    public void setUp() {
        oocut = new Offsetter();
    }

    @Test
    public void testOffset_forFirstNumber() {
        // Arrange

        // Act
        final long result = oocut.offset(1234567890123L);

        // Assert
        assertThat(result, is(equalTo(1234567890123L)));
    }

    //@Test
    public void testOffset_forSubsequentNumbers() {
        // Arrange
        final long[] numbers = new long[] {1234567890123L, 1234567890223L, 1234567890123L};

        // Act
        final long result1 = oocut.offset(numbers[0]);
        final long result2 = oocut.offset(numbers[1]);
        final long result3 = oocut.offset(numbers[2]);

        // Assert
        assertThat(result1, is(equalTo(numbers[0])));
        assertThat(result2, is(equalTo(numbers[1] - numbers[0])));
        assertThat(result3, is(equalTo(numbers[2] - numbers[1])));
    }
}