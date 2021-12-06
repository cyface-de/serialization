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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serializer for files to upload to a Cyface Data Collector.
 * This might either be a file with serialized data or a file with serialized events.
 *
 * @author Klemens Muthmann
 * @author Armin Schnabel
 * @see DataSerializable
 * @see EventSerializer
 */
public final class Serializer {
    /**
     * The logger used to log messages from this class. Configure it using <tt>src/main/resources/logback.xml</tt>.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Serializer.class);
    /**
     * ZLIB compression level used to compress simulated data.
     */
    private static final int DEFLATER_LEVEL = 5; // 'cause Steve Jobs said so
    /**
     * In iOS there are no parameters to set nowrap to false as it is default in Android.
     * In order for the iOS and Android Cyface SDK to be compatible we set nowrap explicitly to true
     * <p>
     * <b>ATTENTION:</b> When decompressing in Android you need to pass this parameter to the {@link Inflater}'s
     * constructor.
     */
    private static final boolean COMPRESSION_NOWRAP = true;

    /**
     * Writes compressed provided measurement data in the current format to a temp file, ready to
     * be transferred.
     *
     * @param serializable the data to serialize
     * @param fileName The file name of a temporary file to serialize the data to
     * @return A {@link Path} pointing to a temporary file containing the serialized compressed data for transfer
     */
    public static Path serialize(final DataSerializable serializable, final String fileName) {

        Path compressedTempFile = null;
        try {
            // Store the compressed bytes into a temp file to be able to read the byte size for transmission
            compressedTempFile = makeTempFile(fileName);

            try (var fileOutputStream = Files.newOutputStream(compressedTempFile)) {

                // As we create the DeflaterOutputStream with an FileOutputStream the compressed data is written to file
                writeTo(serializable, fileOutputStream);
            }

            return compressedTempFile;
        } catch (IOException e) {
            if (compressedTempFile != null && Files.exists(compressedTempFile)) {
                try {
                    Files.delete(compressedTempFile);
                } catch (IOException eInner) {
                    throw new IllegalStateException(eInner);
                }
            }

            throw new IllegalStateException(e);
        }
    }

    private static Path makeTempFile(final String fileName) throws IOException {
        if (fileName.contains(".")) {
            final var name = fileName.substring(0, fileName.lastIndexOf('.'));
            final var fileSuffix = fileName.substring(fileName.lastIndexOf('.'));
            return Files.createTempFile(name, fileSuffix);
        } else {
            return Files.createTempFile(fileName, null);
        }
    }

    /**
     * Writes the provided data to the provided <code>OutputStream</code>.
     *
     * @param fileOutputStream the {@link OutputStream} to write the compressed data to
     * @throws IOException When flushing or closing the {@link OutputStream} fails
     */
    private static void writeTo(final DataSerializable serializable, final OutputStream fileOutputStream)
            throws IOException {

        LOGGER.debug("loadSerializedCompressed: start");
        final var startTimestamp = System.currentTimeMillis();
        // These streams don't throw anything and, thus, it should be enough to close the outermost stream at the end

        // Wrapping the streams with Buffered streams for performance reasons
        final var bufferedFileOutputStream = new BufferedOutputStream(fileOutputStream);

        final var compressor = new Deflater(DEFLATER_LEVEL, COMPRESSION_NOWRAP);
        // As we wrap the injected outputStream with Deflater the serialized data is automatically compressed
        final var deflaterStream = new DeflaterOutputStream(bufferedFileOutputStream, compressor);

        // This architecture catches the IOException thrown by the close() called in the "finally" without IDE warning
        try (var bufferedDeflaterOutputStream = new BufferedOutputStream(deflaterStream)) {

            // Injecting the outputStream into which the serialized (in this case compressed) data is written to
            serializable.serialize(bufferedDeflaterOutputStream);
            bufferedDeflaterOutputStream.flush();
        }
        LOGGER.debug("loadSerializedCompressed: finished after "
                + ((System.currentTimeMillis() - startTimestamp) / 1000) + " s with Deflater Level: " + DEFLATER_LEVEL);
    }
}
