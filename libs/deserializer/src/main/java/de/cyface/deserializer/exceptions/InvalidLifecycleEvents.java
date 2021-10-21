/*
 * Copyright 2020-2021 Cyface GmbH
 *
 * This file is part of the Cyface Serialization.
 *
 *  The Cyface Serialization is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  The Cyface Serialization is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with the Cyface Serialization.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.cyface.deserializer.exceptions;

/**
 * This <code>Exception</code> is thrown if the sequence of {@code de.cyface.model.Event}s is invalid.
 * <p>
 * E.g.:
 * - {@link de.cyface.model.Event.EventType#LIFECYCLE_RESUME} event recorded without prior pause event. [VIC-263]
 *
 * @author Armin Schnabel
 * @version 1.0.0
 * @since 1.3.0
 */
public class InvalidLifecycleEvents extends Exception {
    /**
     * Used to serialize objects of this class. Only change this value if the attribute set changes.
     */
    private static final long serialVersionUID = 1971945670723702382L;

    /**
     * Creates a new instance of this class.
     *
     * @param message An error message to display
     */
    @SuppressWarnings("unused") // API
    public InvalidLifecycleEvents(String message) {
        super(message);
    }

    /**
     * Creates a new instance of this class.
     *
     * @param message An error message to display
     * @param cause The stacktrace which lead to this exception
     */
    @SuppressWarnings("unused") // API
    public InvalidLifecycleEvents(String message, Throwable cause) {
        super(message, cause);
    }
}
