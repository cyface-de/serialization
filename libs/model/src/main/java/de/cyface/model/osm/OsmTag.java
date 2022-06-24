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

import org.apache.commons.lang3.Validate;

/**
 * Represents a single key,value paired Open Street Map (OSM) attribute.
 *
 * @author Klemens Muthmann
 * @version 1.0.0
 * @since 2.2.0
 */
public final class OsmTag extends MapTag {

    /**
     * The value of the OSM attribute.
     */
    private final Object value;

    /**
     * Creates a new completely initialized {@link OsmTag}. The created object is immutable.
     *
     * @param key The key of the OSM attribute
     * @param value The value of the OSM attribute
     */
    public OsmTag(final String key, final Object value) {
        super(key);
        Validate.notNull(value);
        // Before other types were injected by accident, like `TextNode`
        Validate.isTrue(value instanceof String || value instanceof Double || value instanceof Integer);

        this.value = value;
    }

    /**
     * @return The value of the OSM attribute
     */
    public Object getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;
        OsmTag osmTag = (OsmTag)o;
        return value.equals(osmTag.value);
    }

    @Override
    public String toString() {
        return "OsmTag{" +
                "key='" + super.getKey() + '\'' +
                "value=" + value +
                '}';
    }
}
