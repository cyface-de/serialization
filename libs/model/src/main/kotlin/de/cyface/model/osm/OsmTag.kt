/*
 * Copyright 2020-2022 Cyface GmbH
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

/**
 * Represents a single key,value paired Open Street Map (OSM) attribute.
 *
 * @author Klemens Muthmann
 * @property key The key of the OSM attribute
 * @property value The value of the OSM attribute
 */
class OsmTag(key: String?, val value: Any) : MapTag(key!!) {

    // Before other types were injected by accident, like `TextNode`
    init {
        require(value is String || value is Double || value is Int) {
            "Invalid value type for OsmTag: ${value::class}"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        if (!super.equals(other)) return false
        val osmTag = other as OsmTag
        return value == osmTag.value
    }

    override fun hashCode(): Int {
        return 31 * super.hashCode() + value.hashCode()
    }

    override fun toString(): String {
        return "OsmTag{" +
                "key='" + super.key + '\'' +
                "value=" + value +
                '}'
    }
}
