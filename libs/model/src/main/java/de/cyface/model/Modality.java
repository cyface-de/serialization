/*
 * Copyright 2019-2026 Cyface GmbH
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
package de.cyface.model;

import java.util.HashMap;
import java.util.Map;

/**
 * The {@link Modality} types to choose from when starting a {@link Measurement}. This class maps the database values to
 * enum values, to make sure the correct values are used within the Java code.
 * <p>
 * Some historic database identifiers are accepted as <em>read aliases</em> via
 * {@link #forDatabaseIdentifier(String)} but are normalised to the canonical constant on lookup:
 * {@code "Bike"} → {@link #BICYCLE}, {@code "Other"} → {@link #UNKNOWN}.
 *
 * @author Armin Schnabel
 * @author Klemens Muthmann
 */
public enum Modality {
    /**
     * This is used if a bicycle was used to capture a measurement.
     */
    BICYCLE("BICYCLE"),
    /**
     * This is used if an electrically assisted bicycle (e-bike or pedelec) was used to capture a measurement.
     */
    EBIKE("EBike"),
    /**
     * This is used if a cargo bicycle was used to capture a measurement.
     */
    CARGO_BIKE("CargoBike"),
    /**
     * This is used if an electrically assisted cargo bicycle was used to capture a measurement.
     */
    ECARGO_BIKE("ECargoBike"),
    /**
     * This is used if a car was used to capture a measurement.
     */
    CAR("CAR"),
    /**
     * This is used if a motorbike was used to capture a measurement.
     */
    MOTORBIKE("MOTORBIKE"),
    /**
     * This is used if a bus was used to capture a measurement.
     */
    BUS("BUS"),
    /**
     * This is used if a train was used to capture a measurement.
     */
    TRAIN("TRAIN"),
    /**
     * This is used if the user walked while capturing a measurement.
     */
    WALKING("WALKING"),
    /**
     * This is used if the modality which was used to capture a measurement is not known.
     */
    UNKNOWN("UNKNOWN");

    /**
     * Lookup table mapping every accepted database identifier (canonical and legacy aliases) to its
     * canonical {@link Modality} constant. Precomputed once to avoid the per-call allocation of
     * {@link #values()} and a linear scan in {@link #forDatabaseIdentifier(String)}.
     * <p>
     * Legacy aliases ({@code "Bike"} → {@link #BICYCLE}, {@code "Other"} → {@link #UNKNOWN}) are
     * accepted on read but never written; they exist only for backwards-compatible deserialization.
     */
    private static final Map<String, Modality> BY_DATABASE_IDENTIFIER;

    static {
        final Map<String, Modality> map = new HashMap<>();
        for (final Modality modality : values()) {
            map.put(modality.databaseIdentifier, modality);
        }
        map.put("Bike", BICYCLE);
        map.put("Other", UNKNOWN);
        BY_DATABASE_IDENTIFIER = Map.copyOf(map);
    }

    /**
     * The modality types identifier within the database.
     */
    private final String databaseIdentifier;

    /**
     * Creates a new enumeration case for a <code>Modality</code>. This constructor is only used internally and thus not
     * visible outside of this package.
     *
     * @param databaseIdentifier The modality types identifier within the database
     */
    Modality(final String databaseIdentifier) {
        this.databaseIdentifier = databaseIdentifier;
    }

    /**
     * Returns the string identifier used to persist this modality type in the database.
     *
     * @return The modality types identifier within the database
     */
    public String getDatabaseIdentifier() {
        return databaseIdentifier;
    }

    /**
     * Resolves a {@link Modality} by its {@link #getDatabaseIdentifier() database identifier}.
     * <p>
     * Unlike {@link #valueOf(String)}, which expects the enum constant name, this method matches the
     * identifier under which the modality is persisted (e.g. {@code "EBike"} for {@link #EBIKE}).
     * <p>
     * Historic aliases are accepted and normalised to their canonical constant:
     * {@code "Bike"} → {@link #BICYCLE}, {@code "Other"} → {@link #UNKNOWN}.
     *
     * @param databaseIdentifier The database identifier to resolve.
     * @return The matching {@code Modality}.
     */
    public static Modality forDatabaseIdentifier(final String databaseIdentifier) {
        if (databaseIdentifier == null) {
            return UNKNOWN;
        }
        final Modality modality = BY_DATABASE_IDENTIFIER.get(databaseIdentifier);
        if (modality == null) {
            return UNKNOWN;
        }
        return modality;
    }
}
