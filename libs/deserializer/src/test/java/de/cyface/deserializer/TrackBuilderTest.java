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
package de.cyface.deserializer;

import static de.cyface.model.Event.EventType.LIFECYCLE_PAUSE;
import static de.cyface.model.Event.EventType.LIFECYCLE_RESUME;
import static de.cyface.model.Event.EventType.LIFECYCLE_START;
import static de.cyface.model.Event.EventType.LIFECYCLE_STOP;
import static de.cyface.model.Event.EventType.MODALITY_TYPE_CHANGE;
import static de.cyface.model.MetaData.SUPPORTED_VERSIONS;
import static de.cyface.model.Modality.BICYCLE;
import static de.cyface.model.Modality.WALKING;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import de.cyface.deserializer.exceptions.InvalidLifecycleEvents;
import de.cyface.model.Measurement;
import de.cyface.model.MetaData;
import de.cyface.model.NoTracksRecorded;
import de.cyface.model.Point3DImpl;
import de.cyface.serializer.GeoLocation;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.cyface.model.Event;
import de.cyface.model.MeasurementIdentifier;
import de.cyface.model.Modality;
import de.cyface.model.RawRecord;
import de.cyface.model.Track;

/**
 * Tests whether the {@link TrackBuilder} works as expected.
 *
 * @author Armin Schnabel
 * @version 2.0.0
 * @since 1.0.0
 */
public class TrackBuilderTest {
    /**
     * An instance of the handler under test.
     */
    private TrackBuilder oocut;
    /**
     * A globally unique identifier of the simulated upload device. The actual value does not really matter.
     */
    private static final String DEVICE_IDENTIFIER = UUID.randomUUID().toString();
    /**
     * The measurement identifier used for the test measurement. The actual value does not matter that much. It
     * simulates a device wide unique identifier.
     */
    private static final long MEASUREMENT_IDENTIFIER = 1L;

    /**
     * Initialize mock objects and set up the object of the class under test.
     */
    @BeforeEach
    void setUp() {
        oocut = new TrackBuilder();
    }

    @Test
    void testBuild() throws InvalidLifecycleEvents {
        // Arrange
        final var numberOfLocations = 4;
        final var identifier = new MeasurementIdentifier(DEVICE_IDENTIFIER, MEASUREMENT_IDENTIFIER);
        final var locationRecords = generateLocationRecords(numberOfLocations,
                new Long[] {1000L, 1500L, 3500L, 4000L}, identifier);
        final var events = new ArrayList<Event>();
        events.add(new Event(LIFECYCLE_START, 1000L, null));
        events.add(new Event(MODALITY_TYPE_CHANGE, 1000L, WALKING.getDatabaseIdentifier()));
        events.add(new Event(LIFECYCLE_PAUSE, 2000L, null));
        events.add(new Event(MODALITY_TYPE_CHANGE, 2500L, BICYCLE.getDatabaseIdentifier()));
        events.add(new Event(LIFECYCLE_RESUME, 3000L, null));
        events.add(new Event(LIFECYCLE_STOP, 4000L, null));
        final var point3DS = new ArrayList<Point3DImpl>();
        point3DS.add(new Point3DImpl(1.0f, -2.0f, 3.0f, 1010L));
        // All points >= LIFECYCLE_RESUME should be in the track, no matter when the first GPS point is captured
        point3DS.add(new Point3DImpl(1.1f, -2.1f, 3.1f, 3010L));

        // Act
        final var tracks = oocut.build(locationRecords, events, point3DS, point3DS, point3DS);

        // Assert
        locationRecords.get(0).setModality(WALKING);
        locationRecords.get(1).setModality(WALKING);
        locationRecords.get(2).setModality(BICYCLE);
        locationRecords.get(3).setModality(BICYCLE);
        final var expectedTracks = new ArrayList<Track>();
        final var track1 = new Track();
        final var locations1 = new ArrayList<RawRecord>();
        locations1.add(locationRecords.get(0));
        locations1.add(locationRecords.get(1));
        track1.setLocationRecords(locations1);
        track1.setAccelerations(point3DS.subList(0, 1));
        track1.setRotations(point3DS.subList(0, 1));
        track1.setDirections(point3DS.subList(0, 1));
        final var track2 = new Track();
        final var locations2 = new ArrayList<RawRecord>();
        locations2.add(locationRecords.get(2));
        locations2.add(locationRecords.get(3));
        track2.setLocationRecords(locations2);
        track2.setAccelerations(point3DS.subList(1, 2));
        track2.setRotations(point3DS.subList(1, 2));
        track2.setDirections(point3DS.subList(1, 2));
        expectedTracks.add(track1);
        expectedTracks.add(track2);
        assertThat(tracks, is(equalTo(expectedTracks)));
    }

    /**
     * This test reproduced a bug in the internal workings of the tested method.
     */
    @Test
    void testMoveIteratorToLastBefore_withIteratorAfterEventTime() {
        // Arrange
        final int numberOfLocations = 4;
        final var identifier = new MeasurementIdentifier(DEVICE_IDENTIFIER, MEASUREMENT_IDENTIFIER);
        final var locationRecords = generateLocationRecords(numberOfLocations,
                new Long[] {1_000L, 1_500L, 3_500L, 4_000L}, identifier);
        final var geoLocationIterator = locationRecords.listIterator();
        // Move iterator to first location after the pause event as this is the case in extractTracks() when this method
        // is called
        geoLocationIterator.next(); // location 1
        geoLocationIterator.next(); // location 2
        geoLocationIterator.next(); // location 3
        final var resumeEventTime = 3_000L;

        // Act
        oocut.moveIteratorToLastBefore(geoLocationIterator, resumeEventTime);

        // Assert
        final var expectedLocation = locationRecords.get(2);
        // moveIteratorToLastBefore() moves the iterator to "last before the event" so .next() gives the "first after"
        final var firstLocationAfterResumeEvent = geoLocationIterator.next();
        assertThat(firstLocationAfterResumeEvent, is(equalTo(expectedLocation)));
    }

    @Test
    void testMoveIteratorToLastBefore_withIteratorBeforeEventTime() {
        // Arrange
        final var numberOfLocations = 4;
        final var identifier = new MeasurementIdentifier(DEVICE_IDENTIFIER, MEASUREMENT_IDENTIFIER);
        final var locations = generateLocationRecords(numberOfLocations,
                new Long[] {1_000L, 1_500L, 3_500L, 4_000L}, identifier);
        final var geoLocationIterator = locations.listIterator();
        // Move iterator to first location after the pause event as this is the case in extractTracks() when this method
        // is called
        geoLocationIterator.next(); // location 1
        geoLocationIterator.next(); // location 2
        final var resumeEventTime = 3_000L;

        // Act
        oocut.moveIteratorToLastBefore(geoLocationIterator, resumeEventTime);

        // Assert
        final var expectedLocation = locations.get(2);
        // moveIteratorToLastBefore() moves the iterator to "last before the event" so .next() gives the "first after"
        final var firstLocationAfterResumeEvent = geoLocationIterator.next();
        assertThat(firstLocationAfterResumeEvent, is(equalTo(expectedLocation)));
    }

    @Test
    void testMoveIteratorToLastBefore_withIteratorEqualEventTime() {
        // Arrange
        final var numberOfLocations = 4;
        final var identifier = new MeasurementIdentifier(DEVICE_IDENTIFIER, MEASUREMENT_IDENTIFIER);
        final var locations = generateLocationRecords(numberOfLocations,
                new Long[] {1_000L, 1_500L, 3_000L, 4_000L}, identifier);
        final var geoLocationIterator = locations.listIterator();
        // Move iterator to first location after the pause event as this is the case in extractTracks() when this method
        // is called
        geoLocationIterator.next(); // location 1
        geoLocationIterator.next(); // location 2
        geoLocationIterator.next(); // location 3
        final var resumeEventTime = 3_000L;

        // Act
        oocut.moveIteratorToLastBefore(geoLocationIterator, resumeEventTime);

        // Assert
        final var expectedLocation = locations.get(2);
        // moveIteratorToLastBefore() moves the iterator to "last before the event" so .next() gives the "first after"
        final var firstLocationAfterResumeEvent = geoLocationIterator.next();
        assertThat(firstLocationAfterResumeEvent, is(equalTo(expectedLocation)));
    }

    /**
     * Reproduced two crashes which occur when we collect no sensor data [STAD-712].
     */
    @Test
    void testBuild_withEmptyAccelerationList_shouldNotThrow() throws InvalidLifecycleEvents {
        // Arrange
        final var identifier = new MeasurementIdentifier(DEVICE_IDENTIFIER, MEASUREMENT_IDENTIFIER);
        final var locationRecords = generateLocationRecords(
                4, new Long[]{1_000L, 1_400L, 2_100L, 2_400L}, identifier
        );

        final var events = List.of(
                new Event(LIFECYCLE_START, 1_000L, null),
                new Event(LIFECYCLE_PAUSE, 1_500L, null),
                new Event(LIFECYCLE_RESUME, 2_000L, null),
                new Event(LIFECYCLE_STOP, 2_500L, null)
        );
        final var empty = List.<Point3DImpl>of(); // triggers .next() on empty list

        // Act
        final var result = oocut.build(locationRecords, events, empty, empty, empty);

        // Assert
        assertThat(result.size(), is(2));

        // First track: before pause
        final var track1 = result.get(0);
        assertThat(track1.getLocationRecords().size(), is(2));
        assertThat(track1.getAccelerations().isEmpty(), is(true));
        assertThat(track1.getRotations().isEmpty(), is(true));
        assertThat(track1.getDirections().isEmpty(), is(true));

        // Second track: after resume
        final var track2 = result.get(1);
        assertThat(track2.getLocationRecords().size(), is(2));
        assertThat(track2.getAccelerations().isEmpty(), is(true));
        assertThat(track2.getRotations().isEmpty(), is(true));
        assertThat(track2.getDirections().isEmpty(), is(true));
    }

    /**
     * Reproduced a crash which occurred in SR-2025 campaign on an Android device [STAD-712].
     *      osVersion: 'Android 11',
     *       deviceType: 'SM-A505FN',
     *       appVersion: '3.3.25042964', (SR app,  cyface_sdk_version = "7.13.12")
     *       formatVersion: 3,
     *       length: 0,
     *       locationCount: Long('2'),
     *  <p>
     *  Instead of fixing the location iterator in the middle of a campaign, we just make this error fail softly
     *  and make it fail as INFO when we have very few locations:
     *  - e.g. no resume: <= (1+0+1)*2 locations => INFO
     *  - e.g. 1 resume: <= (1+1+1)**2 locations => INFO
     *  As soon as one track has > 3 locations this error should not occur.
     *  <p>
     *  (1+1+1) because: with 1 resume and 2*2+1 locations it's possible, that
     *  - track 1: 2 location, in-between: 1 locations, track 2: 2 locations, after stop: 1 location
     *  <p>
     *  If there are much more locations than resume events, this must be logged as WARN at least. (or crash)
     *  (and show the number of locations and resume events involved)
     */
    @Test
    @DisplayName("TrackBuilder creates one track for short final lifecycle segment")
    void testTrackBuilderWithShortFinalSegment() throws InvalidLifecycleEvents, NoTracksRecorded {
        // Given
        final var identifier = new MeasurementIdentifier("test", 1);
        final var events = List.of(
                new Event(LIFECYCLE_START, 1746429259640L, ""),
                new Event(MODALITY_TYPE_CHANGE, 1746429259640L, "BICYCLE"),
                new Event(LIFECYCLE_PAUSE, 1746429328310L, ""),
                new Event(LIFECYCLE_RESUME, 1746445158656L, ""),
                new Event(LIFECYCLE_PAUSE, 1746445161947L, ""),
                new Event(LIFECYCLE_RESUME, 1746518861627L, ""),
                new Event(LIFECYCLE_STOP, 1746518872724L, "")
        );

        final var locations = List.of(
                // This narrow case with only 2 locations produces the crash as we always loose the first location
                new GeoLocation(48.123509, 11.372924, 1746518871000L, 1.35f, 29.5f),
                new GeoLocation(48.123484, 11.372941, 1746518872000L, 1.77f, 27.0f),
                // But with only 3 more location this does not happen, as 2 locations form a track
                new GeoLocation(48.123485, 11.372942, 1746518872001L, 1.77f, 27.0f)
        );

        // No sensor data for this test
        final var accelerations = List.<Point3DImpl>of();
        final var rotations = List.<Point3DImpl>of();
        final var directions = List.<Point3DImpl>of();

        final var builder = new TrackBuilder();

        // When
        final var tracks = builder.build(locations, events, accelerations, rotations, directions, identifier);
        final var metaData = MetaData.Companion.create(
                new MeasurementIdentifier("test", 1L),
                "deviceType",
                "osVersion",
                "appVersion",
                0,
                UUID.randomUUID(),
                SUPPORTED_VERSIONS,
                new Date()
        );
        Measurement.Companion.create(metaData, tracks);
    }


    /**
     * Generates GeoLocationRecords for testing.
     *
     * @param numberOfLocations to generate, e.g. 3
     * @param locationTimestamps as array, e.g.: new Long[] {1000L, 2500L, 3000L}
     * @param identifier the identifier of the measurement context for the locations
     * @return the generates Locations ordered by timestamp
     */
    static List<RawRecord> generateLocationRecords(
            @SuppressWarnings("SameParameterValue") int numberOfLocations,
            Long[] locationTimestamps, final MeasurementIdentifier identifier) {

        Validate.isTrue(locationTimestamps.length == numberOfLocations);
        final var locationRecords = new ArrayList<RawRecord>();
        for (var i = 0; i < numberOfLocations; i++) {
            var j = i + 1;
            final var locationRecord = new RawRecord(identifier, locationTimestamps[i],
                    51.0 + j / 10.0, 13.0 + j / 10.0, null,
                    100 + j * 10, 0.0 + j * 0.1, Modality.UNKNOWN);
            locationRecords.add(locationRecord);
        }
        return locationRecords;
    }
}
