/*
 * Copyright 2020-2022 Cyface GmbH
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.lang3.Validate;

import de.cyface.deserializer.exceptions.InvalidLifecycleEvents;
import de.cyface.deserializer.exceptions.NoSuchMeasurement;
import de.cyface.model.Measurement;
import de.cyface.model.MeasurementIdentifier;

/**
 * A {@link Deserializer} for zipped phone data. This is the format as it is returned by the smartphone. This data comes
 * in the form of four zip archives. The first contains an SQLite database with the location information and some
 * metadata. The other three contain the sensor data from the accelerometer, the gyroscope and the compass.
 * 
 * @author Klemens Muthmann
 * @version 1.0.1
 * @since 1.0.0
 */
public class ZippedPhoneDataDeserializer extends PhoneDataDeserializer {

    /**
     * The archive containing the SQLite database with the location data.
     */
    private final Path sqliteDatabasePath;
    /**
     * The archive containing the accelerations from the accelerometer.
     */
    private final Path accelerationsFilePath;
    /**
     * The archive containing rotations from the gyroscope.
     */
    private final Path rotationsFilePath;
    /**
     * The archive containing directions from the compass.
     */
    private final Path directionsFilePath;
    /**
     * A flag indicating, whether the data has been unzipped or not. This is used to avoid unzipping on each call to
     * {@link #read()}.
     */
    private boolean isUnzipped;
    /**
     * The userId to identify the deserialized information. This is lost during export. It is not necessary
     * to use the correct one here, but a userId is often necessary for further processing steps
     */
    private final UUID userId;
    /**
     * The path in the local file system to the SQLite database with the location and event
     * information
     */
    private Path databaseFile;
    /**
     * The files containing the accelerometer sensor data
     */
    private List<Path> accelerationPaths;
    /**
     * The files containing the gyroscope sensor data
     */
    private List<Path> rotationPaths;
    /**
     * The files containing the compass sensor data
     */
    private List<Path> directionPaths;
    /**
     * The upload date when the `Measurement`s were uploaded to the collector.
     */
    private Date uploadDate;

    /**
     * Create a new {@link Deserializer} for phone data. Before calling read on an instance of this class
     * * {@link #setMeasurementNumber(long)} must have been called with a valid number. The valid numbers are available
     * * via {@link #peakIntoDatabase()}.
     *
     * @param userId The userId to identify the deserialized information. This is lost during export. It is not necessary
     *            to use the correct one here, but a userId is often necessary for further processing steps
     * @param sqliteDatabasePath The archive containing the SQLite database with the location data
     * @param accelerationsFilePath The archive containing the accelerations from the accelerometer
     * @param rotationsFilePath The archive containing rotations from the gyroscope
     * @param directionsFilePath The archive containing directions from the compass
     * @param uploadDate The upload date when the `Measurement`s were uploaded to the collector.
     */
    ZippedPhoneDataDeserializer(final UUID userId, final Path sqliteDatabasePath, final Path accelerationsFilePath,
            final Path rotationsFilePath, final Path directionsFilePath, final Date uploadDate) {
        Validate.isTrue(Files.exists(sqliteDatabasePath));
        Validate.isTrue(Files.exists(accelerationsFilePath));
        Validate.isTrue(Files.exists(rotationsFilePath));
        Validate.isTrue(Files.exists(directionsFilePath));
        Validate.notNull(userId);

        this.sqliteDatabasePath = sqliteDatabasePath;
        this.accelerationsFilePath = accelerationsFilePath;
        this.rotationsFilePath = rotationsFilePath;
        this.directionsFilePath = directionsFilePath;
        this.isUnzipped = false;
        this.userId = userId;
        this.uploadDate = uploadDate;
    }

    @Override
    public Measurement read() throws IOException, InvalidLifecycleEvents, NoSuchMeasurement {
        if (!isUnzipped) {
            this.databaseFile = unzipAndReturnMatching(sqliteDatabasePath, "/measures");
            this.accelerationPaths = unzip(accelerationsFilePath);
            this.rotationPaths = unzip(rotationsFilePath);
            this.directionPaths = unzip(directionsFilePath);
            isUnzipped = true;
        }
        final var phoneDataDeserializer = new UnzippedPhoneDataDeserializer(userId, databaseFile, accelerationPaths,
                rotationPaths, directionPaths, uploadDate);
        phoneDataDeserializer.setMeasurementNumber(measurementNumber);

        return phoneDataDeserializer.read();
    }

    @Override
    public List<MeasurementIdentifier> peakIntoDatabase() throws IOException {
        final var unzippedDatabasePath = unzipAndReturnMatching(sqliteDatabasePath, "/measures");
        return SQLiteDatabaseParser.queryMeasurementIdentifierFrom(unzippedDatabasePath);
    }

    /**
     * Unzip an archive to a local temporary location
     *
     * @param zipFile A path to a zipped file
     * @return The path to all the unzipped entries
     * @throws IOException If unzipping the data fails
     */
    private List<Path> unzip(final Path zipFile) throws IOException {
        final var ret = new ArrayList<Path>();
        unzip(zipFile, (unzippedPath, zipEntry) -> ret.add(unzippedPath));
        return ret;
    }

    /**
     * Unzip a single entry from an archive to a local temporary location
     *
     * @param zipFile A path to a zipped file
     * @param nameOfZipFileEntryToReturn The file or folder from within the zip file to return
     * @return The path to the unzipped entry
     * @throws IOException If unzipping the data fails
     */
    private Path unzipAndReturnMatching(final Path zipFile,
            @SuppressWarnings("SameParameterValue") final String nameOfZipFileEntryToReturn)
            throws IOException {
        final var onUnzippedAction = new BiConsumer<Path, ZipEntry>() {

            Path value;

            @Override
            public void accept(final Path path, final ZipEntry zipEntry) {
                if (zipEntry.getName().equals(nameOfZipFileEntryToReturn)) {
                    value = path;
                }
            }
        };

        unzip(zipFile, onUnzippedAction);
        Validate.notNull(onUnzippedAction.value);

        return onUnzippedAction.value;
    }

    /**
     * Unzips an archive and allows carrying out some operation on each entry from the archive.
     * 
     * @param zipFile The zip archive
     * @param onUnzipped The operation to carry out on each entry from the archive. The first parameter is the path to
     *            the temporary location of the unzipped entry, while the second is the information about the entry
     *            itself
     * @throws IOException If unzipping the data fails
     */
    private void unzip(final Path zipFile, final BiConsumer<Path, ZipEntry> onUnzipped) throws IOException {
        byte[] buffer = new byte[1024];

        try (final var inputStream = Files.newInputStream(zipFile);
                final var zis = new ZipInputStream(inputStream)) {
            var entry = zis.getNextEntry();
            while (entry != null) {

                final var path = Paths.get(entry.getName());
                final var fileName = path.getFileName().toString(); // path.endsWith() does not work
                final var fileExtension = fileName.endsWith(".cyfa") ? ".cyfa"
                        : fileName.endsWith(".cyfr") ? ".cyfr" : fileName.endsWith(".cyfd") ? ".cyfd" : ".tmp";
                final var tempPath = Files.createTempFile(path.getFileName().toString(), fileExtension);
                try (final var fos = Files.newOutputStream(tempPath)) {
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }
                onUnzipped.accept(tempPath, entry);

                zis.closeEntry();
                entry = zis.getNextEntry();
            }
        }
    }
}
