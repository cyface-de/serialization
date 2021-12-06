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
package de.cyface.serializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Objects;

public class GeoLocation {

    /**
     * The logger used to log messages from this class. Configure it using <tt>src/main/resources/logback.xml</tt>.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(GeoLocation.class);
    /**
     * The captured latitude of this {@code GeoLocation} in decimal coordinates as a value between -90.0 (South Pole)
     * and 90.0 (North Pole).
     */
    private final double lat;
    /**
     * The captured longitude of this {@code GeoLocation} in decimal coordinates as a value between -180.0 and 180.0.
     */
    private final double lon;
    /**
     * The timestamp at which this <code>GeoLocation</code> was captured in milliseconds since 1.1.1970.
     */
    private final long timestamp;
    /**
     * The current speed of the measuring device according to its location sensor in meters per second.
     */
    private final double speed;
    /**
     * The current accuracy of the measuring device in meters.
     */
    private final double accuracy;

    /**
     * Creates a new completely initialized <code>GeoLocation</code>.
     *
     * @param lat The captured latitude of this GeoLocation in decimal coordinates as a value between -90.0 (South Pole)
     *            and 90.0 (North Pole).
     * @param lon The captured longitude of this {@code GeoLocation} in decimal coordinates as a value between -180.0
     *            and 180.0.
     * @param timestamp The timestamp at which this <code>GeoLocation</code> was captured in milliseconds since
     *            1.1.1970.
     * @param speed The current speed of the measuring device according to its location sensor in meters per second.
     * @param accuracy The current accuracy of the measuring device in meters.
     */
    public GeoLocation(final double lat, final double lon, final long timestamp, final double speed,
                       final double accuracy) {
        if (lat < -90. || lat > 90.) {
            throw new IllegalArgumentException(String.format(Locale.US,
                    "Illegal value for latitude. Is required to be between -90.0 and 90.0 but was %f.", lat));
        }
        if (lon < -180. || lon > 180.) {
            throw new IllegalArgumentException(String.format(Locale.US,
                    "Illegal value for longitude. Is required to be between -180.0 and 180.0 but was %f.", lon));
        }
        if (speed < 0.) {
            // Occurred on Huawei 10 Mate Pro (RAD-51) and Huawei P30 Android 10 (2021/07)
            LOGGER.warn(String.format(Locale.US, "Illegal value for speed. Is required to be positive but was %f.", speed));
        }
        if (accuracy < 0.) {
            throw new IllegalArgumentException(String.format(Locale.US,
                    "Illegal value for accuracy. Is required to be positive but was %f.", accuracy));
        }
        if (timestamp < 0L) {
            throw new IllegalArgumentException(String.format(Locale.US,
                    "Illegal value for timestamp. Is required to be greater then 0L but was %d.", timestamp));
        }

        this.lat = lat;
        this.lon = lon;
        this.timestamp = timestamp;
        this.speed = speed;
        this.accuracy = accuracy;
    }

    /**
     * @return The captured latitude of this GeoLocation in decimal coordinates as a value between -90.0 (South Pole)
     *         and 90.0 (North Pole).
     */
    public double getLat() {
        return lat;
    }

    /**
     * @return The captured longitude of this {@code GeoLocation} in decimal coordinates as a value between -180.0 and
     *         180.0.
     */
    public double getLon() {
        return lon;
    }

    /**
     * @return The timestamp at which this <code>GeoLocation</code> was captured in milliseconds since 1.1.1970.
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * @return The current speed of the measuring device according to its location sensor in meters per second.
     */
    public double getSpeed() {
        return speed;
    }

    /**
     * @return The current accuracy of the measuring device in meters.
     */
    public double getAccuracy() {
        return accuracy;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        GeoLocation location = (GeoLocation)o;
        return Double.compare(location.lat, lat) == 0 &&
                Double.compare(location.lon, lon) == 0 &&
                timestamp == location.timestamp &&
                Double.compare(location.speed, speed) == 0 &&
                Double.compare(location.accuracy, accuracy) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(lat, lon, timestamp, speed, accuracy);
    }
}
