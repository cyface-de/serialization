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
package de.cyface.serializer;

import static de.cyface.serializer.DataSerializable.TRANSFER_FILE_FORMAT_VERSION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.cyface.model.Event;
import de.cyface.protos.model.Measurement;

public class EventSerializerTest {

    @DisplayName("Happy Path test for the serialization of events.")
    @Test
    void testEventSerialization() {
        // Arrange
        final Event modalitySelectionEvent = new Event(Event.EventType.MODALITY_TYPE_CHANGE, 0L, "BICYCLE");
        final Event capturingStartEvent = new Event(Event.EventType.LIFECYCLE_START, 0L, null);
        final Event capturingStopEvent = new Event(Event.EventType.LIFECYCLE_STOP, 60_000L, null);
        final List<Event> events = new ArrayList<>();
        events.add(capturingStartEvent);
        events.add(capturingStopEvent);
        events.add(modalitySelectionEvent);

        // Act
        final var serializableEvents = EventSerializer.events(events);
        final var serializedBytes = Measurement.newBuilder()
                .setFormatVersion(TRANSFER_FILE_FORMAT_VERSION)
                .addAllEvents(serializableEvents)
                .build().toByteArray();

        // Assert
        // Check for correct length of serialized events
        final var formatVersionHeaderBytes = 3;
        final var eventHeaderBytes = 3;
        final var timestampHeaderBytes = 2;
        final var capturingStartEventBytes = eventHeaderBytes + timestampHeaderBytes;
        final var capturingStopEventBytes = eventHeaderBytes + timestampHeaderBytes + 4;
        final var valueHeaderBytes = 2;
        final var modalitySelectionEventBytes = eventHeaderBytes + timestampHeaderBytes + valueHeaderBytes
                + "BICYCLE".getBytes(StandardCharsets.UTF_8).length;
        final int expectedLength = formatVersionHeaderBytes + capturingStartEventBytes
                + capturingStopEventBytes + modalitySelectionEventBytes;
        assertThat("Serialized data did not conform to expected length in bytes.", serializedBytes.length,
                is(expectedLength));
    }
}
