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

import static de.cyface.serializer.model.Point3DType.ACCELERATION;
import static de.cyface.serializer.model.Point3DType.DIRECTION;
import static de.cyface.serializer.model.Point3DType.ROTATION;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.cyface.model.Point3D;
import de.cyface.protos.model.Accelerations;
import de.cyface.protos.model.AccelerationsFile;
import de.cyface.protos.model.Directions;
import de.cyface.protos.model.DirectionsFile;
import de.cyface.protos.model.Rotations;
import de.cyface.protos.model.RotationsFile;
import de.cyface.serializer.model.Point3DType;

/**
 * Serializer wrapper for {@code Point3D}s which takes care of offsetting and lossy compression applied before
 * serializing.
 *
 * @author Armin Schnabel
 * @version 1.0.0
 * @since 5.0.0
 */
public class Point3DSerializer {

    /**
     * The logger used to log messages from this class. Configure it using <tt>src/main/resources/logback.xml</tt>.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Point3DSerializer.class);

    /**
     * Serializes the provided {@link Formatter.Point3D} points.
     * <p>
     * Also writes the Header field to separate this batch from the next when written to a file.
     *
     * @param data The {@code Point3d} points to serialize.
     * @param type The sensor data type of the {@code Point3d} data.
     * @return A {@code byte} array containing all the data.
     */
    public static byte[] serialize(final List<? extends Point3D> data, Point3DType type) {
        LOGGER.debug(String.format("Serializing %d Point3d points.", data.size()));

        switch (type) {
            case ACCELERATION:
                final var aBatch = accelerations(data).build();
                // Ensure the `Accelerations` Header is also written to separate this batch from the next
                return AccelerationsFile.newBuilder().addAccelerations(aBatch).build().toByteArray();
            case ROTATION:
                final var rBatch = rotations(data).build();
                // Ensure the `Rotations` Header is also written to separate this batch from the next
                return RotationsFile.newBuilder().addRotations(rBatch).build().toByteArray();
            case DIRECTION:
                final var dBatch = directions(data).build();
                // Ensure the `Directions` Header is also written to separate this batch from the next
                return DirectionsFile.newBuilder().addDirections(dBatch).build().toByteArray();
            default:
                throw new IllegalArgumentException("Unknown type: " + type);
        }
    }

    /**
     * Converts accelerations to the class supported by the {@code Protobuf} generated serializer classes.
     * <p>
     * Applies lossy compression and offsetting to optimize the encoding density.
     *
     * @param data the locations to convert
     * @return the converted data
     */
    public static Accelerations.Builder accelerations(final List<? extends Point3D> data) {
        final var builder = Accelerations.newBuilder();

        // The offsetter must be initialized once for each point
        final var offsetter = new Point3DOffsetter();

        for (final var point : data) {
            final var offsets = convert(point, ACCELERATION, offsetter);
            builder.addTimestamp(offsets.getTimestamp())
                    .addX(offsets.getX())
                    .addY(offsets.getY())
                    .addZ(offsets.getZ());
        }
        return builder;
    }

    /**
     * Converts rotations to the class supported by the {@code Protobuf} generated serializer classes.
     * <p>
     * Applies lossy compression and offsetting to optimize the encoding density.
     *
     * @param data the locations to convert
     * @return the converted data
     */
    public static Rotations.Builder rotations(final List<? extends Point3D> data) {
        final var builder = Rotations.newBuilder();

        // The offsetter must be initialized once for each point
        final var offsetter = new Point3DOffsetter();

        for (final var point : data) {
            final var offsets = convert(point, ROTATION, offsetter);
            builder.addTimestamp(offsets.getTimestamp())
                    .addX(offsets.getX())
                    .addY(offsets.getY())
                    .addZ(offsets.getZ());
        }
        return builder;
    }

    /**
     * Converts direction points to the class supported by the {@code Protobuf} generated serializer classes.
     * <p>
     * Applies lossy compression and offsetting to optimize the encoding density.
     *
     * @param data the locations to convert
     * @return the converted data
     */
    public static Directions.Builder directions(final List<? extends Point3D> data) {
        final var builder = Directions.newBuilder();

        // The offsetter must be initialized once for each point
        final var offsetter = new Point3DOffsetter();

        for (final var point : data) {
            final var offsets = convert(point, DIRECTION, offsetter);
            builder.addTimestamp(offsets.getTimestamp())
                    .addX(offsets.getX())
                    .addY(offsets.getY())
                    .addZ(offsets.getZ());
        }
        return builder;
    }

    /**
     * Converts a {@code Point3D} to the class supported by the {@link Formatter}.
     * <p>
     * Applies lossy compression and offsetting to optimize the encoding density.
     *
     * @param point the point to convert
     * @param type the {@link Point3DType} of the {@code point} provided
     * @param offsetter the {@link Point3DOffsetter} to use for applying off-setting
     * @return the converted point
     */
    private static Formatter.Point3D convert(final Point3D point, final Point3DType type,
            final Point3DOffsetter offsetter) {

        // The proto serializer expects some fields in a different format and in offset-format
        final var formatted = new Formatter.Point3D(type, point.getTimestamp(), point.getX(),
                point.getY(), point.getZ());
        return offsetter.offset(formatted);
    }
}
