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

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

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
        // Peek at the first two bytes to detect whether the stream uses standard ZLIB format (nowrap=false)
        // or raw DEFLATE (nowrap=true, the Cyface default).
        // A valid ZLIB stream satisfies: (CMF & 0x0F) == 8  AND  (CMF * 256 + FLG) % 31 == 0.
        // Data uploaded by external partners may use standard ZLIB even though the Cyface SDK uses raw DEFLATE.
        final var pushback = new PushbackInputStream(compressedData, 2);
        final var header = new byte[2];
        final int bytesRead = pushback.read(header, 0, 2);
        if (bytesRead > 0) {
            pushback.unread(header, 0, bytesRead);
        }
        final boolean isZlibWrapped = bytesRead == 2
                && (header[0] & 0x0F) == 8
                && ((header[0] & 0xFF) * 256 + (header[1] & 0xFF)) % 31 == 0;
        final boolean nowrap = !isZlibWrapped;

        try (InflaterInputStream uncompressedInput = new InflaterInputStream(pushback, new Inflater(nowrap))) {
            final var version = BinaryFormatParser.readShort(uncompressedInput);
            if (version != TRANSFER_FILE_FORMAT_VERSION) {
                throw new UnsupportedFileVersion(
                        String.format("Encountered data in invalid format version (%s).", version));
            }

            return new V3UncompressedBinaryFormatDeserializer(metaData, uncompressedInput).read();
        }
    }

    @Override
    public List<MeasurementIdentifier> peakIntoDatabase() {
        return Collections.singletonList(metaData.getIdentifier());
    }
}
