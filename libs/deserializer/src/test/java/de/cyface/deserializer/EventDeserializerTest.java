package de.cyface.deserializer;

import de.cyface.model.Event;
import de.cyface.protos.model.Event.EventType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventDeserializerTest {

    @Test
    void testMetadataEventMapping() {
        var proto = de.cyface.protos.model.Event.newBuilder()
                .setType(EventType.EVENT_TYPE_UNSPECIFIED)
                .setTimestamp(1_000L)
                .setValue("gender=UNKNOWN")
                .build();

        var result = EventDeserializer.deserialize(List.of(proto));

        assertEquals(1, result.size());
        var event = result.get(0);
        assertEquals(Event.EventType.METADATA, event.getType());
        assertEquals(1_000L, event.getTimestamp());
        assertEquals("gender=UNKNOWN", event.getValue());
    }

    @Test
    void testMetadataEventWithoutValue() {
        var proto = de.cyface.protos.model.Event.newBuilder()
                .setType(EventType.EVENT_TYPE_UNSPECIFIED)
                .setTimestamp(2_000L)
                .build();

        var result = EventDeserializer.deserialize(List.of(proto));

        assertEquals(1, result.size());
        assertEquals(Event.EventType.METADATA, result.get(0).getType());
    }

    @Test
    void testUnrecognizedEventThrows() {
        var proto = de.cyface.protos.model.Event.newBuilder()
                .setTypeValue(-1)
                .setTimestamp(3_000L)
                .build();

        assertThrows(IllegalArgumentException.class, () -> EventDeserializer.deserialize(List.of(proto)));
    }

    @Test
    void testLifecycleEventsStillDeserialize() {
        var start = de.cyface.protos.model.Event.newBuilder()
                .setType(EventType.LIFECYCLE_START)
                .setTimestamp(0L)
                .build();
        var stop = de.cyface.protos.model.Event.newBuilder()
                .setType(EventType.LIFECYCLE_STOP)
                .setTimestamp(60_000L)
                .build();

        var result = EventDeserializer.deserialize(List.of(start, stop));

        assertEquals(2, result.size());
        assertEquals(Event.EventType.LIFECYCLE_START, result.get(0).getType());
        // Protobuf proto3 returns "" for an unset string field; treat blank as no value
        assertTrue(result.get(0).getValue() == null || result.get(0).getValue().isEmpty());
        assertEquals(Event.EventType.LIFECYCLE_STOP, result.get(1).getType());
    }
}