/*
 * Copyright (C) 2020 Cyface GmbH - All Rights Reserved
 *
 * This file is part of the Cyface Server Backend.
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */
package de.cyface.deserializer.v1;

import java.io.IOException;
import java.io.InputStream;

import de.cyface.model.v1.MetaData;

/**
 * A collection of static factory methods to hide the possible complexity of {@link Deserializer} creation.
 *
 * @author Klemens Muthmann
 */
public final class DeserializerFactory {

    /**
     * Private constructor to avoid instantiation of static utility class.
     */
    private DeserializerFactory() {
        // Nothing to do here
    }

    /**
     * Create a new {@link Deserializer} for a {@link de.cyface.model.Measurement} in Cyface Binary data with its
     * accompanying events.
     * Both are provided as compressed input streams, together with the {@link MetaData} about the
     * <code>Measurement</code>.
     *
     * @param metaData The meta information about the <code>Measurement</code> to load
     * @param compressedEventsDataStream The compressed input stream containing the event data in binary format
     * @param compressedDataStream The compressed input stream containing the <code>Measurement</code> data in Cyface
     *            binary format
     * @return A <code>Deserializer</code> for the Cyface binary format
     * @throws IOException
     */
    public static BinaryFormatDeserializer create(final MetaData metaData, final InputStream compressedEventsDataStream,
                                                  final InputStream compressedDataStream) throws IOException {
        final var events = BinaryFormatDeserializer.readEvents(compressedEventsDataStream);
        return new BinaryFormatDeserializer(metaData, events, compressedDataStream);
    }
}