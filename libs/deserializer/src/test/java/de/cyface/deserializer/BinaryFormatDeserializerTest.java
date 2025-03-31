/*
 * Copyright 2020-2021 Cyface GmbH
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

import static de.cyface.model.Event.EventType.LIFECYCLE_START;
import static de.cyface.model.Event.EventType.LIFECYCLE_STOP;
import static de.cyface.model.Event.EventType.MODALITY_TYPE_CHANGE;
import static de.cyface.model.Modality.BICYCLE;
import static de.cyface.model.Modality.WALKING;
import static de.cyface.protos.model.Measurement.parseFrom;
import static de.cyface.serializer.model.Point3DType.ACCELERATION;
import static de.cyface.serializer.model.Point3DType.DIRECTION;
import static de.cyface.serializer.model.Point3DType.ROTATION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.protobuf.ByteString;

import de.cyface.deserializer.exceptions.InvalidLifecycleEvents;
import de.cyface.model.Event;
import de.cyface.model.MeasurementIdentifier;
import de.cyface.model.MetaData;
import de.cyface.model.Modality;
import de.cyface.model.Point3D;
import de.cyface.model.Point3DImpl;
import de.cyface.model.RawRecord;
import de.cyface.protos.model.LocationRecords;
import de.cyface.protos.model.MeasurementBytes;
import de.cyface.serializer.DataSerializable;
import de.cyface.serializer.Formatter;
import de.cyface.serializer.LocationOffsetter;
import de.cyface.serializer.Point3DSerializer;
import de.cyface.serializer.Serializer;

/**
 * Test for reading measurements from the Cyface binary format.
 *
 * @author Klemens Muthmann
 * @author Armin Schnabel
 * @version 1.0.1
 * @since 1.0.0
 */
class BinaryFormatDeserializerTest {

    /**
     * Expected double accuracy for asserts
     */
    private static final float EPSILON = 0.0000001f;
    private static final long MEASUREMENT_START_TIME = 1556050163000L;
    private static final long TRACK1_PAUSE_TIME = 1556050163050L;
    private static final long TRACK2_RESUME_TIME = 1556050163100L;
    private static final long TRACK2_PAUSE_TIME = 1556050163150L;
    private static final long TRACK3_RESUME_TIME = 1556050163200L;
    private static final long MEASUREMENT_STOP_TIME = 1556050163250L;
    /**
     * The id of the user to add test data for.
     */
    private static final UUID TEST_USER_ID = UUID.randomUUID();

    private final static short PERSISTENCE_FILE_FORMAT_VERSION = 3;

    /**
     * This test evaluates the general workings of reading some binary data from a very short file in the Cyface binary
     * format.
     * <p>
     * This test can be used as a tutorial for how to use this serialization library.
     *
     * @throws IOException Upon failure to read the input data. This fails the test
     */
    @Test
    @DisplayName("Happy Path")
    void test() throws IOException, InvalidLifecycleEvents, UnsupportedFileVersion {
        // Arrange
        final var identifier = new MeasurementIdentifier("test", 1);
        try (final var testData = testData(identifier)) {
            final var uploadDate = new Date();
            final var metaData = MetaData.Companion.create(
                    identifier,
                    "Pixel 3",
                    "Android 9.0.0",
                    "1.2.0-beta1",
                    500.5,
                    TEST_USER_ID,
                    MetaData.CURRENT_VERSION,
                    uploadDate
            );
            final var reader = new BinaryFormatDeserializer(metaData, testData);

            // Act
            final var result = reader.read();

            // Assert
            assertThat(result, notNullValue());
            assertThat(result.getMetaData().getIdentifier(), is(identifier));
            assertThat(result.getMetaData().getDeviceType(), is("Pixel 3"));
            assertThat(result.getMetaData().getOsVersion(), is("Android 9.0.0"));
            assertThat(result.getMetaData().getAppVersion(), is("1.2.0-beta1"));
            assertThat(result.getMetaData().getLength(), is(500.5));
            assertThat(result.getMetaData().getUserId(), is(TEST_USER_ID));
            assertThat(result.getMetaData().getVersion(), is(MetaData.CURRENT_VERSION));
            assertThat(result.getMetaData().getUploadDate(), is(uploadDate));

            final var resultTracks = result.getTracks();
            assertThat(resultTracks, hasSize(3));
            final var firstTrack = resultTracks.get(0);
            final var secondTrack = resultTracks.get(1);
            final var lastTrack = resultTracks.get(2);

            assertThat(firstTrack.getLocationRecords(), hasSize(3));
            assertThat(firstTrack.getAccelerations(), hasSize(3));
            assertThat(firstTrack.getRotations(), hasSize(3));
            assertThat(firstTrack.getDirections(), hasSize(3));

            assertThat(secondTrack.getLocationRecords(), hasSize(3));
            assertThat(secondTrack.getAccelerations(), hasSize(3));
            assertThat(secondTrack.getRotations(), hasSize(3));
            assertThat(secondTrack.getDirections(), hasSize(3));

            // We don't cut the tail at the STOP event
            assertThat(lastTrack.getLocationRecords(), hasSize(4));
            assertThat(lastTrack.getAccelerations(), hasSize(4));
            assertThat(lastTrack.getRotations(), hasSize(4));
            assertThat(lastTrack.getDirections(), hasSize(4));

            assertThat(firstTrack.getLocationRecords(), everyItem(
                    allOf(
                            hasProperty("timestamp", greaterThanOrEqualTo(MEASUREMENT_START_TIME)),
                            hasProperty("timestamp", lessThanOrEqualTo(TRACK1_PAUSE_TIME)),
                            hasProperty("speed", closeTo(0.9900000095367432, 0.1)),
                            hasProperty("accuracy", greaterThanOrEqualTo(17.11)),
                            hasProperty("accuracy", lessThanOrEqualTo(17.15)))));
            assertThat(
                    firstTrack.getLocationRecords(),
                    everyItem(
                            allOf(
                                    hasProperty("latitude", closeTo(51.05001449584961, EPSILON + 0.00001)),
                                    hasProperty("longitude", closeTo(13.709659576416016, EPSILON + 0.00001)))));
            assertThat(secondTrack.getLocationRecords(), everyItem(
                    allOf(
                            hasProperty("timestamp", greaterThanOrEqualTo(TRACK2_RESUME_TIME)),
                            hasProperty("timestamp", lessThanOrEqualTo(TRACK2_PAUSE_TIME)))));
            assertThat(lastTrack.getLocationRecords(), everyItem(
                    allOf(
                            hasProperty("timestamp", greaterThanOrEqualTo(TRACK3_RESUME_TIME)))));

            assertThat(firstTrack.getAccelerations(),
                    hasItem(allOf(
                            hasProperty("x", between(-0.693f, -0.693f + 0.011f)),
                            hasProperty("y", between(5.555f, 5.555f + 0.011f)),
                            hasProperty("z", between(8.079f, 8.079f + 0.011f)),
                            hasProperty("timestamp", equalTo(MEASUREMENT_START_TIME)))));
            assertThat(firstTrack.getRotations(),
                    hasItem(allOf(
                            hasProperty("x", between(-0.693f + 0.020f, -0.693f + 0.031f)),
                            hasProperty("y", between(5.555f + 0.020f, 5.555f + 0.031f)),
                            hasProperty("z", between(8.079f + 0.020f, 8.079f + 0.031f)),
                            hasProperty("timestamp", equalTo(MEASUREMENT_START_TIME)))));
            // Acceleration and Rotation have a precision of 0.001 and Direction 0.01 (see Protobuf message)
            assertThat(firstTrack.getDirections(),
                    hasItem(allOf(
                            hasProperty("x", between(-0.69f + 0.040f, -0.69f + 0.051f)),
                            hasProperty("y", between(5.55f + 0.040f, 5.55f + 0.051f)),
                            hasProperty("z", between(8.07f + 0.040f, 8.07f + 0.051f)),
                            hasProperty("timestamp", equalTo(MEASUREMENT_START_TIME)))));

            assertThat(firstTrack.getAccelerations(),
                    everyItem(allOf(
                            hasProperty("timestamp", greaterThanOrEqualTo(MEASUREMENT_START_TIME)),
                            hasProperty("timestamp", lessThanOrEqualTo(TRACK1_PAUSE_TIME)))));
            assertThat(secondTrack.getAccelerations(),
                    everyItem(allOf(
                            hasProperty("timestamp", greaterThanOrEqualTo(TRACK2_RESUME_TIME)),
                            hasProperty("timestamp", lessThanOrEqualTo(TRACK2_PAUSE_TIME)))));
            assertThat(lastTrack.getAccelerations(),
                    everyItem(allOf(
                            hasProperty("timestamp", greaterThanOrEqualTo(TRACK3_RESUME_TIME)))));
        }
    }

    /**
     * This test generates random sensor values within the supported value range and deserializes the bytes to ensure
     * the values did not change.
     */
    @DisplayName("Happy Path test for the serialization and deserialization of 3d points.")
    @Test
    void testSerializeDeserialize() throws IOException, InvalidLifecycleEvents {

        // Arrange - Events: start, stop (1 track)
        final var batches = 100;
        final var sensorPointsPerBatch = 100;
        final var sensorPoints = batches * sensorPointsPerBatch;
        final var measurementStart = 1_660_000_000_000L;
        final var measurementEnd = measurementStart + sensorPoints * 10L + 1L;
        final var events = new ArrayList<de.cyface.protos.model.Event>(3);
        events.add(de.cyface.protos.model.Event.newBuilder()
                .setTimestamp(measurementStart)
                .setType(de.cyface.protos.model.Event.EventType.valueOf(LIFECYCLE_START.getDatabaseIdentifier()))
                .build());
        // Modality selected during start
        events.add(de.cyface.protos.model.Event.newBuilder()
                .setTimestamp(measurementStart)
                .setType(de.cyface.protos.model.Event.EventType.valueOf(MODALITY_TYPE_CHANGE.getDatabaseIdentifier()))
                .setValue(BICYCLE.getDatabaseIdentifier())
                .build());
        // The measurement stop time needs to be after the last sensor points we generate (start + i * 10L + 1L)
        events.add(de.cyface.protos.model.Event.newBuilder()
                .setTimestamp(measurementEnd)
                .setType(de.cyface.protos.model.Event.EventType.valueOf(LIFECYCLE_STOP.getDatabaseIdentifier()))
                .build());

        // Arrange - Locations: more than 0 or else there no track is generated by the deserialization
        final var identifier = new MeasurementIdentifier("test", 1);
        final var locationBuilder = LocationRecords.newBuilder();
        final LocationOffsetter offsetter = new LocationOffsetter();
        // Location 1
        final Formatter.Location formatted1 = new Formatter.Location(measurementStart + 1000L, 51.05001449584961,
                13.709659576416016, 0.9900000095367432, 17.15);
        final Formatter.Location offsets1 = offsetter.offset(formatted1);
        locationBuilder.addTimestamp(offsets1.getTimestamp())
                .addLatitude(offsets1.getLatitude())
                .addLongitude(offsets1.getLongitude())
                .addAccuracy(offsets1.getAccuracy())
                .addSpeed(offsets1.getSpeed());
        // Location 2
        final Formatter.Location formatted2 = new Formatter.Location(measurementEnd - 1000L, 51.05001430188195,
                13.70965955217006, 0.9816101789474487, 17.13);
        final Formatter.Location offsets2 = offsetter.offset(formatted2);
        locationBuilder.addTimestamp(offsets2.getTimestamp())
                .addLatitude(offsets2.getLatitude())
                .addLongitude(offsets2.getLongitude())
                .addAccuracy(offsets2.getAccuracy())
                .addSpeed(offsets2.getSpeed());
        final var locations = locationBuilder.build();

        // Arrange - Sensor points: 100 batches with each 100 points
        final var maxAcceleration = 16.0; // m/s²
        final var maxRotation = 2 * 34.906585; // rad/s
        final var maxDirection = 4911.994; // µT
        final List<List<Point3D>> accelerationBatches = new ArrayList<>();
        final List<List<Point3D>> rotationBatches = new ArrayList<>();
        final List<List<Point3D>> directionBatches = new ArrayList<>();
        for (int b = 0; b < batches; b++) {
            final List<Point3D> accelerations = new ArrayList<>();
            final List<Point3D> rotations = new ArrayList<>();
            final List<Point3D> directions = new ArrayList<>();
            for (int i = 0; i < sensorPointsPerBatch; i++) {
                final var signum = i % 2 == 0 ? -1 : 1;
                final var acceleration = new Point3DImpl(signum * Math.random() * maxAcceleration,
                        signum * Math.random() * maxAcceleration, signum * Math.random() * maxAcceleration,
                        measurementStart + i * 10L);
                final var rotation = new Point3DImpl(signum * Math.random() * maxRotation,
                        signum * Math.random() * maxRotation, signum * Math.random() * maxRotation,
                        measurementStart + i * 10L);
                final var direction = new Point3DImpl(signum * Math.random() * maxDirection,
                        signum * Math.random() * maxDirection, signum * Math.random() * maxDirection,
                        measurementStart + i * 10L);
                accelerations.add(acceleration);
                rotations.add(rotation);
                directions.add(direction);
            }
            accelerationBatches.add(accelerations);
            rotationBatches.add(rotations);
            directionBatches.add(directions);
        }

        // Act - Serialize
        // Write two sensor data batches in the `cyfa/r/d` format as on the mobile devices (e.g. Point3DFile.append)
        final var cyfa = new ByteArrayOutputStream();
        final var cyfd = new ByteArrayOutputStream();
        final var cyfr = new ByteArrayOutputStream();
        for (final var accelerations : accelerationBatches) {
            final var accelerationsBatch = Point3DSerializer.serialize(accelerations, ACCELERATION);
            cyfa.write(accelerationsBatch);
        }
        for (final var rotations : rotationBatches) {
            final var rotationsBatch = Point3DSerializer.serialize(rotations, ROTATION);
            cyfr.write(rotationsBatch);
        }
        for (final var directions : directionBatches) {
            final var directionsBatch = Point3DSerializer.serialize(directions, DIRECTION);
            cyfd.write(directionsBatch);
        }

        // Act: Build Transfer file
        final var accelerationBytes = cyfa.toByteArray();
        final var rotationBytes = cyfr.toByteArray();
        final var directionBytes = cyfd.toByteArray();
        // MeasurementBytes allows us to inject bytes without parsing them (to save resources on the mobile devices)
        final var builder = MeasurementBytes.newBuilder()
                .setFormatVersion(PERSISTENCE_FILE_FORMAT_VERSION)
                .addAllEvents(events)
                .setLocationRecords(locations)
                .setAccelerationsBinary(ByteString.copyFrom(accelerationBytes))
                .setRotationsBinary(ByteString.copyFrom(rotationBytes))
                .setDirectionsBinary(ByteString.copyFrom(directionBytes));
        // During transfer, the measurement bytes would be succeeded by 2 bytes of the transfer file format (short)
        final var measurementBytes = builder.build().toByteArray();

        // Act - Deserialize
        final var parsedMeasurement = parseFrom(measurementBytes);
        final var deserializedEvents = EventDeserializer.deserialize(parsedMeasurement.getEventsList());
        final var deserializedLocations = LocationDeserializer.deserialize(parsedMeasurement.getLocationRecords());
        final var accelerations = Point3DDeserializer
                .accelerations(parsedMeasurement.getAccelerationsBinary().getAccelerationsList());
        final var rotations = Point3DDeserializer.rotations(parsedMeasurement.getRotationsBinary().getRotationsList());
        final var directions = Point3DDeserializer
                .directions(parsedMeasurement.getDirectionsBinary().getDirectionsList());
        final var trackBuilder = new TrackBuilder();
        final var metaData = MetaData.Companion.create(
                identifier,
                "Pixel 3",
                "Android 12.0.0",
                "3.0.2",
                0.0,
                TEST_USER_ID,
                MetaData.CURRENT_VERSION,
                new Date()
        );
        final var tracks = trackBuilder.build(deserializedLocations, deserializedEvents, accelerations, rotations,
                directions, identifier);
        final var deserializedMeasurement = de.cyface.model.Measurement.create(metaData, tracks);

        // Assert
        assertThat(parsedMeasurement.getFormatVersion(), is(equalTo(3)));
        assertThat(deserializedMeasurement.getTracks().size(), is(equalTo(1)));
        final var track = deserializedMeasurement.getTracks().get(0);
        // Check number of sensor points
        final var deserializedAccelerations = track.getAccelerations();
        final var deserializedRotations = track.getRotations();
        final var deserializedDirections = track.getDirections();
        assertThat(deserializedAccelerations.size(), is(equalTo(sensorPoints)));
        assertThat(deserializedRotations.size(), is(equalTo(sensorPoints)));
        assertThat(deserializedDirections.size(), is(equalTo(sensorPoints)));
        // Check first sensor points values
        final var accelerationPrecision = 0.001;
        final var rotationPrecision = 0.001;
        final var directionPrecision = 0.01;
        final var expectedFirstAcceleration = accelerationBatches.get(0).get(0);
        assertThat((double)deserializedAccelerations.get(0).getX(),
                is(closeTo(expectedFirstAcceleration.getX(), accelerationPrecision)));
        assertThat((double)deserializedAccelerations.get(0).getY(),
                is(closeTo(expectedFirstAcceleration.getY(), accelerationPrecision)));
        assertThat((double)deserializedAccelerations.get(0).getZ(),
                is(closeTo(expectedFirstAcceleration.getZ(), accelerationPrecision)));
        assertThat(deserializedAccelerations.get(0).getTimestamp(),
                is(equalTo(expectedFirstAcceleration.getTimestamp())));
        final var expectedFirstRotation = rotationBatches.get(0).get(0);
        assertThat((double)deserializedRotations.get(0).getX(),
                is(closeTo(expectedFirstRotation.getX(), rotationPrecision)));
        assertThat((double)deserializedRotations.get(0).getY(),
                is(closeTo(expectedFirstRotation.getY(), rotationPrecision)));
        assertThat((double)deserializedRotations.get(0).getZ(),
                is(closeTo(expectedFirstRotation.getZ(), rotationPrecision)));
        assertThat(deserializedRotations.get(0).getTimestamp(), is(equalTo(expectedFirstRotation.getTimestamp())));
        final var expectedFirstDirection = directionBatches.get(0).get(0);
        assertThat((double)deserializedDirections.get(0).getX(),
                is(closeTo(expectedFirstDirection.getX(), directionPrecision)));
        assertThat((double)deserializedDirections.get(0).getY(),
                is(closeTo(expectedFirstDirection.getY(), directionPrecision)));
        assertThat((double)deserializedDirections.get(0).getZ(),
                is(closeTo(expectedFirstDirection.getZ(), directionPrecision)));
        assertThat(deserializedDirections.get(0).getTimestamp(), is(equalTo(expectedFirstDirection.getTimestamp())));
        // Check last sensor points values
        final var expectedLastAcceleration = accelerationBatches.get(batches - 1).get(sensorPointsPerBatch - 1);
        assertThat((double)deserializedAccelerations.get(sensorPoints - 1).getX(),
                is(closeTo(expectedLastAcceleration.getX(), accelerationPrecision)));
        assertThat((double)deserializedAccelerations.get(sensorPoints - 1).getY(),
                is(closeTo(expectedLastAcceleration.getY(), accelerationPrecision)));
        assertThat((double)deserializedAccelerations.get(sensorPoints - 1).getZ(),
                is(closeTo(expectedLastAcceleration.getZ(), accelerationPrecision)));
        assertThat(deserializedAccelerations.get(sensorPoints - 1).getTimestamp(),
                is(equalTo(expectedLastAcceleration.getTimestamp())));
        final var expectedLastRotation = rotationBatches.get(batches - 1).get(sensorPointsPerBatch - 1);
        assertThat((double)deserializedRotations.get(sensorPoints - 1).getX(),
                is(closeTo(expectedLastRotation.getX(), rotationPrecision)));
        assertThat((double)deserializedRotations.get(sensorPoints - 1).getY(),
                is(closeTo(expectedLastRotation.getY(), rotationPrecision)));
        assertThat((double)deserializedRotations.get(sensorPoints - 1).getZ(),
                is(closeTo(expectedLastRotation.getZ(), rotationPrecision)));
        assertThat(deserializedRotations.get(sensorPoints - 1).getTimestamp(),
                is(equalTo(expectedLastRotation.getTimestamp())));
        final var expectedLastDirection = directionBatches.get(batches - 1).get(sensorPointsPerBatch - 1);
        assertThat((double)deserializedDirections.get(sensorPoints - 1).getX(),
                is(closeTo(expectedLastDirection.getX(), directionPrecision)));
        assertThat((double)deserializedDirections.get(sensorPoints - 1).getY(),
                is(closeTo(expectedLastDirection.getY(), directionPrecision)));
        assertThat((double)deserializedDirections.get(sensorPoints - 1).getZ(),
                is(closeTo(expectedLastDirection.getZ(), directionPrecision)));
        assertThat(deserializedDirections.get(sensorPoints - 1).getTimestamp(),
                is(equalTo(expectedLastDirection.getTimestamp())));
    }

    private Matcher<?> between(final float lower, final float upper) {
        final var floatPrecision = 0.000001f;
        return is(both(greaterThanOrEqualTo(lower - floatPrecision)).and(lessThanOrEqualTo(upper + floatPrecision)));
    }

    /**
     * Generate a test fixture.
     *
     * @param measurementIdentifier The identifier of the measurement to create the test data for.
     * @return An <code>InputStream</code> with binary test data
     * @throws IOException If writing or reading the binary data fails
     */
    InputStream testData(final MeasurementIdentifier measurementIdentifier) throws IOException {
        final var serializable = new DataSerializable(testEvents(), testLocations(measurementIdentifier),
                testAccelerations(), testRotations(), testDirections());
        final var serializedFile = Serializer.serialize(serializable, "tmp");
        return Files.newInputStream(serializedFile);
    }

    /**
     * Fixture of 6 life-cycle events and 3 modality type changed events.
     *
     * @return A stream os serialized test events
     */
    List<Event> testEvents() {
        final var events = new ArrayList<Event>(9);
        // Track 1
        events.add(new Event(LIFECYCLE_START, MEASUREMENT_START_TIME, null));
        // Modality selected during start
        events.add(new Event(MODALITY_TYPE_CHANGE, MEASUREMENT_START_TIME,
                WALKING.getDatabaseIdentifier()));
        events.add(new Event(Event.EventType.LIFECYCLE_PAUSE, TRACK1_PAUSE_TIME, null));

        // Changes modality during first pause
        events.add(
                new Event(MODALITY_TYPE_CHANGE, TRACK2_RESUME_TIME - 10,
                        Modality.BUS.getDatabaseIdentifier()));

        // Track 2
        events.add(new Event(Event.EventType.LIFECYCLE_RESUME, TRACK2_RESUME_TIME, null));
        events.add(new Event(Event.EventType.LIFECYCLE_PAUSE, TRACK2_PAUSE_TIME, null));

        // Track 3
        events.add(new Event(Event.EventType.LIFECYCLE_RESUME, TRACK3_RESUME_TIME, null));
        // Changes modality during track 3
        events.add(
                new Event(MODALITY_TYPE_CHANGE, TRACK3_RESUME_TIME + 10,
                        Modality.BICYCLE.getDatabaseIdentifier()));
        events.add(new Event(LIFECYCLE_STOP, MEASUREMENT_STOP_TIME, null));
        return events;
    }

    /**
     * Fixture of two batches with 6 accelerations each.
     *
     * @return An ordered list of some test accelerations
     */
    List<List<? extends Point3D>> testAccelerations() {
        final var batches = new ArrayList<List<? extends Point3D>>();
        final var batch1 = new ArrayList<Point3D>(6);

        // Track 1
        batch1.add(point3D(0, MEASUREMENT_START_TIME));
        batch1.add(point3D(1, MEASUREMENT_START_TIME + 25));
        batch1.add(point3D(2, TRACK1_PAUSE_TIME));

        batch1.add(point3D(3, TRACK1_PAUSE_TIME + 25));

        // Track 2
        batch1.add(point3D(4, TRACK2_RESUME_TIME));
        batch1.add(point3D(5, TRACK2_RESUME_TIME + 25));
        final var batch2 = new ArrayList<Point3D>(6);
        batch2.add(point3D(6, TRACK2_PAUSE_TIME));

        batch2.add(point3D(7, TRACK2_PAUSE_TIME + 25));

        // Track 3
        batch2.add(point3D(8, TRACK3_RESUME_TIME));
        batch2.add(point3D(9, TRACK3_RESUME_TIME + 25));
        batch2.add(point3D(10, MEASUREMENT_STOP_TIME));

        batch2.add(point3D(11, MEASUREMENT_STOP_TIME + 25));
        batches.add(batch1);
        batches.add(batch2);
        return batches;
    }

    /**
     * Fixture of two batches with 6 rotations each.
     *
     * @return An ordered list of some test rotations
     */
    List<List<? extends Point3D>> testRotations() {
        final var batches = new ArrayList<List<? extends Point3D>>();
        final var batch1 = new ArrayList<Point3D>(6);
        batch1.add(point3D(20, MEASUREMENT_START_TIME));
        batch1.add(point3D(21, MEASUREMENT_START_TIME + 25));
        batch1.add(point3D(22, TRACK1_PAUSE_TIME));
        batch1.add(point3D(23, TRACK1_PAUSE_TIME + 25));
        batch1.add(point3D(24, TRACK2_RESUME_TIME));
        batch1.add(point3D(25, TRACK2_RESUME_TIME + 25));
        final var batch2 = new ArrayList<Point3D>(6);
        batch2.add(point3D(26, TRACK2_PAUSE_TIME));
        batch2.add(point3D(27, TRACK2_PAUSE_TIME + 25));
        batch2.add(point3D(28, TRACK3_RESUME_TIME));
        batch2.add(point3D(29, TRACK3_RESUME_TIME + 25));
        batch2.add(point3D(30, MEASUREMENT_STOP_TIME));
        batch2.add(point3D(31, MEASUREMENT_STOP_TIME + 25));
        batches.add(batch1);
        batches.add(batch2);
        return batches;
    }

    /**
     * Fixture of two batches with 6 directions each.
     *
     * @return An ordered list of some test direction points
     */
    List<List<? extends Point3D>> testDirections() {
        final var batches = new ArrayList<List<? extends Point3D>>();
        final var batch1 = new ArrayList<Point3D>(6);
        batch1.add(point3D(40, MEASUREMENT_START_TIME));
        batch1.add(point3D(41, MEASUREMENT_START_TIME + 25));
        batch1.add(point3D(42, TRACK1_PAUSE_TIME));
        batch1.add(point3D(43, TRACK1_PAUSE_TIME + 25));
        batch1.add(point3D(44, TRACK2_RESUME_TIME));
        batch1.add(point3D(45, TRACK2_RESUME_TIME + 25));
        final var batch2 = new ArrayList<Point3D>(6);
        batch2.add(point3D(46, TRACK2_PAUSE_TIME));
        batch2.add(point3D(47, TRACK2_PAUSE_TIME + 25));
        batch2.add(point3D(48, TRACK3_RESUME_TIME));
        batch2.add(point3D(49, TRACK3_RESUME_TIME + 25));
        batch2.add(point3D(50, MEASUREMENT_STOP_TIME));
        batch2.add(point3D(51, MEASUREMENT_STOP_TIME + 25));
        batches.add(batch1);
        batches.add(batch2);
        return batches;
    }

    private Point3DImpl point3D(final int index, final long timestamp) {
        return new Point3DImpl(-0.6931915283203125F + index * 0.001, 5.5552825927734375F + index * 0.001,
                8.079452514648438F + index * 0.001, timestamp);
    }

    /**
     * Fixture of ten geolocations,
     *
     * @param measurementIdentifier The identifier of the measurement to create locations for.
     * @return An ordered list of some test locations
     */
    List<RawRecord> testLocations(final MeasurementIdentifier measurementIdentifier) {
        final var geoLocations = new ArrayList<RawRecord>(12);
        geoLocations
                .add(new RawRecord(measurementIdentifier, MEASUREMENT_START_TIME, 51.05001449584961, 13.709659576416016,
                        17.15,
                        0.9900000095367432, Modality.UNKNOWN));
        geoLocations
                .add(new RawRecord(measurementIdentifier, MEASUREMENT_START_TIME + 25, 51.05001430188195,
                        13.70965955217006, 17.13,
                        0.9816101789474487, Modality.UNKNOWN));
        geoLocations
                .add(new RawRecord(measurementIdentifier, TRACK1_PAUSE_TIME, 51.05001410791429, 13.709659527924101,
                        17.11, 0.9732203483581543, Modality.UNKNOWN));
        geoLocations
                .add(new RawRecord(measurementIdentifier, TRACK1_PAUSE_TIME + 25, 51.050013913946636,
                        13.709659503678145,
                        17.10, 0.9648305177688599, Modality.UNKNOWN));
        geoLocations
                .add(new RawRecord(measurementIdentifier, TRACK2_RESUME_TIME, 51.05001371997898, 13.709659479432187,
                        17.08, 0.9564406871795654, Modality.UNKNOWN));
        geoLocations
                .add(new RawRecord(measurementIdentifier, TRACK2_RESUME_TIME + 25, 51.05001352601132, 13.70965945518623,
                        17.06, 0.948050856590271, Modality.UNKNOWN));
        geoLocations
                .add(new RawRecord(measurementIdentifier, TRACK2_PAUSE_TIME, 51.05001333204366, 13.709659430940272,
                        17.04, 0.9396610260009766, Modality.UNKNOWN));
        geoLocations
                .add(new RawRecord(measurementIdentifier, TRACK2_PAUSE_TIME + 25, 51.050013138076004,
                        13.709659406694316,
                        17.02, 0.9312711954116821, Modality.UNKNOWN));
        geoLocations
                .add(new RawRecord(measurementIdentifier, TRACK3_RESUME_TIME, 51.050012944108346, 13.709659382448358,
                        17.00, 0.9228813648223877, Modality.UNKNOWN));
        geoLocations
                .add(new RawRecord(measurementIdentifier, TRACK3_RESUME_TIME + 25, 51.05001275014069,
                        13.709659358202401,
                        16.99, 0.9144915342330933, Modality.UNKNOWN));
        geoLocations
                .add(new RawRecord(measurementIdentifier, MEASUREMENT_STOP_TIME, 51.05001410791429, 51.05001430188195,
                        16.98, 0.911, Modality.UNKNOWN));
        geoLocations
                .add(new RawRecord(measurementIdentifier, MEASUREMENT_STOP_TIME + 25, 51.05001333204366,
                        13.709659479432187, 16.97, 0.910, Modality.UNKNOWN));

        return geoLocations;
    }
}
