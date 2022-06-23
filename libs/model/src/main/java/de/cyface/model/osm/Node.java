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
package de.cyface.model.osm;

import de.cyface.model.GeoLocation;

/**
 * A POJO that represents a single OSM node in the OSM street network.
 * <p>
 * ATTENTION: After initialisation all values of this object are <code>null</code>. You need to call the appropriate
 * setters to feed values into the object.
 *
 * @author Klemens Muthmann
 * @since 2.2.0
 * @version 1.0.4
 */
public final class Node extends GeoLocation {
    /**
     * The OSM identifier of this node.
     */
    private long identifier;

    @SuppressWarnings("unused") // Part of the API
    public Node() {
        // No arguments' constructor as required by Apache Flink
    }

    @SuppressWarnings("unused") // Part of the API
    public Node(final long identifier, final double latitude, final double longitude) {
        super(latitude, longitude);
        this.identifier = identifier;
    }

    /**
     * @return The OSM identifier of this node.
     */
    public long getIdentifier() {
        return identifier;
    }

    /**
     * @param identifier The OSM identifier of this node.
     */
    public void setIdentifier(final long identifier) {
        this.identifier = identifier;
    }

    @Override
    public String toString() {
        return "Node [identifier=" + identifier + ", latitude=" + getLatitude() + ", longitude=" + getLongitude() + "]";
    }

    @Override
    public int hashCode() {
        final var prime = 31;
        var result = 1;
        result = prime * result + (int)(identifier ^ (identifier >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Node other = (Node)obj;
        return identifier == other.identifier;
    }

    @SuppressWarnings("UnusedReturnValue") // Part of the API
    public String toJson() {
        return String.format("{\"identifier\":%d,\"latitude\":%f,\"longitude\":%f}", identifier, getLatitude(),
                getLongitude());
    }
}
