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

import de.cyface.serializer.Formatter;
import org.apache.commons.lang3.Validate;

/**
 * Calculates the absolute number from the offset/diff format, e.g.: 12345678901234, 12345678902234, 12345678903234 from
 * 12345678901234, 1000, 1000 for timestamps.
 * <p>
 * The first number "seen" is used as offset and as absolute number. Subsequent numbers are seen as in the
 * diff-format, i.e. as the relative difference to the previous number passed.
 * <p>
 * This format is used by the Cyface ProtoBuf Messages: https://github.com/cyface-de/protos
 *
 * @author Armin Schnabel
 * @version 1.0.0
 * @since 5.0.0
 */
public class Point3DDeOffsetter {
    private final DeOffsetter ts;
    private final DeOffsetter x;
    private final DeOffsetter y;
    private final DeOffsetter z;

    /**
     * Constructs a fully initialized instance of this class.
     */
    public Point3DDeOffsetter() {
        ts = new DeOffsetter();
        x = new DeOffsetter();
        y = new DeOffsetter();
        z = new DeOffsetter();
    }

    /**
     * Calculates the absolute number from the offset/diff format, e.g.: 12345678901234, 12345678902234, 12345678903234
     * from 12345678901234, 1000, 1000 for timestamps.
     * <p>
     * The first number "seen" is used as offset and as absolute number. Subsequent numbers are seen as in the
     * diff-format, i.e. as the relative difference to the previous number passed.
     *
     * @param point the data in the offset-format. Make subsequent calls in order!
     * @return the data as absolute number.
     */
    public Formatter.Point3D absolute(Formatter.Point3D point) {
        final long timestamp = ts.absolute(point.getTimestamp());
        final long xValue = x.absolute(point.getX());
        final long yValue = y.absolute(point.getY());
        final long zValue = z.absolute(point.getZ());
        Validate.isTrue(xValue <= Integer.MAX_VALUE);
        Validate.isTrue(yValue <= Integer.MAX_VALUE);
        Validate.isTrue(zValue <= Integer.MAX_VALUE);
        return new Formatter.Point3D(timestamp, (int)xValue, (int)yValue, (int)zValue);
    }
}