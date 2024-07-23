/*
 * Copyright 2019-2024 Cyface GmbH
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

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Serializable
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A model POJO representing a geographical location measured in latitude and longitude. Typically on earth, but this
 * would work on other planets too, as soon as we require road maintenance there.
 *
 * A record of such a location is represented by a [GeoLocationRecord].
 *
 * @author Armin Schnabel
 * @author Klemens Muthmann
 * @property latitude Geographical latitude in coordinates (decimal fraction) raging from -90° (south) to 90° (north)
 * @property longitude Geographical longitude in coordinates (decimal fraction) ranging from -180° (west) to 180° (east)
 */
open class GeoLocation : Serializable {
    /**
     * Geographical latitude in coordinates (decimal fraction) raging from -90° (south) to 90° (north).
     */
    @Suppress("MagicNumber")
    var latitude: Double = 0.0
        set(value) {
            if (value < -90.0 || value > 90.0) {
                LOGGER.warn("Setting latitude to invalid value: {}", value)
            }
            field = value
        }

    /**
     * Geographical longitude in coordinates (decimal fraction) ranging from -180° (west) to 180°
     * (east).
     */
    @Suppress("MagicNumber")
    var longitude: Double = 0.0
        set(value) {
            if (value < -180.0 || value > 180.0) {
                LOGGER.warn("Setting longitude to invalid value: {}", value)
            }
            field = value
        }

    /**
     * Elevation above sea level in meters or null if it could not be calculated.
     */
    @JvmField
    var elevation: Double? = null

    /**
     * No argument constructor as required by Apache Flink. Do not use this in your own code.
     */
    constructor()

    /**
     * Creates a new completely initialized [GeoLocation] with not altitude information.
     *
     * @param latitude Geographical latitude in coordinates (decimal fraction) raging from -90° (south) to 90° (north)
     * @param longitude Geographical longitude in coordinates (decimal fraction) ranging from -180° (west) to 180°
     * (east)
     */
    @JvmOverloads
    constructor(latitude: Double, longitude: Double, elevation: Double? = null) {
        this.latitude = latitude
        this.longitude = longitude
        this.elevation = elevation
    }

    /**
     * Calculates the distance from this geolocations to another one based on their latitude and longitude. This simple
     * formula assumes the earth is a perfect sphere. As the earth is a spheroid instead, the result can be inaccurate,
     * especially for longer distances.
     *
     * Source: https://stackoverflow.com/a/27943/5815054
     *
     * @param location The location to calculate the distance to
     * @return the estimated distance between both locations in meters
     */
    fun distanceTo(location: GeoLocation): Double {
        @SuppressWarnings("MagicNumber")
        val earthRadiusInMeters = 6371000
        val latitudeDifferenceRad = degreeToRad(location.latitude - latitude)
        val longitudeDifferenceRad = degreeToRad(location.longitude - longitude)
        val a = sin(latitudeDifferenceRad / 2) * sin(latitudeDifferenceRad / 2)+ cos(degreeToRad(latitude)) * cos(
            degreeToRad(location.latitude)
        ) * sin(
            longitudeDifferenceRad / 2
        ) * sin(longitudeDifferenceRad / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusInMeters * c
    }

    /**
     * Converts a degree value to the "rad" unit.
     *
     *
     * Source: https://stackoverflow.com/a/27943/5815054
     *
     * @param degree the value to be converted in the degree unit
     * @return the value in the rad unit
     */
    @SuppressWarnings("MagicNumber")
    private fun degreeToRad(degree: Double): Double {
        return degree * (Math.PI / 180)
    }

    override fun toString(): String {
        return "GeoLocation{latitude=$latitude, longitude=$longitude, elevation=$elevation}"
    }

    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + (if ((elevation == null)) 0 else elevation.hashCode())
        var temp = java.lang.Double.doubleToLongBits(latitude)
        result = prime * result + (temp xor (temp ushr 32)).toInt()
        temp = java.lang.Double.doubleToLongBits(longitude)
        result = prime * result + (temp xor (temp ushr 32)).toInt()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null) {
            return false
        }
        if (javaClass != other.javaClass) {
            return false
        }
        val geolocation = other as GeoLocation
        if (elevation == null) {
            if (geolocation.elevation != null) {
                return false
            }
        } else if (elevation!! != geolocation.elevation) {
            return false
        }
        if (java.lang.Double.doubleToLongBits(latitude) != java.lang.Double.doubleToLongBits(geolocation.latitude)) {
            return false
        }
        return java.lang.Double.doubleToLongBits(longitude) == java.lang.Double.doubleToLongBits(geolocation.longitude)
    }

    companion object {
        /**
         * Used to serialize objects of this class. Only change this value if this classes attribute set changes.
         */
        @Suppress("ConstPropertyName")
        private const val serialVersionUID = 4613514835798881192L

        /**
         * The logger used by instances of this class. Configure it using <tt>src/main/resources/logback.xml</tt>.
         */
        private val LOGGER: Logger = LoggerFactory.getLogger(GeoLocation::class.java)
    }
}
