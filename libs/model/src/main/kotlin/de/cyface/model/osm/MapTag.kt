/*
 * Copyright 2020-2024 Cyface GmbH
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

import org.apache.commons.lang3.Validate
import java.util.Objects

/**
 * Interface for a single key,value paired attribute used to represent a feature on a map.
 *
 * @author Armin Schnabel
 * @property key The key of the OSM attribute
 */
abstract class MapTag(key: String) {
    @JvmField
    val key: String

    init {
        Validate.notNull(key)
        this.key = key
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val mapTag = other as MapTag
        return key == mapTag.key
    }

    override fun hashCode(): Int {
        return Objects.hash(key)
    }

    override fun toString(): String {
        return "MapTag{" +
                "key='" + key + '\'' +
                '}'
    }
}
