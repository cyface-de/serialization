/*
 * Copyright 2020-2026 Cyface GmbH
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
package de.cyface.deserializer.factory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipException;

import de.cyface.deserializer.Deserializer;
import de.cyface.deserializer.UnsupportedFileVersion;
import de.cyface.deserializer.exceptions.InvalidLifecycleEvents;
import de.cyface.model.Measurement;
import de.cyface.model.MeasurementIdentifier;
import de.cyface.model.MetaData;
import de.cyface.model.NoTracksRecorded;

/**
 * A {@link Deserializer} for a file in Cyface binary format. Constructs a new measurement from a ZLIB compressed
 * <code>InputStream</code> of data.
 * <p>
 * A {@link DeserializerFactory} is necessary to create such a <code>Deserializer</code>.
 * 
 * @see DeserializerFactory
 */
public class BinaryFormatDeserializer implements Deserializer {
    /**
     * Used to serialize objects of this class. Only change this if this classes attribute set has changed.
     */
    private static final long serialVersionUID = -706300845329533657L;
    /**
     * The default value for the parameter "nowrap" used in the Cyface Binary Format. This parameter needs to be passed
     * to the Inflater's constructor to decompress the compressed bytes.
     */
    private static final boolean NOWRAP = true;
    /**
     * The current version of the transferred file. This is always specified by the first two bytes of the file
     * transferred and helps compatible APIs to process data from different client versions.
     */
    public static final short TRANSFER_FILE_FORMAT_VERSION = 3;

    /**
     * The meta information about the {@link Measurement}. This information is not part of the datafiles but is usually
     * stored alongside the binary data. It is usually used to get a glimpse into what data to expect.
     */
    private final MetaData metaData;
    /**
     * The stream of compressed data to load locations and measured data points from
     */
    private final InputStream compressedData;

    /**
     * Creates a new completely initialized object of this class. The constructor is not public, since one should use a
     * {@link DeserializerFactory} to create instances of this class.
     * 
     * @param metaData The meta information about the {@link Measurement}. This information is not part of the datafiles
     *            but is usually stored alongside the binary data. It is usually used to get a glimpse into what data to
     *            expect
     * @param compressedData The stream of compressed data to load locations and measured data points from
     */
    BinaryFormatDeserializer(final MetaData metaData, final InputStream compressedData) {
        this.metaData = Objects.requireNonNull(metaData);
        this.compressedData = Objects.requireNonNull(compressedData);
    }

    @Override
    public Measurement read() throws IOException, InvalidLifecycleEvents, UnsupportedFileVersion, NoTracksRecorded {
        // Buffer the full compressed stream upfront so we can retry with the opposite decompression
        // mode if the format detection from the first two bytes turns out to be a false positive.
        // (A raw DEFLATE stream can accidentally satisfy the ZLIB header check and vice versa.)
        final byte[] compressedBytes = compressedData.readAllBytes();

        // Detect compression format: a valid ZLIB stream satisfies
        // (CMF & 0x0F) == 8  AND  (CMF * 256 + FLG) % 31 == 0.
        final boolean isZlibWrapped = compressedBytes.length >= 2
                && (compressedBytes[0] & 0x0F) == 8
                && ((compressedBytes[0] & 0xFF) * 256 + (compressedBytes[1] & 0xFF)) % 31 == 0;

        // Try the detected mode first; fall back to the opposite on ZipException.
        try {
            return readDecompressed(compressedBytes, isZlibWrapped);
        } catch (ZipException e) {
            return readDecompressed(compressedBytes, !isZlibWrapped);
        }
    }

    /**
     * Decompresses <code>compressedBytes</code> and parses the resulting stream as a {@link Measurement}.
     *
     * @param compressedBytes the raw compressed bytes
     * @param isZlibWrapped   <code>true</code> to use standard ZLIB (nowrap=false),
     *                        <code>false</code> to use raw DEFLATE (nowrap=true)
     */
    private Measurement readDecompressed(final byte[] compressedBytes, final boolean isZlibWrapped)
            throws IOException, InvalidLifecycleEvents, UnsupportedFileVersion, NoTracksRecorded {
        try (final var inflated = new InflaterInputStream(
                new ByteArrayInputStream(compressedBytes), new Inflater(!isZlibWrapped))) {
            // Peek at the first two decompressed bytes to detect whether the Cyface 2-byte version
            // header is present. The high byte is always 0x00 for any supported version (< 256),
            // while protobuf field tags can never start with 0x00 (field number 0 is illegal in
            // protobuf), so the two formats are unambiguous.
            final var decompressedPushback = new PushbackInputStream(inflated, 2);
            final var firstTwoBytes = new byte[2];
            final int dBytesRead = decompressedPushback.read(firstTwoBytes, 0, 2);
            if (dBytesRead > 0) {
                decompressedPushback.unread(firstTwoBytes, 0, dBytesRead);
            }

            if (dBytesRead >= 1 && firstTwoBytes[0] == 0x00) {
                // Cyface format: validate the version and consume the 2-byte header.
                final short version = dBytesRead == 2
                        ? (short) (((firstTwoBytes[0] & 0xFF) << 8) | (firstTwoBytes[1] & 0xFF))
                        : 0;
                if (version != TRANSFER_FILE_FORMAT_VERSION) {
                    throw new UnsupportedFileVersion(
                            String.format("Encountered data in invalid format version (%s).", version));
                }
                if (decompressedPushback.read() == -1 || decompressedPushback.read() == -1) {
                    throw new IOException("Unexpected end of stream while discarding version header.");
                }
            }
            // Whether the header was present or not, the stream now points at the raw protobuf payload.
            return new V3UncompressedBinaryFormatDeserializer(metaData, decompressedPushback).read();
        }
    }

    @Override
    public List<MeasurementIdentifier> peakIntoDatabase() {
        return Collections.singletonList(metaData.getIdentifier());
    }
}
