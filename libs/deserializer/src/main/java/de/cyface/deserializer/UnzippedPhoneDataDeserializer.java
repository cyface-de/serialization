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

import static de.cyface.serializer.model.Point3DType.ACCELERATION;
import static de.cyface.serializer.model.Point3DType.DIRECTION;
import static de.cyface.serializer.model.Point3DType.ROTATION;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.Validate;

import de.cyface.deserializer.exceptions.InvalidLifecycleEvents;
import de.cyface.deserializer.exceptions.NoSuchMeasurement;
import de.cyface.model.Event;
import de.cyface.model.Measurement;
import de.cyface.model.MeasurementIdentifier;
import de.cyface.model.MetaData;
import de.cyface.model.Modality;
import de.cyface.model.Point3DImpl;
import de.cyface.model.RawRecord;

/**
 * A {@link Deserializer} for phone data exports, that are already unzipped. Each such export consists of an SQLite file
 * containing some meta information, the captured geographic locations and user interaction events during capturing. In
 * addition, there are binary files with the
 * information from the individual <code>Measurement</code>s. There is one such file for each sensor and
 * <code>Measurement</code>. The sensors are the accelerometer (*.cyfa files), the gyroscope (*.cyfr files) and the
 * compass (*.cyfd files). The name of each file is the <code>Measurement</code> number, which can be used as a foreign
 * key into the database.
 * 
 * @author Klemens Muthmann
 * @version 1.0.2
 * @since 1.0.0
 */
public class UnzippedPhoneDataDeserializer extends PhoneDataDeserializer {
    /**
     * A query for loading a single <code>Measurement</code> from the SQLite database.
     */
    private static final String MEASUREMENT_QUERY = "SELECT * FROM measurements WHERE _id = ?";
    /**
     * A query to load all the geolocations for one <code>Measurement</code> from the database.
     */
    private static final String LOCATIONS_QUERY = "SELECT * FROM locations WHERE measurement_fk = ?";
    /**
     * A query to load all the events for a <code>Measurement</code> from the database.
     */
    private static final String EVENTS_QUERY = "SELECT * FROM events WHERE measurement_fk = ?";
    /**
     * A query to load the distance or length metadata for a single <code>Measurement</code>.
     */
    private static final String MEASUREMENT_LENGTH_QUERY = "SELECT distance FROM measurements WHERE _id = ?";
    /**
     * A query to load the device identifier from the database.
     */
    private static final String DEVICE_IDENTIFIER_QUERY = "SELECT device_id FROM identifiers LIMIT 1";
    /**
     * Used to serialize objects of this class. Only change this value if this objects attribute set changes.
     */
    private static final long serialVersionUID = -2195934964202856521L;
    /**
     * The path in the local file system to the SQLite database with the location and event information.
     */
    private final Path sqliteDatabasePath;
    /**
     * The files containing the accelerometer sensor data.
     */
    private final List<Path> accelerationsFilePaths;
    /**
     * The files containing the gyroscope sensor data.
     */
    private final List<Path> rotationsFilePaths;
    /**
     * The files containing the compass sensor data.
     */
    private final List<Path> directionsFilePaths;
    /**
     * The user id used to identify the deserialized information. This is lost during export. It does not matter to use
     * the correct one here, but a user id is often necessary for further processing steps.
     */
    private final UUID userId;

    /**
     * Create a new {@link Deserializer} for phone data. Before calling read on an instance of this class
     * {@link #setMeasurementNumber(long)} must have been called with a valid number. The valid numbers are available
     * via {@link #peakIntoDatabase()}.
     * 
     * @param userId The user id used to identify the deserialized information. This is lost during export. It does
     *            not matter to use the correct one here, but a user id is often necessary for further processing
     *            steps
     * @param sqliteDatabasePath The path in the local file system to the SQLite database with the location and event
     *            information
     * @param accelerationsFilePaths The files containing the accelerometer sensor data
     * @param rotationsFilePaths The files containing the gyroscope sensor data
     * @param directionsFilePaths The files containing the compass sensor data
     */
    UnzippedPhoneDataDeserializer(final UUID userId, final Path sqliteDatabasePath,
                                  final List<Path> accelerationsFilePaths,
                                  final List<Path> rotationsFilePaths, final List<Path> directionsFilePaths) {
        Validate.isTrue(Files.exists(sqliteDatabasePath));
        Validate.isTrue(accelerationsFilePaths.stream().map(Files::exists)
                .reduce((first, second) -> first || second).orElseThrow());
        Validate.isTrue(rotationsFilePaths.stream().map(Files::exists)
                .reduce((first, second) -> first || second).orElseThrow());
        Validate.isTrue(directionsFilePaths.stream().map(Files::exists)
                .reduce((first, second) -> first || second).orElseThrow());
        Validate.notNull(userId);

        this.sqliteDatabasePath = sqliteDatabasePath;
        this.accelerationsFilePaths = accelerationsFilePaths;
        this.rotationsFilePaths = rotationsFilePaths;
        this.directionsFilePaths = directionsFilePaths;
        this.userId = userId;
    }

    @Override
    public Measurement read() throws InvalidLifecycleEvents, NoSuchMeasurement {
        try (final var connection = createConnection()) {
            PreparedStatement measurementExistsStatement = connection.prepareStatement(MEASUREMENT_QUERY);
            measurementExistsStatement.setLong(1, measurementNumber);
            final var measurementExistsResultSet = measurementExistsStatement.executeQuery();
            // Check that there is exactly one result
            if (!measurementExistsResultSet.next() || measurementExistsResultSet.next()) {
                throw new NoSuchMeasurement();
            }

            final var metaData = queryForMetaData(connection, measurementNumber);
            final var locations = queryForLocations(connection, metaData.getIdentifier());
            final var events = queryForEvents(connection, metaData.getIdentifier());

            final var accelerations = readBinaryData(accelerationsFilePaths, measurementNumber);
            final var rotations = readBinaryData(rotationsFilePaths, measurementNumber);
            final var directions = readBinaryData(directionsFilePaths, measurementNumber);

            final var trackBuilder = new TrackBuilder();
            final var tracks = trackBuilder.build(locations, events, accelerations, rotations, directions);

            return new Measurement(metaData, tracks);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @return A list with all the valid <code>{@link MeasurementIdentifier}</code> within the deserializable data.
     */
    public List<MeasurementIdentifier> peakIntoDatabase() {
        return SQLiteDatabaseParser.queryMeasurementIdentifierFrom(sqliteDatabasePath);
    }

    /**
     * @return A valid <code>Connection</code> to the SQLite database from {@link #sqliteDatabasePath}
     * @throws SQLException If the <code>Connection</code> was not successfully established
     */
    private Connection createConnection() throws SQLException {
        return DriverManager.getConnection(String.format("jdbc:sqlite:%s", sqliteDatabasePath.toString()));
    }

    /**
     * Query the SQLite database for the location records from one <code>Measurement</code>.
     *
     * @param connection A JDBC <code>Connection</code> to the SQLite database at {@link #sqliteDatabasePath}
     * @param measurementIdentifier The identifier of the <code>Measurement</code> to load locations for
     * @return A <code>List</code> of all the location records for the <code>Measurement</code> ordered by their
     *         timestamp
     * @throws SQLException If the query was not successful
     */
    private List<RawRecord> queryForLocations(final Connection connection,
            final MeasurementIdentifier measurementIdentifier) throws SQLException {
        final var locationsQuery = connection.prepareStatement(LOCATIONS_QUERY);
        locationsQuery.setLong(1, measurementIdentifier.getMeasurementIdentifier());
        final var locationsResultSet = locationsQuery.executeQuery();

        final var ret = new ArrayList<RawRecord>();

        while (locationsResultSet.next()) {
            final var timestamp = locationsResultSet.getLong("gps_time");
            final var latitude = locationsResultSet.getDouble("lat");
            final var longitude = locationsResultSet.getDouble("lon");
            final var accuracy = locationsResultSet.getDouble("accuracy");
            final var speed = locationsResultSet.getDouble("speed");
            ret.add(new RawRecord(measurementIdentifier, timestamp, latitude, longitude, null, accuracy, speed,
                    Modality.UNKNOWN));
        }
        Collections.sort(ret);

        return ret;
    }

    /**
     * Query the SQLite database for the user interaction events that occurred during <code>Measurement</code>.
     * 
     * @param connection A JDBC <code>Connection</code> to the SQLite database at {@link #sqliteDatabasePath}
     * @param measurementIdentifier The identifier of the <code>Measurement</code> to load locations for
     * @return A <code>List</code> of <code>Event</code> objects with all the user interaction events during the
     *         <code>Measurement</code> ordered by timestamp
     * @throws SQLException If the query was not successful
     */
    private List<Event> queryForEvents(final Connection connection, final MeasurementIdentifier measurementIdentifier)
            throws SQLException {
        final var eventsQuery = connection.prepareStatement(EVENTS_QUERY);
        eventsQuery.setLong(1, measurementIdentifier.getMeasurementIdentifier());
        final var eventsResultSet = eventsQuery.executeQuery();

        final var ret = new ArrayList<Event>();

        while (eventsResultSet.next()) {
            final var type = Event.EventType.valueOf(eventsResultSet.getString("type"));
            final var timestamp = eventsResultSet.getLong("timestamp");
            try {
                final var value = eventsResultSet.getString("value");
                ret.add(new Event(type, timestamp, value));
            } catch (SQLException e) {
                ret.add(new Event(type, timestamp, null));
            }
        }

        return ret;
    }

    /**
     * Query the SQLite database for the metadata of the requested <code>Measurement</code>.
     * 
     * @param connection A JDBC <code>Connection</code> to the SQLite database at {@link #sqliteDatabasePath}
     * @param measurementNumber The number of the <code>Measurement</code> to read the data for
     * @return The metadata for the requested <code>Measurement</code> all fields not stored in the data are set with
     *         default values
     * @throws SQLException If the query was not successful
     */
    private MetaData queryForMetaData(final Connection connection, final long measurementNumber) throws SQLException {
        final var deviceIdentifierQuery = connection.prepareStatement(DEVICE_IDENTIFIER_QUERY);

        final var deviceIdentifierResultSet = deviceIdentifierQuery.executeQuery();
        deviceIdentifierResultSet.next();
        final var deviceIdentifier = deviceIdentifierResultSet.getString(1);
        final var measurementIdentifier = new MeasurementIdentifier(deviceIdentifier, measurementNumber);
        final var deviceType = "data export";
        final var osVersion = "0";
        final var appVersion = "0";
        final var lengthQuery = connection.prepareStatement(MEASUREMENT_LENGTH_QUERY);
        lengthQuery.setLong(1, measurementNumber);
        final var lengthResultSet = lengthQuery.executeQuery();
        lengthResultSet.next();
        final var length = lengthResultSet.getDouble(1);
        return new MetaData(measurementIdentifier, deviceType, osVersion, appVersion, length, userId, MetaData.CURRENT_VERSION);
    }

    /**
     * Reads sensor values from a binary data file.
     * 
     * @param paths The paths containing the binary data
     * @param measurementNumber The number of the <code>Measurement</code> to read the data for
     * @return A <code>List</code> of 3D data points ordered by timestamp for the requested <code>Measurement</code>
     */
    private List<Point3DImpl> readBinaryData(final List<Path> paths, final long measurementNumber) {
        return paths.stream()
                .filter(path -> path.getFileName().toString().startsWith(String.valueOf(measurementNumber)))
                .findFirst().stream()
                .map(path -> {
                    try (final var stream = Files.newInputStream(path)) {

                        final var fileName = path.getFileName().toString(); // path.endsWith() does not work
                        final var type = fileName.endsWith(".cyfa") ? ACCELERATION
                                : fileName.endsWith(".cyfr") ? ROTATION
                                        : fileName.endsWith(".cyfd") ? DIRECTION : null;
                        Validate.notNull(type, String.format("Unknown point3d type for path: %s", path));
                        final var measurement = BinaryFormatParser.readPoint3Ds(stream, type);

                        switch (type) {
                            case ACCELERATION:
                                return Point3DDeserializer
                                        .accelerations(measurement.getAccelerationsBinary().getAccelerationsList());
                            case ROTATION:
                                return Point3DDeserializer.rotations(measurement.getRotationsBinary().getRotationsList());
                            case DIRECTION:
                                return Point3DDeserializer
                                        .directions(measurement.getDirectionsBinary().getDirectionsList());
                            default:
                                throw new IllegalArgumentException(String.format("Unknown type: %s", type));
                        }
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                })
                .findFirst()
                .orElse(Collections.emptyList());
    }
}
