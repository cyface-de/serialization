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

import de.cyface.deserializer.Deserializer;

/**
 * An <code>Exception</code> that is thrown when a {@link Deserializer} can not read the requested
 * {@link de.cyface.model.Measurement}.
 * This may only occur on <code>Deserializer</code>s reading from a source with multiple <code>Measurement</code>s such
 * as a phone data export.
 * 
 * @author Klemens Muthmann
 */
public class NoSuchMeasurement extends Throwable {
}
