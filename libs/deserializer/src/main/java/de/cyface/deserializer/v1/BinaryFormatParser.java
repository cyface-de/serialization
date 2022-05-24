/*
 * Copyright (C) 2020 Cyface GmbH - All Rights Reserved
 *
 * This file is part of the Cyface Server Backend.
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */
package de.cyface.deserializer.v1;

import java.io.IOException;
import java.io.InputStream;
import java.io.StreamCorruptedException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.cyface.model.v1.MeasurementIdentifier;
import de.cyface.model.v1.Modality;
import de.cyface.model.v1.Point3D;
import de.cyface.model.v1.RawRecord;

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
     * The charset used to parse Strings (e.g. for JSON data)
     */
    static final String DEFAULT_CHARSET = "UTF-8";

    /**
     * Private constructor to avoid instantiation of utility class
     */
    private BinaryFormatParser() {
        // Nothing to do here.
    }

    /**
     * Reads <code>count</code> geo locations from the <code>input</code> and
     * provides them in the form of a new <code>BufferedDataTable</code>.
     *
     * @param input The stream to read the geo locations from
     * @param count The number of geo locations to read
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
     * @param count The number of points to read
     * @return The list of {@link Point3D} instances that were part of the provided <code>InputStream</code>.
     * @throws IOException If reading the stream fails
     */
    static List<Point3D> readPoint3Ds(final InputStream input, final int count)
            throws IOException {
        final var ret = new ArrayList<Point3D>(count);
        for (var i = 0; i < count; i++) {
            LOGGER.trace("Processing {} point3Ds!", i);
            final var timestamp = readLong(input);
            final var xValue = readDouble(input);
            final var yValue = readDouble(input);
            final var zValue = readDouble(input);

            final var point3D = new Point3D(Double.valueOf(xValue).floatValue(),
                    Double.valueOf(yValue).floatValue(), Double.valueOf(zValue).floatValue(), timestamp);
            ret.add(point3D);
        }
        Collections.sort(ret);
        LOGGER.debug("Read {} point3Ds!", ret.size());
        return ret;
    }

    /**
     * Reads the next <code>byteLength</code> bytes from the <code>input</code> as a <code>String</code>.
     * The bytes should be ordered in Java typical big endian format.
     *
     * @param input An open input stream capable of providing at least two bytes of
     *            data.
     * @return The <code>short</code> value read from the input stream.
     * @throws IOException If reading the stream fails
     */
    static String readString(final InputStream input, final short byteLength) throws IOException {
        var buffer = read(input, byteLength);
        var bytes = buffer.array();
        return new String(bytes, DEFAULT_CHARSET);
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