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
import java.util.zip.GZIPInputStream;
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
        // Buffer the full compressed stream upfront so we can retry with different decompression
        // modes without re-reading from the source stream.
        final byte[] compressedBytes = compressedData.readAllBytes();

        // Three distinct compression formats are encountered in practice:
        //
        //  1. GZIP       — magic bytes 0x1F 0x8B; requires GZIPInputStream.
        //  2. ZLIB       — satisfies (CMF & 0x0F) == 8 AND (CMF*256+FLG) % 31 == 0; Inflater(false).
        //  3. Raw DEFLATE— no header; the Cyface SDK default; Inflater(true).
        //
        // Detection is best-effort: GZIP magic bytes are unambiguous, but a raw DEFLATE stream can
        // accidentally satisfy the ZLIB header check, so we always retry on ZipException.

        if (isGzip(compressedBytes)) {
            try {
                return readGzip(compressedBytes);
            } catch (ZipException e) {
                // Unlikely, but fall through and try as DEFLATE/ZLIB below.
            }
        }

        // Four distinct formats are encountered in practice:
        //
        //  1. GZIP          — handled above.
        //  2. ZLIB          — standard ZLIB wrapper; Inflater(nowrap=false).
        //  3. Raw DEFLATE   — no header; Cyface/iOS SDK default; Inflater(nowrap=true).
        //  4. Uncompressed  — raw protobuf bytes, no compression at all (observed from iOS partners).
        //
        // Try both DEFLATE modes first (ZLIB detection can be a false positive), then fall back to
        // treating the bytes as an uncompressed protobuf stream.
        final boolean isZlibWrapped = isZlib(compressedBytes);
        try {
            return readDeflate(compressedBytes, isZlibWrapped);
        } catch (ZipException ignored) {
            // fall through and retry with opposite nowrap mode
        }
        try {
            return readDeflate(compressedBytes, !isZlibWrapped);
        } catch (ZipException ignored) {
            // fall through and attempt uncompressed protobuf
        }
        // Last resort: the data was never compressed (e.g. certain iOS partner uploads).
        return parseDecompressedStream(new ByteArrayInputStream(compressedBytes));
    }

    /** Returns true if the bytes start with the GZIP magic number 0x1F 0x8B. */
    private static boolean isGzip(final byte[] bytes) {
        return bytes.length >= 2
                && (bytes[0] & 0xFF) == 0x1F
                && (bytes[1] & 0xFF) == 0x8B;
    }

    /**
     * Returns true if the bytes start with a valid ZLIB header.
     * A ZLIB stream satisfies: (CMF & 0x0F) == 8 AND (CMF * 256 + FLG) % 31 == 0.
     */
    private static boolean isZlib(final byte[] bytes) {
        return bytes.length >= 2
                && (bytes[0] & 0x0F) == 8
                && ((bytes[0] & 0xFF) * 256 + (bytes[1] & 0xFF)) % 31 == 0;
    }

    /**
     * Decompresses <code>compressedBytes</code> as GZIP and parses the result as a {@link Measurement}.
     */
    private Measurement readGzip(final byte[] compressedBytes)
            throws IOException, InvalidLifecycleEvents, UnsupportedFileVersion, NoTracksRecorded {
        try (final var gzipStream = new GZIPInputStream(new ByteArrayInputStream(compressedBytes))) {
            return parseDecompressedStream(gzipStream);
        }
    }

    /**
     * Decompresses <code>compressedBytes</code> using an {@link Inflater} and parses the result as a
     * {@link Measurement}.
     *
     * @param isZlibWrapped <code>true</code> to use standard ZLIB (nowrap=false),
     *                      <code>false</code> to use raw DEFLATE (nowrap=true)
     */
    private Measurement readDeflate(final byte[] compressedBytes, final boolean isZlibWrapped)
            throws IOException, InvalidLifecycleEvents, UnsupportedFileVersion, NoTracksRecorded {
        try (final var inflated = new InflaterInputStream(
                new ByteArrayInputStream(compressedBytes), new Inflater(!isZlibWrapped))) {
            return parseDecompressedStream(inflated);
        }
    }

    /**
     * Parses a decompressed stream as a {@link Measurement}, handling both the Cyface format
     * (which prepends a 2-byte version header) and the external format (raw protobuf, no header).
     * <p>
     * The Cyface version header always starts with 0x00 (high byte of a big-endian short for any
     * version &lt; 256). Protobuf field tags can never start with 0x00 (field number 0 is illegal),
     * so the two formats are unambiguous.
     */
    private Measurement parseDecompressedStream(final InputStream decompressedStream)
            throws IOException, InvalidLifecycleEvents, UnsupportedFileVersion, NoTracksRecorded {
        final var pushback = new PushbackInputStream(decompressedStream, 2);
        final var firstTwoBytes = new byte[2];
        final int bytesRead = pushback.read(firstTwoBytes, 0, 2);
        if (bytesRead > 0) {
            pushback.unread(firstTwoBytes, 0, bytesRead);
        }

        if (bytesRead >= 1 && firstTwoBytes[0] == 0x00) {
            // Cyface format: validate the version and consume the 2-byte header.
            final short version = bytesRead == 2
                    ? (short) (((firstTwoBytes[0] & 0xFF) << 8) | (firstTwoBytes[1] & 0xFF))
                    : 0;
            if (version != TRANSFER_FILE_FORMAT_VERSION) {
                throw new UnsupportedFileVersion(
                        String.format("Encountered data in invalid format version (%s).", version));
            }
            if (pushback.read() == -1 || pushback.read() == -1) {
                throw new IOException("Unexpected end of stream while discarding version header.");
            }
        }
        // Stream now points at the raw protobuf payload.
        return new V3UncompressedBinaryFormatDeserializer(metaData, pushback).read();
    }

    @Override
    public List<MeasurementIdentifier> peakIntoDatabase() {
        return Collections.singletonList(metaData.getIdentifier());
    }
}
