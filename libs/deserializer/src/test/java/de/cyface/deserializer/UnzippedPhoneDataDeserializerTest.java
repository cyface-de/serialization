/*
 * Copyright 2020-2021 Cyface GmbH
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
package de.cyface.deserializer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

import de.cyface.deserializer.exceptions.InvalidLifecycleEvents;
import de.cyface.deserializer.exceptions.NoSuchMeasurement;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.cyface.model.MeasurementIdentifier;

/**
 * Tests that deserialization of phone data exports works as expected.
 *
 * @author Klemens Muthmann
 */
public class UnzippedPhoneDataDeserializerTest {

    @Test
    @DisplayName("Test on some unzipped example data")
    void test_unzipped() throws InvalidLifecycleEvents, URISyntaxException, NoSuchMeasurement {
        // Arrange
        final var folder = "/phone-data-export/unzipped/";
        final var mid = 6;
        final var databaseLocation = path(folder + "measures");
        final var accelerationsLocation = path(folder + mid + ".cyfa");
        final var directionsLocation = path(folder + mid + ".cyfd");
        final var rotationsLocation = path(folder + mid + ".cyfr");

        final var oocut = new UnzippedPhoneDataDeserializer("test-user", databaseLocation,
                List.of(accelerationsLocation), List.of(rotationsLocation), List.of(directionsLocation));

        // Act
        oocut.setMeasurementNumber(mid);
        final var result = oocut.read();

        // Assert
        assertThat(result, hasProperty("metaData", hasProperty("identifier",
                is(equalTo(new MeasurementIdentifier("65a6f5dc-459d-4634-a759-197e224f76cf", mid))))));
        assertThat(result, hasProperty("tracks", hasSize(2)));
        assertThat(result.getTracks().get(0), hasProperty("locationRecords", hasSize(4)));
        assertThat(result.getTracks().get(0), hasProperty("accelerations", is(not(empty()))));
        assertThat(result.getTracks().get(0), hasProperty("directions", is(not(empty()))));
        assertThat(result.getTracks().get(0), hasProperty("rotations", is(not(empty()))));
        assertThat(result.getTracks().get(1), hasProperty("locationRecords", hasSize(5)));
        assertThat(result.getTracks().get(1), hasProperty("accelerations", is(not(empty()))));
        assertThat(result.getTracks().get(1), hasProperty("directions", is(not(empty()))));
        assertThat(result.getTracks().get(1), hasProperty("rotations", is(not(empty()))));
    }

    @Test
    @DisplayName("Test on some zipped example data")
    void test_zipped() throws URISyntaxException, InvalidLifecycleEvents, IOException, NoSuchMeasurement {
        // Arrange
        final var folder = "/phone-data-export/zipped/";
        final var mid = 6;
        final var suffix = "_2021-12-06_14-38_60b01824-302a-41b4-8f93-b5b9a9fcb6ba.zip";
        final var databaseLocation = path(folder + "cyface-databases" + suffix);
        final var accelerations = path(folder + "cyface-accelerations" + suffix);
        final var directions = path(folder + "cyface-directions" + suffix);
        final var rotations = path(folder + "cyface-rotations" + suffix);
        final var oocut = new ZippedPhoneDataDeserializer("test-user", databaseLocation, accelerations, directions,
                rotations);

        // Act
        oocut.setMeasurementNumber(mid);
        final var result = oocut.read();

        // Assert
        assertThat(result, hasProperty("tracks", is(not(empty()))));
    }

    private Path path(final String fileName) throws URISyntaxException {
        final var resource = this.getClass().getResource(fileName);
        Validate.notNull(resource);
        return Path.of(resource.toURI());
    }
}
