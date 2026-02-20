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
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import de.cyface.deserializer.Deserializer;
import de.cyface.model.MetaData;

/**
 * A collection of static factory methods to hide the possible complexity of {@link Deserializer} creation.
 * Calling this class allows one to create deserializers for compressed and uncompressed phone and Mongo database data.
 */
public final class DeserializerFactory {

    /**
     * Private constructor to avoid instantiation of static utility class.
     */
    private DeserializerFactory() {
        // Nothing to do here.
    }

    /**
     * Start the builder for deserializers for compressed data.
     */
    public static CompressedCreator forCompressedData() {
        return new CompressedImpl();
    }

    /**
     * Start the builder for deserializers for uncompressed data.
     */
    public static UncompressedCreator forUncompressedData() {
        return new UncompressedImpl();
    }

    public interface Creator {
        /**
         * Create a new {@link Deserializer} for a {@link de.cyface.model.Measurement} in Cyface Binary data with its
         * accompanying events.
         * Both are provided as compressed input streams, together with the {@link MetaData} about the
         * <code>Measurement</code>.
         *
         * @param metaData   The meta information about the <code>Measurement</code> to load
         * @param dataStream The compressed input stream containing the <code>Measurement</code> data in Cyface
         *                   binary format
         * @return A <code>Deserializer</code> for the Cyface binary format
         * @throws IOException When writing data failed
         */
        Deserializer create(final MetaData metaData, final InputStream dataStream) throws IOException;
    }

    private static abstract class CreatorImpl implements Creator {
        private final boolean forCompressedData;
        CreatorImpl(boolean forCompressedData) {
            this.forCompressedData = forCompressedData;
        }
        @Override
        public Deserializer create(final MetaData metaData, final InputStream dataStream) {
            if (forCompressedData) {
                return new BinaryFormatDeserializer(metaData, dataStream);
            } else {
                return new V3UncompressedBinaryFormatDeserializer(metaData, dataStream);
            }
        }
    }

    /**
     * Interface for building Deserializers for compressed data.
     */
    public interface CompressedCreator extends DeserializerFactory.Creator {
        /**
         * Create a new {@link Deserializer} for data directly exported from within the Cyface Android App. This data comes
         * in the form of four zip archives. The first contains an SQLite database with the location information and some
         * metadata. The other three contain the sensor data from the accelerometer, the gyroscope and the compass.
         *
         * @param userId               The id of the user who created the data to be deserialized by the created deserializer.
         *                             This information is lost during the export process and needs to be provided here
         * @param measuresArchive      The archive containing the SQLite database with the location data
         * @param accelerationsArchive The archive containing the accelerations from the accelerometer
         * @param rotationsArchive     The archive containing rotations from the gyroscope
         * @param directionsArchive    The archive containing directions from the compass
         * @param uploadDate           The upload date when the `Measurement`s were uploaded to the collector.
         * @return A {@link ZippedPhoneDataDeserializer} to read measurements from a phone export
         */
        Deserializer create(
                final UUID userId,
                final Path measuresArchive,
                final Path accelerationsArchive,
                final Path rotationsArchive,
                final Path directionsArchive,
                final Date uploadDate);
    }

    private static final class CompressedImpl extends CreatorImpl implements CompressedCreator {

        CompressedImpl() {
            super(true);
        }

        @Override
        public Deserializer create(UUID userId, Path measuresArchive, Path accelerationsArchive, Path rotationsArchive, Path directionsArchive, Date uploadDate) {
            return new ZippedPhoneDataDeserializer(userId, measuresArchive, accelerationsArchive, rotationsArchive,
                    directionsArchive, uploadDate);
        }
    }

    /**
     * Interface for building Deserializers for uncompressed data.
     */
    public interface UncompressedCreator extends DeserializerFactory.Creator {
        /**
         * Create a new {@link Deserializer} for data directly exported from within the Cyface Android App. This
         * <code>Deserializer</code> is for data that has already been extracted to disk. Therefore, it expects sensor data
         * files as a list of multiple files (one per measurement and sensor data).
         *
         * @param userId            The id of the user who created the data to be deserialized by the created deserializer.
         *                          This information is lost during the export process and needs to be provided here
         * @param measuresDatabase  An SQLite database containing the location information
         * @param accelerationFiles The files containing the accelerations from the accelerometer for each measurement
         * @param rotationFiles     The files containing the rotations from the gyroscope for each measurement
         * @param directionFiles    The files containing the directions from the compass for each measurement
         * @param uploadDate        The upload date when the `Measurement`s were uploaded to the collector.
         * @return A {@link UnzippedPhoneDataDeserializer} to read measurements from an unzipped phone export
         */
        Deserializer create(
                final UUID userId,
                final Path measuresDatabase,
                final List<Path> accelerationFiles,
                final List<Path> rotationFiles,
                final List<Path> directionFiles,
                final Date uploadDate
        );
    }

    private static final class UncompressedImpl extends CreatorImpl implements UncompressedCreator {
        UncompressedImpl() {
            super(false);
        }
        @Override
        public UnzippedPhoneDataDeserializer create(
                final UUID userId,
                final Path measuresDatabase,
                final List<Path> accelerationFiles,
                final List<Path> rotationFiles,
                final List<Path> directionFiles,
                final Date uploadDate
        ) {
            return new UnzippedPhoneDataDeserializer(userId, measuresDatabase, accelerationFiles, rotationFiles, directionFiles, uploadDate);
        }
    }
}


