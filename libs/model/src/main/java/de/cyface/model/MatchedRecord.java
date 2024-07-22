/*
 * Copyright 2019-2022 Cyface GmbH
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import de.cyface.model.osm.Node;
import de.cyface.model.osm.OsmTag;
import de.cyface.model.osm.Way;

/**
 * A location record matched on a road net. The location is matched in between the two OSM nodes accessible via
 * <code>predecessor</code> and <code>successor</code>.
 *
 * @author Klemens Muthmann
 * @version 1.0.0
 * @since 2.2.0
 */
@SuppressWarnings("unused") // Part of the API
public class MatchedRecord extends GeoLocationRecord implements MatchedGeometry {

    /**
     * Used to serialize objects of this class. Only change this value if this objects attribute set changes.
     */
    private static final long serialVersionUID = -1263115409492018808L;
    /**
     * The Open Street Map node directly prior to this <code>MatchedRecord</code>.
     */
    private Node predecessor;
    /**
     * The Open Street Map node directly succeeding this <code>MatchedRecord</code>.
     */
    private Node successor;
    /**
     * The list of {@link Way} objects this matched record was matched to. Usually this should have only one entry,
     * except if this was matched to a crossing with two or more ways.
     */
    private List<Way<OsmTag>> ways;

    /**
     * Public no-argument-constructor is required by Apache Flink to send around instances of this class in the cluster.
     * Do not use this as part of your production code.
     */
    public MatchedRecord() {
        // Nothing to do here.
    }

    /**
     * Creates a new completely initialized object of this class.
     *
     * @param timestamp The timestamp this location was captured on in milliseconds since 1st January 1970 (epoch)
     * @param latitude Geographical latitude in coordinates (decimal fraction) raging from -90° (south) to 90° (north)
     * @param longitude Geographical longitude in coordinates (decimal fraction) ranging from -180° (west) to 180°
     *            (east)
     * @param elevation The elevation above sea level in meters or <code>null</code> if it could not be calculated
     * @param predecessor The OSM node directly before the location
     * @param successor The OSM node directly after the location
     * @param ways The list of {@link Way} objects this matched record was matched to. Usually this should have only one
     *            entry, except if this was matched to a crossing with two or more ways
     */
    public MatchedRecord(final MeasurementIdentifier measurementIdentifier, final long timestamp, final double latitude,
            final double longitude, final Double elevation, final Node predecessor, final Node successor,
            final List<Way<OsmTag>> ways) {
        super(measurementIdentifier, timestamp, latitude, longitude, elevation);
        setPredecessor(predecessor);
        setSuccessor(successor);
        setWays(ways);
    }

    @Override
    public Node getPredecessor() {
        return this.predecessor;
    }

    @Override
    public void setPredecessor(final Node predecessor) {
        Objects.requireNonNull(predecessor);

        this.predecessor = predecessor;
    }

    @Override
    public Node getSuccessor() {
        return this.successor;
    }

    @Override
    public void setSuccessor(final Node successor) {
        Objects.requireNonNull(successor);

        this.successor = successor;
    }

    @Override
    public List<Way<OsmTag>> getWays() {
        return Collections.unmodifiableList(this.ways);
    }

    @Override
    public void setWays(final List<Way<OsmTag>> ways) {
        Objects.requireNonNull(ways);

        this.ways = new ArrayList<>(ways);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;
        MatchedRecord that = (MatchedRecord)o;
        return getPredecessor().equals(that.getPredecessor()) && getSuccessor().equals(that.getSuccessor())
                && getWays().equals(that.getWays());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getPredecessor(), getSuccessor(), getWays());
    }

    @Override
    public String toString() {
        return "MatchedRecord{" +
                "measurementIdentifier=" + getMeasurementIdentifier() +
                ", timestamp=" + getTimestamp() +
                ", latitude=" + getLatitude() +
                ", longitude=" + getLongitude() +
                ", elevation=" + elevation +
                ", predecessor=" + predecessor +
                ", successor=" + successor +
                ", ways=" + ways +
                '}';
    }
}
