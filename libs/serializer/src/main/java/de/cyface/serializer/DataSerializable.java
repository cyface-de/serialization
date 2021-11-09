/*
 * Copyright 2019-2021 Cyface GmbH
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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.cyface.model.Event;
import de.cyface.model.Point3D;
import de.cyface.model.RawRecord;
import de.cyface.protos.model.Measurement;

/**
 * An instance of this class contains the serialization functionality, adapted from the implementation used within the
 * Android App.
 * <p>
 * The serialized data is compressed. The Deflater ZLIB (RFC-1950) compression is used.
 * <p>
 * This class is absolutely not thread safe!
 *
 * @author Klemens Muthmann
 * @version 2.0.0
 * @since 1.0.0
 */
public final class DataSerializable {
    /**
     * The logger used to log messages from this class. Configure it using <tt>src/main/resources/logback.xml</tt>.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSerializable.class);
    /**
     * The current version of the transferred file. This is always specified by the first two bytes of the file
     * transferred and helps compatible APIs to process data from different client versions.
     */
    public static final short TRANSFER_FILE_FORMAT_VERSION = 2;
    /**
     * Since our current API Level does not support {@code Short.Bytes}.
     */
    public final static int SHORT_BYTES = Short.SIZE / Byte.SIZE;
    /**
     * The events to write
     */
    private final List<Event> events;
    /**
     * The locations to write
     */
    private final List<RawRecord> geoLocations;
    /**
     * The accelerations to write
     */
    private final List<List<? extends Point3D>> accelerationBatches;
    /**
     * The rotations to write
     */
    private final List<List<? extends Point3D>> rotationBatches;
    /**
     * The directions to write
     */
    private final List<List<? extends Point3D>> directionBatches;

    /**
     * Creates a new completely initialized object of this class.
     *
     * @param events The events to write
     * @param geoLocations The locations to write
     * @param accelerationBatches The accelerations to write
     * @param rotationBatches The rotations to write
     * @param directionBatches The directions to write
     */
    public DataSerializable(final List<Event> events, final List<RawRecord> geoLocations,
            final List<List<? extends Point3D>> accelerationBatches,
            final List<List<? extends Point3D>> rotationBatches, final List<List<? extends Point3D>> directionBatches) {
        Validate.notNull(events);
        Validate.notNull(geoLocations);
        Validate.notNull(accelerationBatches);
        Validate.notNull(rotationBatches);
        Validate.notNull(directionBatches);

        this.events = events;
        this.geoLocations = geoLocations;
        this.accelerationBatches = accelerationBatches;
        this.rotationBatches = rotationBatches;
        this.directionBatches = directionBatches;
    }

    /**
     * Implements the core algorithm of writing data into an array of bytes in the
     * Cyface format, ready to be compressed.
     * <p>
     * We assemble the data using a buffer to avoid OOM exceptions.
     * <p>
     * <b>ATTENTION:</b> The caller must make sure the {@param bufferedOutputStream} is closed when no longer needed
     * or the app crashes.
     *
     * @param bufferedOutputStream The {@link OutputStream} to which the serialized data should be written. Injecting
     *            this allows us to compress the serialized data without the need to write it into a temporary file.
     *            We require a {@link BufferedOutputStream} for performance reasons
     */
    public void serialize(final BufferedOutputStream bufferedOutputStream) {

        final var protoEvents = EventSerializer.events(events);
        final var locationRecords = LocationSerializer.locations(geoLocations);

        final var builder = Measurement.newBuilder()
                .setFormatVersion(TRANSFER_FILE_FORMAT_VERSION)
                .addAllEvents(protoEvents)
                .setLocationRecords(locationRecords);

        if (accelerationBatches.size() > 0) {
            LOGGER.trace(String.format("Serializing %s acceleration batches.", accelerationBatches.size()));
            accelerationBatches.forEach(
                    accelerations -> builder.addAccelerations(Point3DSerializer.accelerations(accelerations)));
        }
        if (rotationBatches.size() > 0) {
            LOGGER.trace(String.format("Serializing %s rotation batches.", rotationBatches.size()));
            rotationBatches.forEach(
                    rotations -> builder.addRotations(Point3DSerializer.rotations(rotations)));
        }
        if (directionBatches.size() > 0) {
            LOGGER.trace(String.format("Serializing %s direction batches.", directionBatches.size()));
            directionBatches.forEach(
                    directions -> builder.addDirections(Point3DSerializer.directions(directions)));
        }

        // Currently, loading the whole measurement into memory (~ 5 MB / hour serialized).
        // - To add high-res image data in the future we cannot use the pre-compiled builder but
        // have to stream the image data without loading it into memory to avoid an OOM exception.
        final var transferFileHeader = transferFileHeader();
        final var measurementBytes = builder.build().toByteArray();
        try {
            // The stream must be closed by the caller in a "finally" catch
            bufferedOutputStream.write(transferFileHeader);
            bufferedOutputStream.write(measurementBytes);
            bufferedOutputStream.flush();
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }

        LOGGER.debug(String.format("Serialized %s",
                humanReadableSize(transferFileHeader.length + measurementBytes.length, true)));
    }

    /**
     * Creates the header field for a serialized {@code Measurement} in big endian format for synchronization.
     *
     * (!) Attention: Changes to this format must be discussed with compatible API providers.
     *
     * @return The header byte array.
     */
    public static byte[] transferFileHeader() {

        byte[] ret = new byte[SHORT_BYTES];
        // noinspection ConstantConditions // short `2` has default `0` in the first byte
        ret[0] = (byte)(TRANSFER_FILE_FORMAT_VERSION >> 8);
        ret[1] = (byte)TRANSFER_FILE_FORMAT_VERSION;

        LOGGER.trace(String.format("Serialized %s fileHeader.", humanReadableSize(ret.length, true)));
        return ret;
    }

    /**
     * Pretty prints a counter of bytes in human-readable form. This is based on a thread from
     * <a href="https://stackoverflow.com/a/3758880/5815054">stackoverflow</a>.
     * <p>
     * For example 4096 becomes 4 kB a.s.o.
     *
     * @param bytes The bytes to pretty print
     * @param si Whether to use the SI notation for large numbers (e.g. kilo, mega) based on a factor of 1,000 or the
     *            binary prefix (e.g. kibi, mebi) based on a factor of 1,024.
     * @return A human-readable form of the provided bytes
     */
    public static String humanReadableSize(final long bytes, @SuppressWarnings("SameParameterValue") final boolean si) {
        final int unit = si ? 1000 : 1024;
        if (bytes < unit) {
            return bytes + " B";
        }
        final int exp = (int)(Math.log(bytes) / Math.log(unit));
        // noinspection SpellCheckingInspection
        final String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format(Locale.GERMAN, "%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }
}
