/*
 * Copyright 2021-2024 Cyface GmbH
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

import org.apache.commons.lang3.Validate
import java.io.Serializable

/**
 * Describes the worldwide unique identifier of a measurement. This identifier consists of a device identifier and the
 * actual measurement from that device.
 *
 * The natural order of [MeasurementIdentifier] is lexicographically by device identifier and then by
 * measurement identifier.
 *
 * @author Klemens Muthmann
 */
class MeasurementIdentifier : Comparable<MeasurementIdentifier>, Serializable {
    /**
     * The worldwide unique identifier of the device that captured this measurement.
     */
    var deviceIdentifier: String? = null
        set (value) {
            Validate.notEmpty(value)
            field = value
        }

    /**
     * The device wide unique identifier of the measurement.
     */
    var measurementIdentifier: Long = 0
        set(value) {
            Validate.isTrue(value >= 0)
            field = value
        }

    /**
     * The default no arguments constructor as required by Apache Flink to serialize and deserialize objects of this
     * class. Do not use this in you own code, it creates an unusable [MeasurementIdentifier].
     */
    constructor()

    /**
     * Creates a new completely initialized object of this class.
     *
     * @param deviceIdentifier The worldwide unique identifier of the device that captured this measurement
     * @param measurementIdentifier The device wide unique identifier of the measurement
     */
    constructor(deviceIdentifier: String?, measurementIdentifier: Long) {
        this.deviceIdentifier = deviceIdentifier
        this.measurementIdentifier = measurementIdentifier
    }

    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + (if ((deviceIdentifier == null)) 0 else deviceIdentifier.hashCode())
        result = prime * result + (measurementIdentifier xor (measurementIdentifier ushr 32)).toInt()
        return result
    }

    override fun toString(): String {
        return ("MeasurementIdentifier [deviceIdentifier=" + deviceIdentifier + ", measurementIdentifier="
                + measurementIdentifier + "]")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (javaClass != other.javaClass) return false
        val otherId = other as MeasurementIdentifier
        if (deviceIdentifier == null) {
            if (otherId.deviceIdentifier != null) return false
        } else if (deviceIdentifier != otherId.deviceIdentifier) return false
        return measurementIdentifier == otherId.measurementIdentifier
    }

    override fun compareTo(other: MeasurementIdentifier): Int {
        val deviceIdentifierComparison = deviceIdentifier!!
            .compareTo(other.deviceIdentifier!!)
        return if (deviceIdentifierComparison == 0) this.measurementIdentifier
            .compareTo(other.measurementIdentifier)
        else deviceIdentifierComparison
    }

    companion object {
        /**
         * Used to serialize objects of this class. Only change this value if this classes attribute set changes.
         */
        @Suppress("ConstPropertyName")
        private const val serialVersionUID = 181303400330020850L
    }
}
