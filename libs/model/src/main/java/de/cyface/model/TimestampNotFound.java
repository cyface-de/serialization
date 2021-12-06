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

/**
 * An <code>Exception</code> thrown if a timestamp was not within range of a certain time frame.
 *
 * @author Klemens Muthmann
 */
public class TimestampNotFound extends Exception {

    /**
     * Creates a new exception with an error message.
     * 
     * @param message The error message to report
     */
    public TimestampNotFound(final String message) {
        super(message);
    }
}
