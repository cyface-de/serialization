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
package de.cyface.model

import java.io.Serializable
import java.util.Collections
import java.util.Objects

/**
 * A part of a measurement for which continuous data is available and ordered by time.
 *
 * A `Track` begins with the first `GeoLocation` collected after start or resume was triggered during data
 * collection. It stops with the last collected `GeoLocation` before the next resume command was triggered or
 * when the very last locations is reached.
 *
 * @author Armin Schnabel
 * @version 2.1.0
 * @since 1.0.0
 */
class Track : Serializable {
    /**
     * The list of `GeoLocationRecord`s collected for this `Track` ordered by timestamp.
     */
    private lateinit var locationRecords: MutableList<RawRecord>

    /**
     * The list of accelerations for this `Track` ordered by timestamp. Unit: m/s².
     */
    private lateinit var accelerations: MutableList<Point3DImpl>

    /**
     * The list of rotations for this `Track` ordered by timestamp. Unit: rad/s.
     */
    private lateinit var rotations: MutableList<Point3DImpl>

    /**
     * The list of directions for this `Track` ordered by timestamp. Unit. micro-Tesla (uT).
     */
    private lateinit var directions: MutableList<Point3DImpl>

    /**
     * Creates a new completely initialized `Track`.
     *
     * @param locationRecords The list of `RawRecord`s collected for this `Track` ordered by
     * timestamp.
     * @param accelerations The list of accelerations for this `Track` ordered by timestamp. Unit: m/s².
     * @param rotations The list of rotations for this `Track` ordered by timestamp. Unit: rad/s.
     * @param directions The list of directions for this `Track` ordered by timestamp. Unit. micro-Tesla (uT).
     */
    constructor(
        locationRecords: List<RawRecord>, accelerations: List<Point3DImpl>,
        rotations: List<Point3DImpl>, directions: List<Point3DImpl>
    ) {
        Objects.requireNonNull(locationRecords)
        Objects.requireNonNull(accelerations)
        Objects.requireNonNull(rotations)
        Objects.requireNonNull(directions)
        this.locationRecords = ArrayList(locationRecords)
        this.accelerations = ArrayList(accelerations)
        this.rotations = ArrayList(rotations)
        this.directions = ArrayList(directions)
    }

    /**
     * No argument constructor as required by Apache Flink. Do not use this in your own code.
     */
    constructor() {
        // Nothing to do
    }

    /**
     * @return The list of `GeoLocationRecord`s collected for this `Track` ordered by timestamp.
     */
    fun getLocationRecords(): List<RawRecord> {
        return Collections.unmodifiableList(locationRecords)
    }

    /**
     * @return The list of accelerations for this `Track` ordered by timestamp. Unit: m/s².
     */
    fun getAccelerations(): List<Point3DImpl> {
        return Collections.unmodifiableList(accelerations)
    }

    /**
     * @return The list of accelerations for this `Track` ordered by timestamp. Unit: m/s².
     */
    fun getRotations(): List<Point3DImpl> {
        return Collections.unmodifiableList(rotations)
    }

    /**
     * @return The list of directions for this `Track` ordered by timestamp. Unit. micro-Tesla (uT).
     */
    fun getDirections(): List<Point3DImpl> {
        return Collections.unmodifiableList(directions)
    }

    /**
     * Required by Apache Flink.
     *
     * @param locationRecords The list of `GeoLocationRecord`s collected for this `Track` ordered by
     * timestamp.
     */
    fun setLocationRecords(locationRecords: List<RawRecord>) {
        Objects.requireNonNull(locationRecords)
        this.locationRecords = ArrayList(locationRecords)
    }

    /**
     * Required by Apache Flink.
     *
     * @param accelerations The list of accelerations for this `Track` ordered by timestamp. Unit: m/s².
     */
    fun setAccelerations(accelerations: List<Point3DImpl>) {
        Objects.requireNonNull(locationRecords)
        this.accelerations = ArrayList(accelerations)
    }

    /**
     * Required by Apache Flink.
     *
     * @param rotations The list of accelerations for this `Track` ordered by timestamp. Unit: m/s².
     */
    fun setRotations(rotations: List<Point3DImpl>) {
        Objects.requireNonNull(locationRecords)
        this.rotations = ArrayList(rotations)
    }

    /**
     * Required by Apache Flink.
     *
     * @param directions The list of directions for this `Track` ordered by timestamp. Unit. micro-Tesla (uT).
     */
    fun setDirections(directions: List<Point3DImpl>) {
        Objects.requireNonNull(locationRecords)
        this.directions = ArrayList(directions)
    }

    /**
     * Removes all data after the provided `timestamp` from this `Track`.
     *
     * @param timestamp A UNIX timestamp in milliseconds since the first of January 1970
     * @return This track for method chaining
     */
    fun clearAfter(timestamp: Long): Track {
        locationRecords.removeIf { it.timestamp < timestamp }
        accelerations.removeIf { it.timestamp <= timestamp }
        rotations.removeIf { it.timestamp <= timestamp }
        directions.removeIf { it.timestamp <= timestamp }
        return this
    }

    /**
     * Removes a specific location and all sensor data that fall between this location and the previous valid location.
     *
     * @param location The location to remove.
     * @return This track for method chaining.
     */
    fun clearFor(location: RawRecord): Track {
        // Find the index of the location to remove
        val locationIndex = locationRecords.indexOf(location)
        if (locationIndex == -1) throw IllegalStateException("Location not found: $location") //return this

        // Determine the timestamp range for which sensor data should be removed
        val startTimestamp = if (locationIndex > 0) locationRecords[locationIndex - 1].timestamp else 0L
        val endTimestamp = location.timestamp

        // Remove the location
        locationRecords.remove(location)

        // Remove sensor data that falls within the determined timestamp range
        accelerations.removeIf { it.timestamp in (startTimestamp until endTimestamp) }
        rotations.removeIf { it.timestamp in (startTimestamp until endTimestamp) }
        directions.removeIf { it.timestamp in (startTimestamp until endTimestamp) }

        return this
    }


    override fun toString(): String {
        return "Track{" +
                "geoLocations=" + locationRecords +
                ", accelerations=" + accelerations +
                ", rotations=" + rotations +
                ", directions=" + directions +
                '}'
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val track = other as Track
        return locationRecords == track.locationRecords && accelerations == track.accelerations && rotations == track.rotations && directions == track.directions
    }

    override fun hashCode(): Int {
        return Objects.hash(locationRecords, accelerations, rotations, directions)
    }

    companion object {
        /**
         * Used to serialize objects of this class. Only change this value if this classes attribute set changes.
         */
        private const val serialVersionUID = 5614152745515907995L
    }
}