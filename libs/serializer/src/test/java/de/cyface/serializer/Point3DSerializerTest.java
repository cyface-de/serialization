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
package de.cyface.serializer;

import static de.cyface.protos.model.Measurement.parseFrom;
import static de.cyface.serializer.model.Point3DType.ACCELERATION;
import static de.cyface.serializer.model.Point3DType.DIRECTION;
import static de.cyface.serializer.model.Point3DType.ROTATION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.protobuf.ByteString;

import de.cyface.model.Point3D;
import de.cyface.model.Point3DImpl;
import de.cyface.protos.model.MeasurementBytes;

/**
 * Tests the inner workings of the serialization of the point 3D serializer.
 *
 * @author Armin Schnabel
 * @version 1.0.0
 * @since 2.2.2
 */
public class Point3DSerializerTest {

    private final static short PERSISTENCE_FILE_FORMAT_VERSION = 3;

    /**
     * This test generates random sensor values within the supported value range.
     * <p>
     * This test only ensures the serialization succeeds with such values, it cannot check the bytes produced as
     * Protobuf does not guarantee that the byte order (and size I think) is always the same.
     * <p>
     * This test cannot deserialize the serialized bytes to check the values as this would lead to a circular dependency
     * to `libs/deserializer` but those tests can be found in {@code BinaryFormatDeserializerTest} in
     * `libs/deserializer`.
     */
    @DisplayName("Happy Path test for the serialization of 3d points.")
    @Test
    void testSerialize() throws IOException {
        // Arrange
        final List<Point3D> accelerations1 = new ArrayList<>();
        final List<Point3D> rotations1 = new ArrayList<>();
        final List<Point3D> directions1 = new ArrayList<>();
        final List<Point3D> accelerations2 = new ArrayList<>();
        final List<Point3D> rotations2 = new ArrayList<>();
        final List<Point3D> directions2 = new ArrayList<>();
        final var startTs = 1660000000L;
        final var maxAcceleration = 16.0; // m/s²
        final var maxRotation = 2 * 34.906585; // rad/s
        final var maxDirection = 4911.994; // µT
        for (int i = 0; i < 200; i++) {
            final var sign = i % 2 == 0 ? -1 : 1;
            final var acceleration = new Point3DImpl(sign * Math.random() * maxAcceleration,
                    sign * Math.random() * maxAcceleration,
                    sign * Math.random() * maxAcceleration, startTs + i * 10L);
            final var rotation = new Point3DImpl(sign * Math.random() * maxRotation, sign * Math.random() * maxRotation,
                    sign * Math.random() * maxRotation, startTs + i * 10L);
            final var direction = new Point3DImpl(sign * Math.random() * maxDirection,
                    sign * Math.random() * maxDirection,
                    sign * Math.random() * maxDirection, startTs + i * 10L);
            if (i < 100) {
                accelerations1.add(acceleration);
                rotations1.add(rotation);
                directions1.add(direction);
            } else {
                accelerations2.add(acceleration);
                rotations2.add(rotation);
                directions2.add(direction);
            }
        }

        // Act - Serialize
        // Write two sensor data batches in the `cyfa/r/d` format as on the mobile devices (e.g. Point3DFile.append)
        final var cyfa = new ByteArrayOutputStream();
        final var cyfd = new ByteArrayOutputStream();
        final var cyfr = new ByteArrayOutputStream();
        final var accelerationsBatch1 = Point3DSerializer.serialize(accelerations1, ACCELERATION);
        final var rotationsBatch1 = Point3DSerializer.serialize(rotations1, ROTATION);
        final var directionsBatch1 = Point3DSerializer.serialize(directions1, DIRECTION);
        final var accelerationsBatch2 = Point3DSerializer.serialize(accelerations2, ACCELERATION);
        final var rotationsBatch2 = Point3DSerializer.serialize(rotations2, ROTATION);
        final var directionsBatch2 = Point3DSerializer.serialize(directions2, DIRECTION);
        cyfa.write(accelerationsBatch1);
        cyfa.write(accelerationsBatch2);
        cyfr.write(rotationsBatch1);
        cyfr.write(rotationsBatch2);
        cyfd.write(directionsBatch1);
        cyfd.write(directionsBatch2);

        // Act - Deserialize
        final var accelerationBytes = cyfa.toByteArray();
        final var rotationBytes = cyfr.toByteArray();
        final var directionBytes = cyfd.toByteArray();
        // MeasurementBytes allows us to inject bytes without parsing them (to save resources on the mobile devices)
        final var builder = MeasurementBytes.newBuilder()
                .setFormatVersion(PERSISTENCE_FILE_FORMAT_VERSION)
                .setAccelerationsBinary(ByteString.copyFrom(accelerationBytes))
                .setRotationsBinary(ByteString.copyFrom(rotationBytes))
                .setDirectionsBinary(ByteString.copyFrom(directionBytes));
        final var measurementBytes = builder.build().toByteArray();
        final var measurement = parseFrom(measurementBytes);

        // Assert
        assertThat(measurement.getFormatVersion(), is(equalTo(3)));
        // We cannot test more here, see the description of this test.
    }
}
