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
package de.cyface.deserializer;

/**
 * This {@code Exception} is thrown when a file of a deprecated or not yet supported file format is to be processed.
 *
 * @author Armin Schnabel
 * @version 1.0.0
 * @since 5.0.0
 */
public class UnsupportedFileVersion extends Exception {
    /**
     * Used to serialize objects of this class. Only change this value if the attribute set changes.
     */
    private static final long serialVersionUID = -3574350415381849173L;

    /**
     * Creates a new instance of this class.
     *
     * @param message An error message to display
     */
    @SuppressWarnings("unused") // API
    public UnsupportedFileVersion(String message) {
        super(message);
    }

    /**
     * Creates a new instance of this class.
     *
     * @param message An error message to display
     * @param cause The stacktrace which lead to this exception
     */
    @SuppressWarnings("unused") // API
    public UnsupportedFileVersion(String message, Throwable cause) {
        super(message, cause);
    }
}
