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

import de.cyface.model.Point3DImpl;
import de.cyface.serializer.Formatter;
import de.cyface.serializer.GeoLocation;
import de.cyface.serializer.model.Point3DType;
import org.apache.commons.lang3.Validate;

import static de.cyface.serializer.model.Point3DType.ACCELERATION;
import static de.cyface.serializer.model.Point3DType.DIRECTION;
import static de.cyface.serializer.model.Point3DType.ROTATION;

/**
 * DeFormatter which parses sensor- or location point attributes from the unit expected by the Cyface ProtoBuf
 * serializer.
 *
 * @author Armin Schnabel
 * @version 1.0.0
 * @since 5.0.0
 */
public class DeFormatter {

    /**
     * Converts the number from the format expected by the Cyface ProtoBuf serializer.
     *
     * @param formatted the formatted number, e.g. 51_012345 or 13_012300
     * @return the coordinate-part, e.g.: 51.012345 or 13.012300
     */
    private static double coordinate(int formatted) {
        return formatted / 1_000_000.0;
    }

    /**
     * Converts the number from the format expected by the Cyface ProtoBuf serializer.
     *
     * @param formatted the formatted number, e.g. 11_00 cm/s
     * @return the speed in m/s, e.g.: 11.0m/s
     */
    private static double speed(int formatted) {
        return formatted / 100.0;
    }

    /**
     * Converts the number from the format expected by the Cyface ProtoBuf serializer.
     *
     * @param formatted the formatted number, e.g. 3_00 cm
     * @return the speed in m, e.g.: 3.0m
     */
    private static double accuracy(int formatted) {
        return formatted / 100.0;
    }

    /**
     * Converts the number from the format expected by the Cyface ProtoBuf serializer.
     *
     * @param formatted the acceleration value in m/s^2, e.g.: +9.81 m/s (earth gravity)
     * @return the formatted number, e.g. 9_810 mm/s^2
     */
    private static float acceleration(int formatted) {
        return formatted / 1_000.0f;
    }

    /**
     * Converts the number from the format expected by the Cyface ProtoBuf serializer.
     *
     * @param formatted the formatted number, e.g. 83 rad/1000s (not /ms!)
     * @return the rotation value in rad/s, e.g.: 0.083 rad/s
     */
    private static float rotation(int formatted) {
        return formatted / 1_000.0f;
    }

    /**
     * Converts the number from the format expected by the Cyface ProtoBuf serializer.
     *
     * @param formatted the formatted number, e.g. 67 µT/100 (unit: 10 nT)
     * @return the direction value in µT, e.g.: 0.67 µT
     */
    private static float direction(int formatted) {
        return formatted / 100.0f;
    }

    /**
     * Parses a location point from the unit expected by the Cyface ProtoBuf serializer.
     *
     * @param location the location to parse
     * @return the parsed location
     */
    public static GeoLocation deFormat(Formatter.Location location) {
        final double latitude = DeFormatter.coordinate(location.getLatitude());
        final double longitude = DeFormatter.coordinate(location.getLongitude());
        final double speed = DeFormatter.speed(location.getSpeed());
        final double accuracy = DeFormatter.accuracy(location.getAccuracy());
        return new GeoLocation(latitude, longitude, location.getTimestamp(), speed, accuracy);
    }

    /**
     * Parses a sensor point from the unit expected by the Cyface ProtoBuf serializer.
     *
     * @param type the sensor type of the data
     * @param point the point to parse
     * @return the parsed data
     */
    public static Point3DImpl deFormat(Point3DType type, Formatter.Point3D point) {
        Validate.isTrue(type.equals(ACCELERATION) || type.equals(ROTATION) || type.equals(DIRECTION));
        final float x = type == ACCELERATION ? acceleration(point.getX())
                : type == ROTATION ? rotation(point.getX()) : direction(point.getX());
        final float y = type == ACCELERATION ? acceleration(point.getY())
                : type == ROTATION ? rotation(point.getY()) : direction(point.getY());
        final float z = type == ACCELERATION ? acceleration(point.getZ())
                : type == ROTATION ? rotation(point.getZ()) : direction(point.getZ());
        return new Point3DImpl(x, y, z, point.getTimestamp());
    }
}