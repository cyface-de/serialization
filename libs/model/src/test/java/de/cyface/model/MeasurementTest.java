/*
 * Copyright 2020-2024 Cyface GmbH
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

import static de.cyface.model.Modality.BICYCLE;
import static de.cyface.model.Modality.UNKNOWN;
import static de.cyface.model.Modality.WALKING;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.commons.lang3.Validate;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests for the functionality provided directly by the {@link Measurement} class.
 *
 * @author Klemens Muthmann
 * @author Armin Schnabel
 * @version 1.0.4
 */
public class MeasurementTest {

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
     * The name of the user to add test data for.
     */
    private static final String TEST_USER_USERNAME = "guest";
    /**
     * The id of the user to add test data for.
     */
    private static final UUID TEST_USER_ID = UUID.randomUUID();

    /**
     * Tests that writing the CSV header produces the correct output.
     */
    @Test
    void testWriteCsvHeaderRow() {
        // Arrange
        final var expectedHeader = "userId,username,deviceId,measurementId,trackId,timestamp [ms],latitude,longitude,"
                + "speed [m/s],accuracy [m],modalityType,modalityTypeDistance [m],distance [m],modalityTypeTravelTime"
                + " [ms],travelTime [ms]\r\n";

        // Act
        final var csvOutput = new StringBuilder();
        final var options = new ExportOptions()
                .format(DataFormat.CSV)
                .type(DataType.LOCATION)
                .includeHeader(true)
                .includeUserId(true)
                .includeUsername(true);
        Measurement.csvHeader(options, csvOutput::append);

        // Assert
        assertThat(csvOutput.toString(), is(equalTo(expectedHeader)));
    }

    /**
     * Tests that a {@link Measurement} without any modality also works. The initial Modality was in this test case
     * deleted by the user
     */
    @Test
    void testWriteLocationAsCsvRows_withoutModalityChanges() {
        // Arrange
        final var point3DS = new ArrayList<Point3DImpl>();
        point3DS.add(new Point3DImpl(1.0f, -2.0f, 3.0f, 1_000L));
        final var metaData = metaData();
        final var identifier = metaData.getIdentifier();
        final var tracks = Arrays.asList(
                new Track(
                        Collections.singletonList(new RawRecord(identifier, 1_000L, latitude(1), longitude(1), null,
                                accuracy(1), speed(1), UNKNOWN)),
                        point3DS, point3DS, point3DS),
                new Track(
                        Collections.singletonList(new RawRecord(identifier, 3_000L, latitude(3), longitude(3), null,
                                accuracy(3), speed(3), UNKNOWN)),
                        point3DS, point3DS, point3DS));
        final var measurement = new Measurement(metaData, tracks);
        final var expectedOutput = "userId,username,deviceId,measurementId,trackId,timestamp [ms],latitude,longitude,"
                + "speed [m/s],accuracy [m],modalityType,modalityTypeDistance [m],distance [m],"
                + "modalityTypeTravelTime [ms],travelTime [ms]\r\n"
                + TEST_USER_ID + "," + TEST_USER_USERNAME + "," + DEVICE_IDENTIFIER + "," + MEASUREMENT_IDENTIFIER
                + ",0,1000," + latitude(1) + ","
                + longitude(1) + "," + speed(1) + "," + accuracy(1) + "," + UNKNOWN.getDatabaseIdentifier()
                + ",0.0,0.0,0,0\r\n"
                + TEST_USER_ID + "," + TEST_USER_USERNAME + "," + DEVICE_IDENTIFIER + "," + MEASUREMENT_IDENTIFIER
                + ",1,3000," + latitude(3) + ","
                + longitude(3) + "," + speed(3) + "," + accuracy(3) + "," + UNKNOWN.getDatabaseIdentifier()
                + ",0.0,0.0,0,0\r\n";

        // Act
        final var csvOutput = new StringBuilder();
        final var options = new ExportOptions()
                .format(DataFormat.CSV)
                .type(DataType.LOCATION)
                .includeHeader(true)
                .includeUserId(true)
                .includeUsername(true);
        measurement.asCsv(options, TEST_USER_USERNAME, csvOutput::append);

        // Assert
        assertThat(csvOutput.toString(), is(equalTo(expectedOutput)));
    }

    /**
     * Tests that modality type changes are correctly handled.
     */
    @Test
    void testWriteLocationAsCsvRows_withModalityTypeChanges() {
        // Arrange
        final var point3DS = new ArrayList<Point3DImpl>();
        point3DS.add(new Point3DImpl(1.0f, -2.0f, 3.0f, 1_000L));
        final var metaData = metaData();
        final var identifier = metaData.getIdentifier();
        final var tracks = Arrays.asList(
                new Track(
                        Arrays.asList(
                                new RawRecord(identifier, 1_000L, latitude(1), longitude(1), null, accuracy(1),
                                        speed(1), WALKING),
                                new RawRecord(identifier, 1_500L, latitude(2), longitude(2), null, accuracy(2),
                                        speed(2), WALKING)),
                        point3DS, point3DS, point3DS),
                new Track(
                        Arrays.asList(
                                new RawRecord(identifier, 3_000L, latitude(3), longitude(3), null, accuracy(3),
                                        speed(3), BICYCLE),
                                new RawRecord(identifier, 4_000L, latitude(4), longitude(4), null, accuracy(4),
                                        speed(4), BICYCLE)),
                        point3DS, point3DS, point3DS));
        final var measurement = new Measurement(metaData, tracks);

        final var expectedOutput = "userId,username,deviceId,measurementId,trackId,timestamp [ms],latitude,longitude,"
                + "speed [m/s],accuracy [m],modalityType,modalityTypeDistance [m],distance [m],"
                + "modalityTypeTravelTime [ms],travelTime [ms]\r\n"
                + TEST_USER_ID + "," + TEST_USER_USERNAME + "," + DEVICE_IDENTIFIER + "," + MEASUREMENT_IDENTIFIER
                + ",0,1000," + latitude(1) + ","
                + longitude(1) + "," + speed(1) + "," + accuracy(1) + "," + WALKING.getDatabaseIdentifier()
                + ",0.0,0.0,0,0\r\n" +
                TEST_USER_ID + "," + TEST_USER_USERNAME + "," + DEVICE_IDENTIFIER + "," + MEASUREMENT_IDENTIFIER
                + ",0,1500," + latitude(2) + ","
                + longitude(2) + "," + speed(2) + "," + accuracy(2) + "," + WALKING.getDatabaseIdentifier()
                + ",13.12610864737932,13.12610864737932,500,500\r\n" +
                TEST_USER_ID + "," + TEST_USER_USERNAME + "," + DEVICE_IDENTIFIER + "," + MEASUREMENT_IDENTIFIER
                + ",1,3000," + latitude(3) + ","
                + longitude(3) + "," + speed(3) + "," + accuracy(3) + "," + BICYCLE.getDatabaseIdentifier()
                + ",0.0,13.12610864737932,0,500\r\n" +
                TEST_USER_ID + "," + TEST_USER_USERNAME + "," + DEVICE_IDENTIFIER + "," + MEASUREMENT_IDENTIFIER
                + ",1,4000," + latitude(4) + ","
                + longitude(4) + "," + speed(4) + "," + accuracy(4) + "," + BICYCLE.getDatabaseIdentifier()
                + ",13.110048189675535,26.236156837054857,1000,1500\r\n";

        // Act
        final var csvOutput = new StringBuilder();
        final var options = new ExportOptions()
                .format(DataFormat.CSV)
                .type(DataType.LOCATION)
                .includeHeader(true)
                .includeUserId(true)
                .includeUsername(true);
        measurement.asCsv(options, TEST_USER_USERNAME, csvOutput::append);

        // Assert
        assertThat(csvOutput.toString(), is(equalTo(expectedOutput)));
    }

    @Test
    void testWriteMeasurementAsGeoJson() {
        // Arrange
        final var point3DS = new ArrayList<Point3DImpl>();
        point3DS.add(new Point3DImpl(1.0f, -2.0f, 3.0f, 1_000L));
        final var metaData = metaData();
        final var identifier = metaData.getIdentifier();
        final var tracks = Arrays.asList(
                new Track(
                        Arrays.asList(
                                new RawRecord(identifier, 1_000L, latitude(1), longitude(1), null, accuracy(1),
                                        speed(1), UNKNOWN),
                                new RawRecord(identifier, 2_000L, latitude(2), longitude(2), null, accuracy(2),
                                        speed(2), UNKNOWN)),
                        point3DS, point3DS, point3DS),
                new Track(
                        Collections.singletonList(new RawRecord(identifier, 3_000L, latitude(3), longitude(3), null,
                                accuracy(3), speed(3), UNKNOWN)),
                        point3DS, point3DS, point3DS));
        final var measurement = new Measurement(metaData, tracks);
        final var expectedOutput = "{\"type\":\"Feature\",\"geometry\":{\"type\":\"MultiLineString\",\"coordinates\":"
                + "[[[13.1,51.1],[13.2,51.2]],[[13.3,51.3]]]},\"properties\":{\"deviceId\":\""
                + identifier.getDeviceIdentifier() + "\"," + "\"measurementId\":"
                + identifier.getMeasurementIdentifier() + ",\"length\":0.0}}";

        // Act
        final var jsonOutput = new StringBuilder();
        measurement.asGeoJson(jsonOutput::append);

        // Assert
        assertThat(jsonOutput.toString(), is(equalTo(expectedOutput)));
    }

    @Test
    void testWriteMeasurementAsJson() {
        // Arrange
        final var point3DS = new ArrayList<Point3DImpl>();
        point3DS.add(new Point3DImpl(1.0f, -2.0f, 3.0f, 1_000L));
        final var metaData = metaData();
        final var identifier = metaData.getIdentifier();
        final var tracks = Collections.singletonList(
                new Track(Collections.singletonList(
                        new RawRecord(identifier, 1_000L, latitude(1), longitude(1), null, accuracy(1),
                                speed(1), UNKNOWN)),
                        point3DS, point3DS, point3DS));
        final var measurement = new Measurement(metaData, tracks);
        final var expectedOutput = "{\"metaData\":{\"userId\":\"" + TEST_USER_ID
                + "\",\"username\":\"guest\",\"deviceId\":\""
                + identifier.getDeviceIdentifier() + "\",\"measurementId\":" + identifier.getMeasurementIdentifier()
                + ",\"length\":0.0},\"tracks\":[{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\","
                + "\"geometry\":{\"type\":\"Point\",\"coordinates\":[13.1,51.1]},\"properties\":{\"timestamp\":1000,"
                + "\"speed\":0.1,\"accuracy\":10.0,\"modality\":\"UNKNOWN\"}}]}]}";

        // Act
        final var jsonOutput = new StringBuilder();
        measurement.asJson(TEST_USER_USERNAME, jsonOutput::append);

        // Assert
        assertThat(jsonOutput.toString(), is(equalTo(expectedOutput)));
    }

    /**
     * Ensures track buckets are sorted before they are composed to a Track.
     */
    @Test
    void testTracks_toSortBuckets() throws ParseException {

        // Arrange
        final var buckets = generateTrackBuckets(0, 2, Modality.BICYCLE);

        // Un-sort track buckets
        buckets.sort(Comparator.comparing(TrackBucket::getBucket).reversed());

        // Act
        final var oocut = new Measurement(buckets);

        // Assert
        final var expectedTrack = generateMeasurement(1, new Modality[] {Modality.BICYCLE}).getTracks().get(0);
        assertThat(oocut.getTracks().size(), CoreMatchers.is(CoreMatchers.equalTo(1)));
        assertThat(oocut.getTracks().get(0), CoreMatchers.is(CoreMatchers.equalTo(expectedTrack)));
    }

    /**
     * Ensures tracks are sorted before they are composed to a Track list.
     */
    @Test
    void testTracks_toSortTracks() throws ParseException {
        // Arrange
        final var buckets = generateTrackBuckets(1, 1, Modality.BICYCLE);
        buckets.addAll(generateTrackBuckets(0, 1, Modality.WALKING));

        // Act
        final var oocut = new Measurement(buckets);

        // Assert
        final var expectedTracks = generateMeasurement(2, new Modality[] {Modality.WALKING, Modality.BICYCLE})
                .getTracks();
        assertThat(oocut.getTracks(), CoreMatchers.is(CoreMatchers.equalTo(expectedTracks)));
    }

    /**
     * Ensures data in the current database format ("track buckets") can be converted to {@code Measurement}s.
     */
    @ParameterizedTest
    @MethodSource("provideTrackBucketsForMeasurements")
    void testMeasurement(final List<TrackBucket> buckets, final Measurement expectedMeasurement) {
        // Act
        final var oocut = new Measurement(buckets);

        // Assert
        assertThat(oocut, CoreMatchers.is(CoreMatchers.equalTo(expectedMeasurement)));
    }

    private static Stream<Arguments> provideTrackBucketsForMeasurements() throws ParseException {

        // Small test case
        final var singleMeasurementBuckets = generateTrackBuckets(0, 1, Modality.BICYCLE);
        final var singleMeasurement = generateMeasurement(1, new Modality[] {Modality.BICYCLE});

        // Multiple tracks in one measurement
        final var multipleTracksBuckets = generateTrackBuckets(0, 1, Modality.BICYCLE);
        multipleTracksBuckets.addAll(generateTrackBuckets(1, 1, Modality.BICYCLE));
        final var multipleTracksMeasurement = generateMeasurement(2, new Modality[] {Modality.BICYCLE});

        // Multiple buckets in one track
        final var multipleBucketsBuckets = generateTrackBuckets(0, 2, Modality.BICYCLE);
        final var multipleBucketsMeasurement = generateMeasurement(1, new Modality[] {Modality.BICYCLE});

        return Stream.of(
                Arguments.of(singleMeasurementBuckets, singleMeasurement),
                Arguments.of(multipleTracksBuckets, multipleTracksMeasurement),
                Arguments.of(multipleBucketsBuckets, multipleBucketsMeasurement));
    }

    private static List<TrackBucket> generateTrackBuckets(final int trackId,
            final int numberOfTrackBuckets, final Modality modality) throws ParseException {

        Validate.isTrue(numberOfTrackBuckets <= 3, "Not implemented for larger data sets");

        final var metaData = metaData();
        final var identifier = metaData.getIdentifier();

        final var locations = new ArrayList<RawRecord>();
        locations.add(new RawRecord(identifier, 1608650009000L, 51.075295000000004, 13.772176666666667, null, 27.04,
                13.039999961853027, modality));
        locations.add(new RawRecord(identifier, 1608650010000L, 51.0753, 13.77215, null, 16.85,
                13.039999961853027, modality));
        locations.add(new RawRecord(identifier, 1608650010000L, 51.0753, 13.77215, null, 27.25,
                13.039999961853027, modality));

        final var trackBuckets = new ArrayList<TrackBucket>();
        for (int i = 0; i < numberOfTrackBuckets; i++) {
            final var isLastBucket = i == numberOfTrackBuckets - 1;
            final var minute = 13 + i;
            final var locationsSlice = new ArrayList<RawRecord>();
            if (isLastBucket) {
                locationsSlice.addAll(locations);
            } else {
                locationsSlice.add(locations.get(0));
                locations.remove(0);
            }
            // noinspection SpellCheckingInspection
            final var date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse("2020-12-22T15:" + minute + ":00Z");
            final var track = new Track(locationsSlice, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
            trackBuckets.add(new TrackBucket(trackId, date, track, metaData));
        }

        return trackBuckets;
    }

    private static Measurement generateMeasurement(final int numberOfTracks, final Modality[] modalities) {
        Validate.isTrue(modalities.length <= 2, "Not implemented");

        final var expectedMetaData = metaData();
        final var measurementIdentifier = metaData().getIdentifier();
        final var expectedTracks = new ArrayList<Track>();
        for (int i = 0; i < numberOfTracks; i++) {
            final var modality = modalities.length == 1 ? modalities[0] : i == 0 ? modalities[0] : modalities[1];
            final var expectedLocations = new ArrayList<RawRecord>();
            expectedLocations.add(new RawRecord(measurementIdentifier, 1608650009000L,
                    51.075295000000004, 13.772176666666667, 27.04, 13.039999961853027, modality));
            expectedLocations.add(new RawRecord(measurementIdentifier, 1608650010000L,
                    51.0753, 13.77215, 16.85, 13.039999961853027, modality));
            expectedLocations.add(new RawRecord(measurementIdentifier, 1608650010000L,
                    51.0753, 13.77215, 27.25, 13.039999961853027, modality));
            expectedTracks.add(new Track(expectedLocations, new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
        }
        return new Measurement(expectedMetaData, expectedTracks);
    }

    /**
     * @param index The 1-based index of the latitude to generate
     * @return A valid latitude value (although it might semantically make no sense)
     */
    private double latitude(final int index) {
        return 51.0 + index / 10.0;
    }

    /**
     * @param index The 1-based index of the longitude to generate
     * @return A valid longitude value (although it might semantically make no sense)
     */
    private double longitude(final int index) {
        return 13.0 + index / 10.0;
    }

    /**
     * @param index The 1-based index of the speed to generate
     * @return A valid speed value (although it might semantically make no sense)
     */
    private double speed(final int index) {
        return 0.0 + index * 0.1;
    }

    /**
     * @param index The 1-based index of the accuracy to generate
     * @return A valid accuracy value (although it might semantically make no sense)
     */
    private double accuracy(final int index) {
        return 0.0 + index * 10.0;
    }

    /**
     * @return A static set of metadata to be used by test {@link Measurement} instances
     */
    private static MetaData metaData() {
        return new MetaData(new MeasurementIdentifier(DEVICE_IDENTIFIER, MEASUREMENT_IDENTIFIER),
                "Android SDK built for x86", "Android 8.0.0",
                "2.7.0-beta1", 0.0, TEST_USER_ID, MetaData.CURRENT_VERSION);
    }
}
