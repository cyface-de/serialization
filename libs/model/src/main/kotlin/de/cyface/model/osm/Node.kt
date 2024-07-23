/*
 * Copyright 2019-2024 Cyface GmbH
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
package de.cyface.model.osm

import de.cyface.model.GeoLocation
import java.util.Locale

/**
 * A POJO that represents a single OSM node in the OSM street network.
 *
 *
 * ATTENTION: After initialisation all values of this object are `null`. You need to call the appropriate
 * setters to feed values into the object.
 *
 * @author Klemens Muthmann
 * @property identifier The OSM identifier of this node.
 */
class Node : GeoLocation {
    var identifier: Long = 0

    /**
     * No arguments' constructor as required by Apache Flink.
     */
    @Suppress("unused") // Part of the API
    constructor()

    /**
     * Creates a fully initialized instance of this class.
     *
     * @param identifier The OSM identifier of this node.
     * @param latitude Geographical latitude in coordinates (decimal fraction) raging from -90° (south) to 90° (north).
     * @param longitude Geographical longitude in coordinates (decimal fraction) ranging from -180° (west) to 180°
     * (east).
     */
    @Suppress("unused") // Part of the API
    constructor(identifier: Long, latitude: Double, longitude: Double) : super(latitude, longitude) {
        this.identifier = identifier
    }

    override fun toString(): String {
        return "Node [identifier=$identifier, latitude=$latitude, longitude=$longitude]"
    }

    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + (identifier xor (identifier ushr 32)).toInt()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null) {
            return false
        }
        if (javaClass != other.javaClass) {
            return false
        }
        val node = other as Node
        return identifier == node.identifier
    }

    /**
     * Converts this [Node] to a `String` in the `Json` object format.
     *
     * @return The node as Json String.
     */
    // Part of the API
    fun toJson(): String {
        return String.format(
            Locale.getDefault(),
            "{\"identifier\":%d,\"latitude\":%f,\"longitude\":%f}",
            identifier,
            latitude,
            longitude,
        )
    }
}
