/*
 * Copyright 2021 Cyface GmbH
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
 * An <code>Exception</code> thrown if an invalid <code>EventType</code> was encountered.
 *
 * @author Klemens Muthmann
 */
public class InvalidEvent extends Exception {
    /**
     * The binary format number encountered, that was not mappable to an <code>EventType</code>.
     */
    private final short eventType;

    /**
     * Create a new completely initialized instance of this object with a message to display on the error log and the
     * binary format encoding number encountered, that caused this <code>Exception</code>.
     *
     * @param message The message to display in the error log
     * @param eventType The number representing the event type in the binary format, which caused this
     *            <code>Exception</code>, since it was unprocessable.
     */
    public InvalidEvent(final String message, final short eventType) {
        super(message + " " + eventType);

        this.eventType = eventType;
    }

    /**
     * @return The unrecognized event type
     */
    public short getEventType() {
        return eventType;
    }
}
