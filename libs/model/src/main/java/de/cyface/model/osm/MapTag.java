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
package de.cyface.model.osm;

import java.util.Objects;

import org.apache.commons.lang3.Validate;

/**
 * Interface for a single key,value paired attribute used to represent a feature on a map.
 *
 * @author Armin Schnabel
 * @version 1.0.0
 * @since 2.2.0
 */
public abstract class MapTag {

    /**
     * The key of the OSM attribute.
     */
    private final String key;

    /**
     * Creates a new completely initialized <code>MapTag</code>. The created object is immutable.
     *
     * @param key The key of the OSM attribute
     */
    public MapTag(final String key) {
        Validate.notNull(key);
        this.key = key;
    }

    /**
     * @return The key of the OSM attribute
     */
    public String getKey() {
        return key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        MapTag mapTag = (MapTag)o;
        return key.equals(mapTag.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }

    @Override
    public String toString() {
        return "MapTag{" +
                "key='" + key + '\'' +
                '}';
    }
}
