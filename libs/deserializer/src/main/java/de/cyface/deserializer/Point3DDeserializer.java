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
package de.cyface.deserializer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import de.cyface.model.Point3DImpl;
import de.cyface.protos.model.Accelerations;
import de.cyface.protos.model.Directions;
import de.cyface.protos.model.Rotations;
import de.cyface.serializer.DataSerializable;
import de.cyface.serializer.Formatter;
import de.cyface.serializer.GeoLocation;
import de.cyface.serializer.model.Point3DType;

/**
 * Deserializes sensor data from the {@link DataSerializable#TRANSFER_FILE_FORMAT_VERSION}.
 * <p>
 * Takes care of de-offsetting and converting the data to the default {@code Java} types used by {@link Point3DImpl}.
 *
 * @author Armin Schnabel
 * @version 1.0.0
 * @since 5.0.0
 */
@SuppressWarnings("unused") // API
public class Point3DDeserializer {

    /**
     * Deserializes acceleration data from the {@link DataSerializable#TRANSFER_FILE_FORMAT_VERSION}.
     * <p>
     * Takes care of de-offsetting and converting the data to the default {@code Java} types used by {@link Point3DImpl}.
     *
     * @param entries the data to deserialize
     * @return the deserialized entries
     */
    @SuppressWarnings("unused") // API
    public static List<Point3DImpl> deserialize(Accelerations entries) {

        final List<Formatter.Point3D> list = new ArrayList<>();
        for (int i = 0; i < entries.getTimestampCount(); i++) {
            final Formatter.Point3D entry = new Formatter.Point3D(entries.getTimestamp(i), entries.getX(i),
                    entries.getY(i),
                    entries.getZ(i));
            list.add(entry);
        }

        return deserialize(list, Point3DType.ACCELERATION);
    }

    /**
     * Deserializes rotation data from the {@link DataSerializable#TRANSFER_FILE_FORMAT_VERSION}.
     * <p>
     * Takes care of de-offsetting and converting the data to the default {@code Java} types used by {@link Point3DImpl}.
     *
     * @param entries the data to deserialize
     * @return the deserialized entries
     */
    @SuppressWarnings("unused") // API
    public static List<Point3DImpl> deserialize(Rotations entries) {

        final List<Formatter.Point3D> list = new ArrayList<>();
        for (int i = 0; i < entries.getTimestampCount(); i++) {
            final Formatter.Point3D entry = new Formatter.Point3D(entries.getTimestamp(i), entries.getX(i),
                    entries.getY(i),
                    entries.getZ(i));
            list.add(entry);
        }

        return deserialize(list, Point3DType.ROTATION);
    }

    /**
     * Deserializes direction data from the {@link DataSerializable#TRANSFER_FILE_FORMAT_VERSION}.
     * <p>
     * Takes care of de-offsetting and converting the data to the default {@code Java} types used by {@link Point3DImpl}.
     *
     * @param entries the data to deserialize
     * @return the deserialized entries
     */
    @SuppressWarnings("unused") // API
    public static List<Point3DImpl> deserialize(Directions entries) {

        final List<Formatter.Point3D> list = new ArrayList<>();
        for (int i = 0; i < entries.getTimestampCount(); i++) {
            final Formatter.Point3D entry = new Formatter.Point3D(entries.getTimestamp(i), entries.getX(i),
                    entries.getY(i),
                    entries.getZ(i));
            list.add(entry);
        }

        return deserialize(list, Point3DType.DIRECTION);
    }

    private static List<Point3DImpl> deserialize(List<Formatter.Point3D> entries, Point3DType type) {

        // The de-offsetter must be initialized once for each location
        final Point3DDeOffsetter deOffsetter = new Point3DDeOffsetter();

        return entries.stream().map(entry -> {

            // The proto serialized comes in a different format and in offset-format
            final Formatter.Point3D offsets = new Formatter.Point3D(entry.getTimestamp(), entry.getX(), entry.getY(),
                    entry.getZ());
            final Formatter.Point3D absolutes = deOffsetter.absolute(offsets);

            return DeFormatter.deFormat(type, absolutes);

        }).collect(Collectors.toList());
    }
}