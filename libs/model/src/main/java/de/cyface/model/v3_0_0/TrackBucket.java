/*
 * Copyright 2021-2022 Cyface GmbH
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
package de.cyface.model.v3_0_0;

import de.cyface.model.Track;

import java.util.Date;

/**
 * The mongo database "bucket" which contains a slice of a track.
 *
 * @author Armin Schnabel
 * @since 1.5.0
 * @version 1.0.0
 */
@SuppressWarnings("unused") // Part of the API
public class TrackBucket {

    /**
     * The track's position within the measurement.
     */
    final int trackId;
    /**
     * The time "slice" of the bucket.
     */
    final Date bucket;
    /**
     * The track slice of the bucket.
     */
    final Track track;
    /**
     * The {@link MetaData} of the track.
     */
    final MetaData metaData;

    /**
     * Initialized a fully constructed instance of this class.
     *
     * @param trackId The track's position within the measurement.
     * @param bucket The time "slice" of the bucket.
     * @param track The track slice of the bucket.
     * @param metaData The {@link MetaData} of the track.
     */
    public TrackBucket(final int trackId, final Date bucket, final Track track, final MetaData metaData) {
        this.trackId = trackId;
        this.bucket = bucket;
        this.track = track;
        this.metaData = metaData;
    }

    /**
     * @return The track's position within the measurement.
     */
    public int getTrackId() {
        return trackId;
    }

    /**
     * @return The time "slice" of the bucket.
     */
    public Date getBucket() {
        return bucket;
    }

    /**
     * @return The track slice of the bucket.
     */
    public Track getTrack() {
        return track;
    }

    /**
     * @return The {@link MetaData} of the track.
     */
    public MetaData getMetaData() {
        return metaData;
    }
}
