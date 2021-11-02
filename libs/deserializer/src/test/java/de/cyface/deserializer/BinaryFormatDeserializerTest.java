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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.apache.commons.lang3.Validate;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.cyface.deserializer.exceptions.InvalidLifecycleEvents;
import de.cyface.model.Event;
import de.cyface.model.MeasurementIdentifier;
import de.cyface.model.MetaData;
import de.cyface.model.Modality;
import de.cyface.model.Point3D;
import de.cyface.model.Point3DImpl;
import de.cyface.model.RawRecord;
import de.cyface.serializer.DataSerializable;
import de.cyface.serializer.Serializer;

/**
 * Test for reading measurements from the Cyface binary format.
 *
 * @author Klemens Muthmann
 * @author Armin Schnabel
 * @version 1.0.0
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
     * This test evaluates the general workings of reading some binary data from a very short file in the Cyface binary
     * format.
     *
     * @throws IOException Upon failure to read the input data. This fails the test
     */
    //@Test
    @DisplayName("Happy Path")
    void test() throws IOException, InvalidLifecycleEvents, UnsupportedFileVersion {
        // Arrange
        final var identifier = new MeasurementIdentifier("test", 1);
        try (final var testData = testData(identifier)) {
            final var metaData = new MetaData(identifier, "Pixel 3", "Android 9.0.0", "1.2.0-beta1", 500.5, "admin",
                    MetaData.CURRENT_VERSION);
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
            assertThat(result.getMetaData().getUsername(), is("admin"));
            assertThat(result.getMetaData().getVersion(), is(MetaData.CURRENT_VERSION));

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
                            hasProperty("x", between(-0.693f,-0.693f+0.011f)),
                            hasProperty("y", between(5.555f, 5.555f+0.011f)),
                            hasProperty("z", between(8.079f, 8.079f+0.011f)),
                            hasProperty("timestamp", equalTo(MEASUREMENT_START_TIME)))));
            assertThat(firstTrack.getRotations(),
                    hasItem(allOf(
                            hasProperty("x", between(-0.693f+0.020f, -0.693f+0.031f)),
                            hasProperty("y", between(5.555f+0.020f,5.555f+0.031f)),
                            hasProperty("z", between(8.079f+0.020f, 8.079f+0.031f)),
                            hasProperty("timestamp", equalTo(MEASUREMENT_START_TIME)))));
            // Acceleration and Rotation have a precision of 0.001 and Direction 0.01 (see Protobuf message)
            assertThat(firstTrack.getDirections(),
                    hasItem(allOf(
                            hasProperty("x", between(-0.69f+0.040f, -0.69f+0.051f)),
                            hasProperty("y", between(5.55f+0.040f,5.55f+0.051f)),
                            hasProperty("z", between(8.07f+0.040f, 8.07f+0.051f)),
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
     * This test evaluates the general workings of writing and reading binary data from a medium-sized file in the Cyface binary
     * format.
     *
     * @throws IOException Upon failure to read the input data. This fails the test
     */
    //@Test
    void test_withLargeNumberOfSensorData() throws IOException, InvalidLifecycleEvents, UnsupportedFileVersion {
        // Arrange
        final var n = 100_000;
        final var identifier = new MeasurementIdentifier("test", 1);
        try (final var testData = testData(identifier, n)) {
            final MetaData metaData = new MetaData(identifier, "Pixel 3", "Android 9.0.0", "1.2.0-beta1", 500.5,
                    "admin", MetaData.CURRENT_VERSION);
            final var reader = new BinaryFormatDeserializer(metaData, testData);

            // Act
            final var result = reader.read();

            // Assert
            final var resultTracks = result.getTracks();
            assertThat(resultTracks, hasSize(1));
            final var track = resultTracks.get(0);
            assertThat(track.getLocationRecords(), hasSize(12));
            assertThat(track.getAccelerations(), hasSize(n));
            assertThat(track.getRotations(), hasSize(n));
            assertThat(track.getDirections(), hasSize(n));
            final var startX = Math.round(-0.6931915283203125F*1000F)/1000F;
            final var startY = Math.round(5.5552825927734375F*1000F)/1000F;
            final var startZ = Math.round(8.079452514648438F*1000F)/1000F;
            assertThat(track.getAccelerations(),
                    hasItem(allOf(
                            hasProperty("x", between(startX,startX + n * 0.001f)),
                            hasProperty("y", between(startY, startY + n * 0.001f)),
                            hasProperty("z", between(startZ, startZ + n * 0.001f)))));
            assertThat(track.getRotations(),
                    hasItem(allOf(
                            hasProperty("x", between(startX,startX + n * 0.001f)),
                            hasProperty("y", between(startY, startY + n * 0.001f)),
                            hasProperty("z", between(startZ, startZ + n * 0.001f)))));
            // Acceleration and Rotation have a precision of 0.001 and Direction 0.01 (see Protobuf message)
            assertThat(track.getDirections(),
                    hasItem(allOf(
                            hasProperty("x", between(startX,startX + n * 0.001f)),
                            hasProperty("y", between(startY, startY + n * 0.001f)),
                            hasProperty("z", between(startZ, startZ + n * 0.001f)))));
            assertThat(track.getAccelerations(),
                    everyItem(allOf(
                            hasProperty("timestamp", greaterThanOrEqualTo(MEASUREMENT_START_TIME)),
                            hasProperty("timestamp", lessThanOrEqualTo(MEASUREMENT_START_TIME+n*10L)))));
            // Check last point to make sure offsetting works correctly
            final var lastAccel = track.getAccelerations().get(track.getAccelerations().size()-1);
            assertThat((double) lastAccel.getX(), closeTo(startX + (float) n * 0.001f, 0.002f));
            assertThat((double) lastAccel.getY(), closeTo(startY + (float) n * 0.001f, 0.002f));
            assertThat((double) lastAccel.getZ(), closeTo(startZ + (float) n * 0.001f, 0.002f));
        }
    }
    /**
     * This test evaluates the general workings of reading some binary data from a very short file in the Cyface binary
     * format.
     *
     * @throws IOException Upon failure to read the input data. This fails the test
     */
    //@Test
    void testDecompress() throws IOException, URISyntaxException {

        // UNCOMPRESS => ccyf to cyf
        final var identifier = new MeasurementIdentifier(did,mid);
        final var fileName = identifier.getDeviceIdentifier() + "_" + identifier.getMeasurementIdentifier();
        final var resource = this.getClass().getResource("/"+fileName+".ccyf");
        Validate.notNull(resource);
        try (final var compressedData = Files.newInputStream(Paths.get(resource.toURI()))) {

            try (InflaterInputStream uncompressedInput = new InflaterInputStream(compressedData, new Inflater(true))) {
                final var targetFile = Paths.get(fileName+".cyf").toFile();
                java.nio.file.Files.copy(
                        uncompressedInput,
                        targetFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    final String pixel3a = "bbd76998-3701-4737-a2b1-0630931160c2";
    final String huaweiP30 = "ad11b686-4d4f-46bf-b14b-fc5e0c1c152f";
    final String did = huaweiP30;
    final int mid = 7;

    /**
     * This test evaluates the general workings of reading some binary data from a very short file in the Cyface binary
     * format.
     *
     * @throws IOException Upon failure to read the input data. This fails the test
     */
    //@Test
    void testReSerialize_withHalfPrecisionFloat() throws IOException, InvalidLifecycleEvents, UnsupportedFileVersion, URISyntaxException {

        // UNCOMPRESS => ccyf to cyf
        final var identifier = new MeasurementIdentifier(did,mid);
        final var fileName = identifier.getDeviceIdentifier() + "_" + identifier.getMeasurementIdentifier();
        final var testMeasurementCompressed = this.getClass().getResource("/"+fileName+".ccyf");
        Validate.notNull(testMeasurementCompressed);
        System.out.println("Reading file of size: " + new File(testMeasurementCompressed.getFile()).length());
        try (final var compressedData = Files.newInputStream(Paths.get(testMeasurementCompressed.toURI()))) {

            // Read the sample measurement from the `ccyf` file
            final MetaData metaData = new MetaData(identifier, "Pixel 3a", "Android 12", "3.0.0-alpha1", 1982.4112927913666,
                    "admin", MetaData.CURRENT_VERSION);
            final var reader = new BinaryFormatDeserializer(metaData, compressedData);
            final var measurement = reader.read();

            // Convert the sensor data from fixed-comma-numbers to half-precision float (which returns shorts)

            // FIXME: DON'T USE THE /1000 und /100 or else it's not the "tested 16-bit float"!
            // Or is it? As I store the shorts as 0.001 which makes them 1 again after /1000

            final var allConvertedAccelerations = new ArrayList<Point3D>();
            final var allConvertedRotations = new ArrayList<Point3D>();
            final var allConvertedDirections = new ArrayList<Point3D>();
            final var allConvertedLocations = new ArrayList<RawRecord>();
            for (int i = 0; i < measurement.getTracks().size(); i++) {
                final var track = measurement.getTracks().get(i);

                ///*final var convertedTracks =*/ measurement.getTracks()/*.map*/.forEach(track -> {
                final var fixCommaPrecisionAccel = 1000.0f;
                final var convertedAccelerations = new ArrayList<Point3D>();
                //final var convertedAccelerations = track.getAccelerations().stream().map(point -> {
                for (int j = 0; j < track.getAccelerations().size(); j++) {
                    final var point = track.getAccelerations().get(j);
                    // In order to use the current measurement.protos
                    // we store the short in the float but with the fixed-comma position in mind:
                    // (short) 123 => 0.123, as this is then encoded by Protobuf to 123 var-int which is
                    // the type closest to short, even using offsets/var-int/zig-zack
                    final var halfPrecisionX = new HalfPrecisionFloat(point.getX());
                    final var halfPrecisionY = new HalfPrecisionFloat(point.getY());
                    final var halfPrecisionZ = new HalfPrecisionFloat(point.getZ());
                    final var x = halfPrecisionX.getHalfPrecisionAsShort();
                    final var y = halfPrecisionY.getHalfPrecisionAsShort();
                    final var z = halfPrecisionZ.getHalfPrecisionAsShort();
                    final var pseudoFloatX = 1.0f / fixCommaPrecisionAccel * x;
                    final var pseudoFloatY = 1.0f / fixCommaPrecisionAccel * y;
                    final var pseudoFloatZ = 1.0f / fixCommaPrecisionAccel * z;
                    if (i == 0 && (j == 0 || j == track.getAccelerations().size()-1)) {
                        System.out.println("accel no. " + j);
                        System.out.println("convert x " + point.getX() + " to half-precision-float " + halfPrecisionX.getFullDouble() + " to short " + x + " to pseudo-float " + pseudoFloatX);
                        System.out.println("convert y " + point.getY() + " to half-precision-float " + halfPrecisionY.getFullDouble() + " to short " + y + " to pseudo-float " + pseudoFloatY);
                        System.out.println("convert z " + point.getZ() + " to half-precision-float " + halfPrecisionZ.getFullDouble() + " to short " + z + " to pseudo-float " + pseudoFloatZ);
                    }
                    //return new Point3DImpl(pseudoFloatX, pseudoFloatY, pseudoFloatZ, point.getTimestamp());
                    final var retPoint = new Point3DImpl(pseudoFloatX, pseudoFloatY, pseudoFloatZ, point.getTimestamp());
                    convertedAccelerations.add(retPoint);
                    //}).collect(Collectors.toList());
                }
                allConvertedAccelerations.addAll(convertedAccelerations);

                final var fixCommaPrecisionRota = 1000.0f;
                final var convertedRotations = track.getRotations().stream().map(point -> {
                    // In order to use the current measurement.protos
                    // we store the short in the float but with the fixed-comma position in mind:
                    // (short) 123 => 0.123, as this is then encoded by Protobuf to 123 var-int which is
                    // the type closest to short, even using offsets/var-int/zig-zack
                    final var x = new HalfPrecisionFloat(point.getX()).getHalfPrecisionAsShort();
                    final var y = new HalfPrecisionFloat(point.getY()).getHalfPrecisionAsShort();
                    final var z = new HalfPrecisionFloat(point.getZ()).getHalfPrecisionAsShort();
                    final var pseudoFloatX = 1.0f / fixCommaPrecisionRota * x;
                    final var pseudoFloatY = 1.0f / fixCommaPrecisionRota * y;
                    final var pseudoFloatZ = 1.0f / fixCommaPrecisionRota * z;
                    return new Point3DImpl(pseudoFloatX, pseudoFloatY, pseudoFloatZ, point.getTimestamp());
                }).collect(Collectors.toList());
                allConvertedRotations.addAll(convertedRotations);

                final var fixCommaPrecisionDirec = 100.0f;
                final var convertedDirections = track.getDirections().stream().map(point -> {
                    // In order to use the current measurement.protos
                    // we store the short in the float but with the fixed-comma position in mind:
                    // (short) 123 => 1.23, as this is then encoded by Protobuf to 123 var-int which is
                    // the type closest to short, even using offsets/var-int/zig-zack
                    final var x = new HalfPrecisionFloat(point.getX()).getHalfPrecisionAsShort();
                    final var y = new HalfPrecisionFloat(point.getY()).getHalfPrecisionAsShort();
                    final var z = new HalfPrecisionFloat(point.getZ()).getHalfPrecisionAsShort();
                    final var pseudoFloatX = 1.0f / fixCommaPrecisionDirec * x;
                    final var pseudoFloatY = 1.0f / fixCommaPrecisionDirec * y;
                    final var pseudoFloatZ = 1.0f / fixCommaPrecisionDirec * z;
                    return new Point3DImpl(pseudoFloatX, pseudoFloatY, pseudoFloatZ, point.getTimestamp());
                }).collect(Collectors.toList());
                allConvertedDirections.addAll(convertedDirections);

                final var convertedLocations = track.getLocationRecords().stream().map(location -> {
                    return new RawRecord(identifier, location.getTimestamp(), location.getLatitude(), location.getLongitude(),
                            location.getAccuracy(), location.getSpeed(), location.getModality());
                }).collect(Collectors.toList());
                allConvertedLocations.addAll(convertedLocations);

                /*final var convertedTrack = new Track();
                convertedTrack.setAccelerations(convertedAccelerations);
                convertedTrack.setRotations(convertedRotations);
                convertedTrack.setDirections(convertedDirections);
                return convertedTrack;*/
            //});//.collect(Collectors.toList());
            }
            /*final var convertedMeasurement = new Measurement();
            convertedMeasurement.setMetaData(metaData);
            convertedMeasurement.setTracks(convertedTracks);*/

            // Serialize converted measurement
            final var serializable = new DataSerializable(new ArrayList<>(), allConvertedLocations,
                    Collections.singletonList(allConvertedAccelerations), Collections.singletonList(allConvertedRotations), Collections.singletonList(allConvertedDirections));
            final var reSerializedCompressed = Serializer.serialize(serializable, "tmp");

            // Write compressed file
            final var targetCompressedFile = Paths.get(fileName+"_16bit.ccyf").toFile();
            java.nio.file.Files.copy(
                    reSerializedCompressed,
                    targetCompressedFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);

            // Write uncompressed file
            try (final var reSerializedCompressedInput = Files.newInputStream(reSerializedCompressed)) {
                try (InflaterInputStream reSerializedUncompressedInput = new InflaterInputStream(reSerializedCompressedInput, new Inflater(true))) {
                    final var targetFile = Paths.get(fileName+"_16bit.cyf").toFile();
                    java.nio.file.Files.copy(
                            reSerializedUncompressedInput,
                            targetFile.toPath(),
                            StandardCopyOption.REPLACE_EXISTING);
                }
            }

        }
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
                testPoint3Ds(), testRotations(), testDirections());
        final var serializedFile = Serializer.serialize(serializable, "tmp");
        return Files.newInputStream(serializedFile);
    }

    /**
     * Generate a test fixture.
     *
     * @param measurementIdentifier The identifier of the measurement to create the test data for.
     * @return An <code>InputStream</code> with binary test data
     * @throws IOException If writing or reading the binary data fails
     */
    InputStream testData(final MeasurementIdentifier measurementIdentifier, @SuppressWarnings("SameParameterValue") final int n) throws IOException {

        final var events = new ArrayList<Event>(9);
        events.add(new Event(Event.EventType.LIFECYCLE_START, MEASUREMENT_START_TIME, null));
        events.add(new Event(Event.EventType.MODALITY_TYPE_CHANGE, MEASUREMENT_START_TIME,
                Modality.WALKING.getDatabaseIdentifier()));
        events.add(new Event(Event.EventType.LIFECYCLE_STOP, MEASUREMENT_START_TIME + (n+1) * 10L, null));

        final var serializable = new DataSerializable(events, testLocations(measurementIdentifier),
                testPoint3Ds(n), testPoint3Ds(n), testPoint3Ds(n));
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
        events.add(new Event(Event.EventType.LIFECYCLE_START, MEASUREMENT_START_TIME, null));
        // Modality selected during start
        events.add(new Event(Event.EventType.MODALITY_TYPE_CHANGE, MEASUREMENT_START_TIME,
                Modality.WALKING.getDatabaseIdentifier()));
        events.add(new Event(Event.EventType.LIFECYCLE_PAUSE, TRACK1_PAUSE_TIME, null));

        // Changes modality during first pause
        events.add(
                new Event(Event.EventType.MODALITY_TYPE_CHANGE, TRACK2_RESUME_TIME - 10,
                        Modality.BUS.getDatabaseIdentifier()));

        // Track 2
        events.add(new Event(Event.EventType.LIFECYCLE_RESUME, TRACK2_RESUME_TIME, null));
        events.add(new Event(Event.EventType.LIFECYCLE_PAUSE, TRACK2_PAUSE_TIME, null));

        // Track 3
        events.add(new Event(Event.EventType.LIFECYCLE_RESUME, TRACK3_RESUME_TIME, null));
        // Changes modality during track 3
        events.add(
                new Event(Event.EventType.MODALITY_TYPE_CHANGE, TRACK3_RESUME_TIME + 10,
                        Modality.BICYCLE.getDatabaseIdentifier()));
        events.add(new Event(Event.EventType.LIFECYCLE_STOP, MEASUREMENT_STOP_TIME, null));
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

    /*s
     * Fixture of 11 accelerations with pause/resume events.
     *
     * @return An ordered list of some test accelerations
     */
    List<List<? extends Point3D>> testPoint3Ds() {
        final var batches = new ArrayList<List<? extends Point3D>>();
        final var points = new ArrayList<Point3D>(11);
        points.add(point3D(0, MEASUREMENT_START_TIME));
        points.add(point3D(1,  MEASUREMENT_START_TIME + 25));
        points.add(point3D(2, TRACK1_PAUSE_TIME));
        points.add(point3D(3, TRACK1_PAUSE_TIME + 25));
        points.add(point3D(4, TRACK2_RESUME_TIME));
        points.add(point3D(5, TRACK2_RESUME_TIME + 25));
        points.add(point3D(6, TRACK2_PAUSE_TIME));
        points.add(point3D(7, TRACK2_PAUSE_TIME + 25));
        points.add(point3D(8, TRACK3_RESUME_TIME));
        points.add(point3D(9, TRACK3_RESUME_TIME + 25));
        points.add(point3D(10, MEASUREMENT_STOP_TIME));
        points.add(point3D(11, MEASUREMENT_STOP_TIME + 25));
        batches.add(points);
        return batches;
    }

    /**
     * Fixture of accelerations.
     *
     * @param n the number of points to generate
     * @return An ordered list of some test accelerations
     */
    List<List<? extends Point3D>> testPoint3Ds(final int n) {
        final var batches = new ArrayList<List<? extends Point3D>>();
        final var points = new ArrayList<Point3D>(n);
        for (int i = 0; i < n; i++) {
            points.add(point3D(i, MEASUREMENT_START_TIME + n * 10L));
        }
        batches.add(points);
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
