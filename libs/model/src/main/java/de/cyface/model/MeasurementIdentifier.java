/*
 * Copyright 2021 Cyface GmbH
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

import org.apache.commons.lang3.Validate;

import java.io.Serializable;

/**
 * Describes the world wide unique identifier of a measurement. This identifier consists of a device identifier and the
 * actual measurement from that device.
 * <p>
 * The natural order of <code>MeasurementIdentifier</code> is lexicographically by device identifier and then by
 * measurement identifier.
 * 
 * @author Klemens Muthmann
 * @version 1.0.0
 * @since 2.0.0
 */
public final class MeasurementIdentifier implements Comparable<MeasurementIdentifier>, Serializable {

    /**
     * Used to serialize objects of this class. Only change this value if this classes attribute set changes.
     */
    private static final long serialVersionUID = 181303400330020850L;
    /**
     * The world wide unique identifier of the device that captured this measurement.
     */
    private String deviceIdentifier;
    /**
     * The device wide unique identifier of the measurement.
     */
    private long measurementIdentifier;

    /**
     * The default no arguments constructor as required by Apache Flink to serialize and deserialize objects of this
     * class. Do not use this in you own code, it creates an unusable <code>MeasurementIdentifier</code>.
     */
    public MeasurementIdentifier() {
        // Nothing to do here.
    }

    /**
     * Creates a new completely initialized object of this class.
     * 
     * @param deviceIdentifier The world wide unique identifier of the device that captured this measurement
     * @param measurementIdentifier The device wide unique identifier of the measurement
     */
    public MeasurementIdentifier(final String deviceIdentifier, final long measurementIdentifier) {
        setDeviceIdentifier(deviceIdentifier);
        setMeasurementIdentifier(measurementIdentifier);
    }

    /**
     * @return The world wide unique identifier of the device that captured this measurement
     */
    public String getDeviceIdentifier() {
        return deviceIdentifier;
    }

    /**
     * @return The device wide unique identifier of the measurement
     */
    public long getMeasurementIdentifier() {
        return measurementIdentifier;
    }

    /**
     * @param deviceIdentifier The world wide unique identifier of the device that captured this measurement
     */
    public void setDeviceIdentifier(final String deviceIdentifier) {
        Validate.notEmpty(deviceIdentifier);

        this.deviceIdentifier = deviceIdentifier;
    }

    /**
     * @param measurementIdentifier The device wide unique identifier of the measurement
     */
    public void setMeasurementIdentifier(final long measurementIdentifier) {
        Validate.isTrue(measurementIdentifier >= 0);

        this.measurementIdentifier = measurementIdentifier;
    }

    @Override
    public int hashCode() {
        final var prime = 31;
        int result = 1;
        result = prime * result + ((deviceIdentifier == null) ? 0 : deviceIdentifier.hashCode());
        result = prime * result + (int)(measurementIdentifier ^ (measurementIdentifier >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "MeasurementIdentifier [deviceIdentifier=" + deviceIdentifier + ", measurementIdentifier="
                + measurementIdentifier + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        var other = (MeasurementIdentifier)obj;
        if (deviceIdentifier == null) {
            if (other.deviceIdentifier != null)
                return false;
        } else if (!deviceIdentifier.equals(other.deviceIdentifier))
            return false;
        return measurementIdentifier == other.measurementIdentifier;
    }

    @Override
    public int compareTo(final MeasurementIdentifier measurementIdentifier) {
        final var deviceIdentifierComparison = this.getDeviceIdentifier()
                .compareTo(measurementIdentifier.getDeviceIdentifier());
        return deviceIdentifierComparison == 0
                ? Long.compare(this.getMeasurementIdentifier(), measurementIdentifier.getMeasurementIdentifier())
                : deviceIdentifierComparison;
    }
}
