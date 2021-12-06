/*
 * Copyright 2019-2021 Cyface GmbH
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
package de.cyface.serializer;

import java.util.ArrayList;
import java.util.List;

import de.cyface.model.Event;

/**
 * Serializer wrapper for events.
 *
 * @author Armin Schnabel
 * @version 1.0.0
 * @since 5.0.0
 */
public final class EventSerializer {

    /**
     * Converts events to the class supported by the {@code Protobuf} generated serializer classes.
     *
     * @param data the locations to convert
     * @return the converted data
     */
    public static List<de.cyface.protos.model.Event> events(final List<Event> data) {
        final var ret = new ArrayList<de.cyface.protos.model.Event>();

        data.forEach(e -> {
            final var typeString = e.getType().getDatabaseIdentifier();
            final var type = de.cyface.protos.model.Event.EventType.valueOf(typeString);
            final var value = e.getValue();

            final var builder = de.cyface.protos.model.Event.newBuilder()
                    .setTimestamp(e.getTimestamp())
                    .setType(type);

            if (value != null) {
                builder.setValue(e.getValue());
            }
            ret.add(builder.build());
        });
        return ret;
    }
}
