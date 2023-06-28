/*
 * Copyright 2020-2023 Cyface GmbH
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

import org.apache.commons.lang3.Validate;

import de.cyface.model.Measurement;

/**
 * Reads a measurement from the Cyface binary format.
 *
 * @author Armin Schnabel
 * @version 1.0.0
 * @since 1.0.0
 */
public abstract class PhoneDataDeserializer implements Deserializer {
    /**
     * The running number of the {@link Measurement} to deserialize on the next call to {@link #read()}. Since each
     * export may contain multiple <code>Measurement</code>s, this information is required to know which one to
     * deserialize.
     */
    protected long measurementNumber;

    /**
     * Sets the number of the <code>Measurement</code> to deserialize next. To get information about valid numbers call
     * {@link #peakIntoDatabase()}.
     *
     * @param measurementNumber The running number of the {@link Measurement} to deserialize on the next call to
     *            {@link #read()}. Since each export may contain multiple <code>Measurement</code>s, this information is
     *            required to know which one to deserialize
     * @return This object for method chaining
     */
    public PhoneDataDeserializer setMeasurementNumber(final long measurementNumber) {
        Validate.isTrue(measurementNumber >= 0);

        this.measurementNumber = measurementNumber;
        return this;
    }
}
