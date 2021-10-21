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

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import de.cyface.model.MeasurementIdentifier;

/**
 * A class containing shared utility methods applied to the SQLite database from a phone data export.
 *
 * @author Klemens Muthmann
 */
public final class SQLiteDatabaseParser {
    /**
     * An SQL query for the device identifier of the device that exported the database.
     */
    private static final String DEVICE_IDENTIFIER_QUERY = "SELECT device_id FROM identifiers LIMIT 1";
    /**
     * An SQL query for all <code>Measurement</code> numbers.
     */
    private static final String MEASUREMENTS_QUERY = "SELECT _id FROM measurements";

    /**
     * Private constructor to avoid instantiation of static utility class.
     */
    private SQLiteDatabaseParser() {
        // Nothing to do here
    }

    /**
     * Queries for all the {@link MeasurementIdentifier} from a local SQLite phone data export database
     *
     * @param databaseFile The file containing the SQLite database
     * @return The <code>MeasurementIdentifier</code> from the provided database
     */
    static List<MeasurementIdentifier> queryMeasurementIdentifierFrom(final Path databaseFile) {
        final var ret = new ArrayList<MeasurementIdentifier>();
        try (final var connection = createConnection(databaseFile)) {
            final var deviceIdentifierQuery = connection.createStatement();
            final var deviceIdentifierResultSet = deviceIdentifierQuery.executeQuery(DEVICE_IDENTIFIER_QUERY);
            deviceIdentifierResultSet.next();
            final var deviceIdentifier = deviceIdentifierResultSet.getString("device_id");

            final var measurementsQuery = connection.createStatement();
            final var measurementsResultSet = measurementsQuery.executeQuery(MEASUREMENTS_QUERY);
            while (measurementsResultSet.next()) {
                final var measurementNumber = measurementsResultSet.getLong("_id");
                ret.add(new MeasurementIdentifier(deviceIdentifier, measurementNumber));
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        return ret;
    }

    /**
     * Create a new JDBC <code>Connection</code> to a local SQLite database file.
     *
     * @param databaseFile The database file containing the SQLite database
     * @return A JDBC <code>Connection</code> to the provided database
     * @throws SQLException If the connection was not successful.
     */
    private static Connection createConnection(final Path databaseFile) throws SQLException {
        return DriverManager.getConnection(String.format("jdbc:sqlite:%s", databaseFile.toString()));
    }
}
