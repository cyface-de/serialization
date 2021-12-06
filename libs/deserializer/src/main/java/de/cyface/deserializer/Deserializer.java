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
package de.cyface.deserializer;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import de.cyface.deserializer.exceptions.InvalidLifecycleEvents;
import de.cyface.deserializer.exceptions.NoSuchMeasurement;
import de.cyface.model.Measurement;
import de.cyface.model.MeasurementIdentifier;

/**
 * Reads {@link Measurement} from the Cyface binary format.
 *
 * @author Klemens Muthmann
 * @author Armin Schnabel
 */
public interface Deserializer extends Serializable {
    /**
     * Carry out the actual reading of the {@link Measurement}.
     * 
     * @return The read <code>Measurement</code>
     * @throws IOException If reading the binary data fails
     * @throws InvalidLifecycleEvents If the binary data contains invalid {@link de.cyface.model.Event} instances. This
     *             might happen if the format of the binary data is newer than the one expected by the
     *             <code>Deserializer</code>
     * @throws NoSuchMeasurement If the read <code>Measurement</code> does not fit the information about the requested
     *             <code>Measurement</code>. It depends on the implementation of the <code>Deserializer</code> if this
     *             <code>Exception</code> can happen or not. Usually it is thrown if the <code>Deserializer</code> reads
     *             a <code>Measurement</code> from a source providing multiple <code>Measurement</code>s.
     * @throws UnsupportedFileVersion If the binary file is from a deprecated or not yet supported file format version.
     */
    Measurement read() throws IOException, InvalidLifecycleEvents, NoSuchMeasurement, UnsupportedFileVersion;
    /**
     * @return A list with all the valid <code>{@link MeasurementIdentifier}</code> within the deserializable data.
     */
    List<MeasurementIdentifier> peakIntoDatabase() throws IOException;
}
