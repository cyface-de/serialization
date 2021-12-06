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
package de.cyface.deserializer;

import java.util.ArrayList;
import java.util.List;

import de.cyface.protos.model.LocationRecords;
import de.cyface.serializer.DataSerializable;
import de.cyface.serializer.Formatter;
import de.cyface.serializer.GeoLocation;

/**
 * Deserializes location data from the {@link DataSerializable#TRANSFER_FILE_FORMAT_VERSION}.
 * <p>
 * Takes care of de-offsetting and converting the data to the default {@code Java} types used by {@link GeoLocation}.
 *
 * @author Armin Schnabel
 * @version 1.0.0
 * @since 5.0.0
 */
public class LocationDeserializer {

    /**
     * Deserializes location data from the {@link DataSerializable#TRANSFER_FILE_FORMAT_VERSION}.
     * <p>
     * Takes care of de-offsetting and converting the data to the default {@code Java} types used by {@link GeoLocation}.
     *
     * @param entries the data to deserialize
     * @return the deserialized entries
     */
    @SuppressWarnings("unused") // API
    public static List<GeoLocation> deserialize(final LocationRecords entries) {

        // The de-offsetter must be initialized once for each location
        final LocationDeOffsetter deOffsetter = new LocationDeOffsetter();

        final List<GeoLocation> ret = new ArrayList<>();
        for (int i = 0; i < entries.getTimestampCount(); i++) {

            // The proto serialized comes in a different format and in offset-format
            final Formatter.Location offsets = new Formatter.Location(entries.getTimestamp(i), entries.getLatitude(i),
                    entries.getLongitude(i), entries.getAccuracy(i), entries.getSpeed(i));
            final Formatter.Location absolutes = deOffsetter.absolute(offsets);
            final GeoLocation deFormatted = DeFormatter.deFormat(absolutes);
            ret.add(deFormatted);
        }
        return ret;
    }
}