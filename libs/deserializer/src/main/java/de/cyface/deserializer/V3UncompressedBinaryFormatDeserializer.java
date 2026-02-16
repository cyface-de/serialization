/*
 * Copyright 2026 Cyface GmbH
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

import de.cyface.deserializer.exceptions.InvalidLifecycleEvents;
import de.cyface.deserializer.exceptions.NoSuchMeasurement;
import de.cyface.model.Measurement;
import de.cyface.model.MeasurementIdentifier;
import de.cyface.model.MetaData;
import de.cyface.model.NoTracksRecorded;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

/**
 * This may be used to deserialize an unzipped binary file in the Version 3 Cyface Format.
 *
 * Such files are usually the result of exporting files directly from GridFS (Mongo Raw Data Database).
 */
public class V3UncompressedBinaryFormatDeserializer implements Deserializer{

    /**
     * The Metadata associated with the binary data. This must be provided since it is not stored together with the binary.
     */
    private MetaData metaData;
    /**
     * An input stream providing the binary data to deserialize.
     */
    private InputStream dataStream;

    /**
     * Create a new fully initialized <code>V3UncompressedBinaryFormatDeserializer</code>.
     *
     * @param metaData The Metadata associated with the binary data. This must be provided since it is not stored together with the binary.
     * @param dataStream An input stream providing the binary data to deserialize.
     */
    public V3UncompressedBinaryFormatDeserializer(final MetaData metaData, final InputStream dataStream) {
        this.metaData = metaData;
        this.dataStream = dataStream;
    }

    @Override
    public Measurement read() throws IOException, InvalidLifecycleEvents, UnsupportedFileVersion, NoTracksRecorded {
        final var measurement = de.cyface.protos.model.Measurement.parseFrom(dataStream);
        final var events = EventDeserializer.deserialize(measurement.getEventsList());
        final var locations = LocationDeserializer.deserialize(measurement.getLocationRecords());
        final var accelerations = Point3DDeserializer
                .accelerations(measurement.getAccelerationsBinary().getAccelerationsList());
        final var rotations = Point3DDeserializer.rotations(measurement.getRotationsBinary().getRotationsList());
        final var directions = Point3DDeserializer
                .directions(measurement.getDirectionsBinary().getDirectionsList());
        final var builder = new TrackBuilder();
        final var tracks = builder.build(locations, events, accelerations, rotations, directions,
                metaData.getIdentifier());
        return Measurement.Companion.create(metaData, tracks);
    }

    @Override
    public List<MeasurementIdentifier> peakIntoDatabase() {
        return Collections.singletonList(metaData.getIdentifier());
    }
}
