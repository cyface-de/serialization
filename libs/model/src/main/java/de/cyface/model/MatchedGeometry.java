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

import java.util.List;

import de.cyface.model.osm.Node;
import de.cyface.model.osm.OsmTag;
import de.cyface.model.osm.Way;

/**
 * Some spatial geometry that has been matched to an OpenStreetMap road network.
 *
 * @author Klemens Muthmann
 * @version 1.0.0
 * @since 2.2.0
 */
public interface MatchedGeometry {

    /**
     * @return The OSM node directly before the location
     */
    Node getPredecessor();

    /**
     * Set the OpenStreetMap node directly preceding this geometry.
     * This method is required to fulfill the POJO specification as required by Apache Flink.
     * Since ways are supposed to be immutable, you should not use this method in your own code.
     *
     * @param predecessor The OSM node directly before the location
     */
    void setPredecessor(final Node predecessor);

    /**
     * @return The OSM node directly after the location
     */
    Node getSuccessor();

    /**
     * Set the OpenStreetMap node directly succeeding this geometry.
     * This method is required to fulfill the POJO specification as required by Apache Flink.
     * Since ways are supposed to be immutable, you should not use this method in your own code.
     *
     * @param successor The OSM node directly after the location
     */
    void setSuccessor(final Node successor);

    /**
     * Provides a <code>List</code> of all the OpenStreetMap ways this geometry has been matched to.
     * This should usually only contain one element.
     * The only exception are geometries that have been matched to a crossing.
     * In such a case all ways leading to that crossing are returned.
     *
     * @return The <code>Way</code> instances the geometry was matched to
     */
    List<Way<OsmTag>> getWays();

    /**
     * Sets the <code>Way</code> instances this geometry was matched to.
     * This method is required to fulfill the POJO specification as required by Apache Flink.
     * Since ways are supposed to be immutable, you should not use this method in your own code.
     *
     * @param ways The <code>Way</code> instances the geometry was matched to
     */
    void setWays(List<Way<OsmTag>> ways);
}
