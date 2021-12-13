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

import java.io.IOException;
import java.io.InputStream;
import java.io.StreamCorruptedException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;

import de.cyface.model.MeasurementIdentifier;
import de.cyface.model.Modality;
import de.cyface.model.Point3D;
import de.cyface.model.RawRecord;
import de.cyface.protos.model.Measurement;
import de.cyface.protos.model.MeasurementBytes;
import de.cyface.serializer.model.Point3DType;

/**
 * An internal class containing utility methods for parsing the Cyface binary format. These methods are used by
 * different {@link Deserializer} implementations.
 * 
 * @author Klemens Muthmann
 */
class BinaryFormatParser {
    /**
     * The logger used by this class. Configure it using <tt>src/main/resources/logback.xml</tt>.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(BinaryFormatParser.class);

    /**
     * Private constructor to avoid instantiation of utility class
     */
    private BinaryFormatParser() {
        // Nothing to do here.
    }

    /**
     * Reads <code>count</code> geolocations from the <code>input</code> and
     * provides them in the form of a new <code>BufferedDataTable</code>.
     *
     * @param input The stream to read the geolocations from
     * @param count The number of geolocations to read
     * @param identifier the identifier of the locations' measurement
     * @throws IOException If reading the stream fails
     */
    static List<RawRecord> readGeoLocations(final InputStream input, final int count,
            final MeasurementIdentifier identifier) throws IOException {
        final var locationRecords = new ArrayList<RawRecord>(count);
        for (var i = 0; i < count; i++) {
            final var timestamp = readLong(input);
            final var latitude = readDouble(input);
            final var longitude = readDouble(input);
            final var speed = readDouble(input);
            final var accuracy = readInt(input);
            final var record = new RawRecord(identifier, timestamp, latitude, longitude, null, accuracy / 100.0,
                    speed, Modality.UNKNOWN);
            locationRecords.add(record);
        }
        LOGGER.debug("Read {} locations!", locationRecords.size());
        return locationRecords;
    }

    /**
     * Reads <code>count</code> 3D points from the <code>input</code> and provides them in the form of a new
     * <code>BufferedDataTable</code>. These might be accelerations, rotations or directions ins space.
     *
     * @param input The stream to read the points from
     * @param type The type of the sensor data provided as {@code input}
     * @return The list of {@link Point3D} instances that were part of the provided <code>InputStream</code>.
     * @throws IOException If reading the stream fails
     */
    static Measurement readPoint3Ds(final InputStream input, final Point3DType type)
            throws IOException {

        // `MeasurementBytes` to inject the sensor bytes which are missing the header of the `ccyf` format
        final var builder = MeasurementBytes.newBuilder();
        switch (type) {
            case ACCELERATION:
                builder.setAccelerationsBinary(ByteString.readFrom(input));
                break;
            case ROTATION:
                builder.setRotationsBinary(ByteString.readFrom(input));
                break;
            case DIRECTION:
                builder.setDirectionsBinary(ByteString.readFrom(input));
                break;
            default:
                throw new IllegalArgumentException(String.format("Unknown type: %s", type));
        }

        // Now we have the bytes in the `ccyf` format and can parse the data using the normal `Measurement` class
        final var bytes = builder.build().toByteArray();
        return Measurement.parseFrom(bytes);
    }

    /**
     * Reads the next two byte from the <code>input</code> as a <code>short</code>
     * value. The bytes should be ordered in Java typical big endian format.
     *
     * @param input An open input stream capable of providing at least two bytes of
     *            data.
     * @return The <code>short</code> value read from the input stream.
     * @throws IOException If reading the stream fails
     */
    static short readShort(final InputStream input) throws IOException {
        var shortByteArray = read(input, Short.BYTES);
        return shortByteArray.getShort();
    }

    /**
     * Reads the next four byte from the <code>input</code> as an <code>int</code>
     * value. The bytes should be ordered in Java typical big endian format.
     *
     * @param input An open input stream capable of providing at least four bytes of
     *            data.
     * @return The <code>double</code> value read from the input stream.
     * @throws IOException If reading the stream fails
     */
    static int readInt(final InputStream input) throws IOException {
        return read(input, Integer.BYTES).getInt();
    }

    /**
     * Reads the next eight byte from the <code>input</code> as a <code>long</code>
     * value. The bytes should be ordered in Java typical big endian format.
     *
     * @param input An open input stream capable of providing at least eight bytes
     *            of data.
     * @return The <code>long</code> value read from the input stream.
     * @throws IOException If reading the stream fails
     */
    static long readLong(final InputStream input) throws IOException {
        return read(input, Long.BYTES).getLong();
    }

    /**
     * Reads the next eight byte from the <code>input</code> as a
     * <code>double</code> value. The bytes should be ordered in Java typical big
     * endian format.
     *
     * @param input An open input stream capable of providing at least eight bytes
     *            of data
     * @return The <code>double</code> value read from the input stream
     * @throws IOException If reading the stream fails
     */
    static double readDouble(final InputStream input) throws IOException {
        return read(input, Double.BYTES).getDouble();
    }

    /**
     * Internal method to read a number of bytes from an <code>InputStream</code> into a <code>ByteBuffer</code>.
     * The returned <code>ByteBuffer</code> is already prepared for being read again.
     *
     * @param input The stream to read the bytes from
     * @param bytes The number of bytes to read
     * @return A <code>ByteBuffer</code> containing the bytes
     * @throws IOException If it was impossible to read <code>bytes</code> from the provided stream
     */
    private static ByteBuffer read(final InputStream input, int bytes) throws IOException {
        final var buffer = ByteBuffer.allocate(bytes);
        for (var i = 0; i < bytes; i++) {
            var readByteAsInt = input.read();
            if (readByteAsInt == -1) {
                throw new StreamCorruptedException("Unexpected end of stream reached!");
            }
            var readByte = (byte)readByteAsInt;
            buffer.put(readByte);
        }
        buffer.position(0);
        return buffer;
    }
}
