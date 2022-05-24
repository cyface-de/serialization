/*
 * Copyright (C) 2019, 2020 Cyface GmbH - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */
package de.cyface.deserializer.v1;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import de.cyface.model.v1.Measurement;
import de.cyface.model.v1.MeasurementIdentifier;

/**
 * Reads {@link Measurement} from the Cyface binary format.
 *
 * @author Klemens Muthmann
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
     */
    Measurement read() throws IOException, InvalidLifecycleEvents, NoSuchMeasurement;
    /**
     * @return A list with all the valid <code>{@link MeasurementIdentifier}</code> within the deserializable data.
     */
    List<MeasurementIdentifier> peakIntoDatabase() throws IOException;
}