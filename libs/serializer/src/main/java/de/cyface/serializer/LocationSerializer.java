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
package de.cyface.serializer;

import java.util.List;

import de.cyface.model.RawRecord;
import de.cyface.protos.model.LocationRecords;

/**
 * Serializer wrapper for geolocations which takes care of offsetting and lossy compression applied before serializing.
 *
 * @author Armin Schnabel
 * @version 1.0.0
 * @since 5.0.0
 */
public final class LocationSerializer {

    /**
     * Converts geo-locations to the class supported by the {@code Protobuf} generated serializer classes.
     * <p>
     * Applies lossy compression and offsetting to optimize the encoding density.
     *
     * @param data the locations to convert
     * @return the converted data
     */
    public static de.cyface.protos.model.LocationRecords locations(final List<RawRecord> data) {
        final var builder = LocationRecords.newBuilder();

        // The offsetter must be initialized once for each location
        final LocationOffsetter offsetter = new LocationOffsetter();

        data.forEach(l -> {
            final Formatter.Location formatted = new Formatter.Location(l.getTimestamp(), l.getLatitude(),
                    l.getLongitude(), l.getSpeed(), l.getAccuracy());
            final Formatter.Location offsets = offsetter.offset(formatted);

            // TODO: When we support nullable elevations, we need to ensure we implement it the same as communicated in STAD-827.
            // - 100, 101, null, 200, 201, null, null → 100, +1, null, 200, +1, null, null
            builder.addTimestamp(offsets.getTimestamp())
                    .addLatitude(offsets.getLatitude())
                    .addLongitude(offsets.getLongitude())
                    .addAccuracy(offsets.getAccuracy())
                    .addSpeed(offsets.getSpeed());
        });
        return builder.build();
    }
}
