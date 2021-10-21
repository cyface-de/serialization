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
package de.cyface.model;

public interface Point3D {
    /**
     * @return The x component of the data point
     */
    float getX();

    /**
     * @return The y component of the data point
     */
    float getY();

    /**
     * @return The z component of the data point
     */
    float getZ();

    /**
     * @return The Unix timestamp at which this point was measured in milliseconds since the first of January 1970.
     */
    long getTimestamp();
}
