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

import java.util.List;
import java.util.stream.Collectors;

import de.cyface.model.Event;
import de.cyface.serializer.DataSerializable;

/**
 * Deserializes event data from the {@link DataSerializable#TRANSFER_FILE_FORMAT_VERSION}.
 *
 * @author Armin Schnabel
 * @version 1.0.0
 * @since 5.0.0
 */
public class EventDeserializer {

    /**
     * Deserializes event data from the {@link DataSerializable#TRANSFER_FILE_FORMAT_VERSION}.
     *
     * @param entries the data to deserialize
     * @return the deserialized entries
     */
    @SuppressWarnings("unused") // API
    public static List<Event> deserialize(final List<de.cyface.protos.model.Event> entries) {

        return entries.stream().map(e -> {
            final var type = e.getType();
            Event.EventType eventType;
            switch (type) {
                case LIFECYCLE_START:
                    eventType = Event.EventType.LIFECYCLE_START;
                    break;
                case LIFECYCLE_STOP:
                    eventType = Event.EventType.LIFECYCLE_STOP;
                    break;
                case LIFECYCLE_PAUSE:
                    eventType = Event.EventType.LIFECYCLE_PAUSE;
                    break;
                case LIFECYCLE_RESUME:
                    eventType = Event.EventType.LIFECYCLE_RESUME;
                    break;
                case MODALITY_TYPE_CHANGE:
                    eventType = Event.EventType.MODALITY_TYPE_CHANGE;
                    break;
                case UNRECOGNIZED:
                case EVENT_TYPE_UNSPECIFIED:
                default:
                    throw new IllegalArgumentException(String.format("Unknown event type: %s", type));
            }
            return new Event(eventType, e.getTimestamp(), e.getValue());
        }).collect(Collectors.toList());
    }
}