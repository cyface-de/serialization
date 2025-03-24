/*
 * Copyright 2020-2025 Cyface GmbH
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
package de.cyface.model

import de.cyface.model.Measurement.Companion.csvHeader
import org.apache.commons.lang3.Validate
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID
import java.util.stream.Stream

/**
 * Tests for the functionality provided directly by the [Measurement] class.
 *
 * @author Klemens Muthmann
 * @author Armin Schnabel
 * @version 1.0.4
 */
class MeasurementTest {
    /**
     * Tests that writing the CSV header produces the correct output.
     */
    @Test
    fun testWriteCsvHeaderRow() {
        // Arrange
        val expectedHeader = ("userId,username,deviceId,measurementId,trackId,timestamp [ms],latitude,longitude,"
                + "speed [m/s],accuracy [m],modalityType,modalityTypeDistance [m],distance [m],modalityTypeTravelTime"
                + " [ms],travelTime [ms]\r\n")

        // Act
        val csvOutput = StringBuilder()
        val options = ExportOptions()
            .format(DataFormat.CSV)
            .type(DataType.LOCATION)
            .includeHeader(true)
            .includeUserId(true)
            .includeUsername(true)
        csvHeader(
            options
        ) { str: String? -> csvOutput.append(str) }

        // Assert
        MatcherAssert.assertThat(csvOutput.toString(), Matchers.`is`(Matchers.equalTo(expectedHeader)))
    }

    /**
     * Tests that a [Measurement] without any modality also works. The initial Modality was in this test case
     * deleted by the user
     */
    @Test
    fun testWriteLocationAsCsvRows_withoutModalityChanges() {
        // Arrange
        val point3DS = ArrayList<Point3DImpl>()
        point3DS.add(Point3DImpl(1.0f, -2.0f, 3.0f, 1000L))
        val metaData = metaData()
        val identifier = metaData.identifier
        val tracks = listOf(
            Track(
                mutableListOf(
                    RawRecord(
                        identifier, 1000L, latitude(1), longitude(1), null,
                        accuracy(1), speed(1), Modality.UNKNOWN
                    )
                ),
                point3DS, point3DS, point3DS
            ),
            Track(
                mutableListOf(
                    RawRecord(
                        identifier, 3000L, latitude(3), longitude(3), null,
                        accuracy(3), speed(3), Modality.UNKNOWN
                    )
                ),
                point3DS, point3DS, point3DS
            )
        )
        val measurement = Measurement(metaData, tracks.toMutableList())
        val expectedOutput = ("""
     userId,username,deviceId,measurementId,trackId,timestamp [ms],latitude,longitude,speed [m/s],accuracy [m],modalityType,modalityTypeDistance [m],distance [m],modalityTypeTravelTime [ms],travelTime [ms]
     $TEST_USER_ID,$TEST_USER_USERNAME,$DEVICE_IDENTIFIER,$MEASUREMENT_IDENTIFIER,0,1000,${
            latitude(
                1
            )
        },${longitude(1)},${speed(1)},${accuracy(1)},${Modality.UNKNOWN.databaseIdentifier},0.0,0.0,0,0
     $TEST_USER_ID,$TEST_USER_USERNAME,$DEVICE_IDENTIFIER,$MEASUREMENT_IDENTIFIER,1,3000,${
            latitude(
                3
            )
        },${longitude(3)},${speed(3)},${accuracy(3)},${Modality.UNKNOWN.databaseIdentifier},0.0,0.0,0,0
     
     """.trimIndent())

        // Act
        val csvOutput = StringBuilder()
        val options = ExportOptions()
            .format(DataFormat.CSV)
            .type(DataType.LOCATION)
            .includeHeader(true)
            .includeUserId(true)
            .includeUsername(true)
        measurement.asCsv(options, TEST_USER_USERNAME) { str: String? -> csvOutput.append(str) }

        // Assert
        MatcherAssert.assertThat(
            csvOutput.toString().replace("\r\n", "\n"), Matchers.`is`(Matchers.equalTo(expectedOutput))
        )
    }

    /**
     * Tests that modality type changes are correctly handled.
     */
    @Test
    fun testWriteLocationAsCsvRows_withModalityTypeChanges() {
        // Arrange
        val point3DS = ArrayList<Point3DImpl>()
        point3DS.add(Point3DImpl(1.0f, -2.0f, 3.0f, 1000L))
        val metaData = metaData()
        val identifier = metaData.identifier
        val tracks = listOf(
            Track(
                mutableListOf(
                    RawRecord(
                        identifier, 1000L, latitude(1), longitude(1), null, accuracy(1),
                        speed(1), Modality.WALKING
                    ),
                    RawRecord(
                        identifier, 1500L, latitude(2), longitude(2), null, accuracy(2),
                        speed(2), Modality.WALKING
                    )
                ),
                point3DS, point3DS, point3DS
            ),
            Track(
                mutableListOf(
                    RawRecord(
                        identifier, 3000L, latitude(3), longitude(3), null, accuracy(3),
                        speed(3), Modality.BICYCLE
                    ),
                    RawRecord(
                        identifier, 4000L, latitude(4), longitude(4), null, accuracy(4),
                        speed(4), Modality.BICYCLE
                    )
                ),
                point3DS, point3DS, point3DS
            )
        )
        val measurement = Measurement(metaData, tracks.toMutableList())

        val expectedOutput = ("""
     userId,username,deviceId,measurementId,trackId,timestamp [ms],latitude,longitude,speed [m/s],accuracy [m],modalityType,modalityTypeDistance [m],distance [m],modalityTypeTravelTime [ms],travelTime [ms]
     $TEST_USER_ID,$TEST_USER_USERNAME,$DEVICE_IDENTIFIER,$MEASUREMENT_IDENTIFIER,0,1000,${
            latitude(
                1
            )
        },${longitude(1)},${speed(1)},${accuracy(1)},${Modality.WALKING.databaseIdentifier},0.0,0.0,0,0
     $TEST_USER_ID,$TEST_USER_USERNAME,$DEVICE_IDENTIFIER,$MEASUREMENT_IDENTIFIER,0,1500,${
            latitude(
                2
            )
        },${longitude(2)},${speed(2)},${accuracy(2)},${Modality.WALKING.databaseIdentifier},13.12610864737932,13.12610864737932,500,500
     $TEST_USER_ID,$TEST_USER_USERNAME,$DEVICE_IDENTIFIER,$MEASUREMENT_IDENTIFIER,1,3000,${
            latitude(
                3
            )
        },${longitude(3)},${speed(3)},${accuracy(3)},${Modality.BICYCLE.databaseIdentifier},0.0,13.12610864737932,0,500
     $TEST_USER_ID,$TEST_USER_USERNAME,$DEVICE_IDENTIFIER,$MEASUREMENT_IDENTIFIER,1,4000,${
            latitude(
                4
            )
        },${longitude(4)},${speed(4)},${accuracy(4)},${Modality.BICYCLE.databaseIdentifier},13.110048189675535,26.236156837054857,1000,1500
     
     """.trimIndent())

        // Act
        val csvOutput = StringBuilder()
        val options = ExportOptions()
            .format(DataFormat.CSV)
            .type(DataType.LOCATION)
            .includeHeader(true)
            .includeUserId(true)
            .includeUsername(true)
        measurement.asCsv(options, TEST_USER_USERNAME) { str: String? -> csvOutput.append(str) }

        // Assert
        MatcherAssert.assertThat(
            csvOutput.toString().replace("\r\n", "\n"), Matchers.`is`(Matchers.equalTo(expectedOutput))
        )
    }

    @Test
    fun testWriteMeasurementAsGeoJson() {
        // Arrange
        val point3DS = ArrayList<Point3DImpl>()
        point3DS.add(Point3DImpl(1.0f, -2.0f, 3.0f, 1000L))
        val metaData = metaData()
        val identifier = metaData.identifier
        val tracks = listOf(
            Track(
                mutableListOf(
                    RawRecord(
                        identifier, 1000L, latitude(1), longitude(1), null, accuracy(1),
                        speed(1), Modality.UNKNOWN
                    ),
                    RawRecord(
                        identifier, 2000L, latitude(2), longitude(2), null, accuracy(2),
                        speed(2), Modality.UNKNOWN
                    )
                ),
                point3DS, point3DS, point3DS
            ),
            Track(
                mutableListOf(
                    RawRecord(
                        identifier, 3000L, latitude(3), longitude(3), null,
                        accuracy(3), speed(3), Modality.UNKNOWN
                    )
                ),
                point3DS, point3DS, point3DS
            )
        )
        val measurement = Measurement(metaData, tracks.toMutableList())
        val expectedOutput = ("{\"type\":\"Feature\",\"geometry\":{\"type\":\"MultiLineString\",\"coordinates\":"
                + "[[[13.1,51.1],[13.2,51.2]],[[13.3,51.3]]]},\"properties\":{\"deviceId\":\""
                + identifier!!.deviceIdentifier + "\"," + "\"measurementId\":"
                + identifier.measurementIdentifier + ",\"length\":0.0}}")

        // Act
        val jsonOutput = StringBuilder()
        measurement.asGeoJson { str: String? -> jsonOutput.append(str) }

        // Assert
        MatcherAssert.assertThat(jsonOutput.toString(), Matchers.`is`(Matchers.equalTo(expectedOutput)))
    }

    @Test
    fun testWriteMeasurementAsJson() {
        // Arrange
        val point3DS = ArrayList<Point3DImpl>()
        point3DS.add(Point3DImpl(1.0f, -2.0f, 3.0f, 1000L))
        val metaData = metaData()
        val identifier = metaData.identifier
        val tracks = listOf(
            Track(
                mutableListOf(
                    RawRecord(
                        identifier, 1000L, latitude(1), longitude(1), null, accuracy(1),
                        speed(1), Modality.UNKNOWN
                    )
                ),
                point3DS, point3DS, point3DS
            )
        )
        val measurement = Measurement(metaData, tracks.toMutableList())
        val expectedOutput = ("{\"metaData\":{\"userId\":\"" + TEST_USER_ID
                + "\",\"username\":\"guest\",\"deviceId\":\""
                + identifier!!.deviceIdentifier + "\",\"measurementId\":" + identifier.measurementIdentifier
                + ",\"length\":0.0},\"tracks\":[{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\","
                + "\"geometry\":{\"type\":\"Point\",\"coordinates\":[13.1,51.1]},\"properties\":{\"timestamp\":1000,"
                + "\"speed\":0.1,\"accuracy\":10.0,\"modality\":\"UNKNOWN\"}}]}]}")

        // Act
        val jsonOutput = StringBuilder()
        measurement.asJson(
            TEST_USER_USERNAME
        ) { str: String? -> jsonOutput.append(str) }

        // Assert
        MatcherAssert.assertThat(jsonOutput.toString(), Matchers.`is`(Matchers.equalTo(expectedOutput)))
    }

    /**
     * Ensures track buckets are sorted before they are composed to a Track.
     */
    @Test
    @Throws(ParseException::class)
    fun testTracks_toSortBuckets() {
        // Arrange

        val buckets: List<TrackBucket> = generateTrackBuckets(0, 2, Modality.BICYCLE)

        // Un-sort track buckets
        buckets.sortedBy { it.bucket }.reversed()

        // Act
        val oocut = Measurement(buckets)

        // Assert
        val expectedTrack = generateMeasurement(1, arrayOf(Modality.BICYCLE)).tracks[0]
        MatcherAssert.assertThat(oocut.tracks.size, CoreMatchers.`is`(CoreMatchers.equalTo(1)))
        MatcherAssert.assertThat(oocut.tracks[0], CoreMatchers.`is`(CoreMatchers.equalTo(expectedTrack)))
    }

    /**
     * Ensures tracks are sorted before they are composed to a Track list.
     */
    @Test
    @Throws(ParseException::class)
    fun testTracks_toSortTracks() {
        // Arrange
        val buckets = generateTrackBuckets(1, 1, Modality.BICYCLE)
        buckets.addAll(generateTrackBuckets(0, 1, Modality.WALKING))

        // Act
        val oocut = Measurement(buckets)

        // Assert
        val expectedTracks: List<Track> = generateMeasurement(2, arrayOf(Modality.WALKING, Modality.BICYCLE))
            .tracks
        MatcherAssert.assertThat<List<Track>>(oocut.tracks, CoreMatchers.`is`(CoreMatchers.equalTo(expectedTracks)))
    }

    /**
     * Ensures data in the current database format ("track buckets") can be converted to `Measurement`s.
     */
    @ParameterizedTest
    @MethodSource("provideTrackBucketsForMeasurements")
    fun testMeasurement(buckets: List<TrackBucket>, expectedMeasurement: Measurement) {
        // Act
        val oocut = Measurement(buckets)

        // Assert
        MatcherAssert.assertThat(oocut, CoreMatchers.`is`(CoreMatchers.equalTo(expectedMeasurement)))
    }

    /**
     * @param index The 1-based index of the latitude to generate
     * @return A valid latitude value (although it might semantically make no sense)
     */
    private fun latitude(index: Int): Double {
        return 51.0 + index / 10.0
    }

    /**
     * @param index The 1-based index of the longitude to generate
     * @return A valid longitude value (although it might semantically make no sense)
     */
    private fun longitude(index: Int): Double {
        return 13.0 + index / 10.0
    }

    /**
     * @param index The 1-based index of the speed to generate
     * @return A valid speed value (although it might semantically make no sense)
     */
    private fun speed(index: Int): Double {
        return 0.0 + index * 0.1
    }

    /**
     * @param index The 1-based index of the accuracy to generate
     * @return A valid accuracy value (although it might semantically make no sense)
     */
    private fun accuracy(index: Int): Double {
        return 0.0 + index * 10.0
    }

    companion object {
        /**
         * A globally unique identifier of the simulated upload device. The actual value does not really matter.
         */
        private val DEVICE_IDENTIFIER = UUID.randomUUID().toString()

        /**
         * The measurement identifier used for the test measurement. The actual value does not matter that much. It
         * simulates a device wide unique identifier.
         */
        private const val MEASUREMENT_IDENTIFIER = 1L

        /**
         * The name of the user to add test data for.
         */
        private const val TEST_USER_USERNAME = "guest"

        /**
         * The id of the user to add test data for.
         */
        private val TEST_USER_ID: UUID = UUID.randomUUID()

        /**
         * The date when the data was uploaded.
         */
        private val uploadDate = Date()

        @Throws(ParseException::class)
        @JvmStatic
        private fun provideTrackBucketsForMeasurements(): Stream<Arguments> {
            // Small test case

            val singleMeasurementBuckets: List<TrackBucket> = generateTrackBuckets(0, 1, Modality.BICYCLE)
            val singleMeasurement = generateMeasurement(1, arrayOf(Modality.BICYCLE))

            // Multiple tracks in one measurement
            val multipleTracksBuckets = generateTrackBuckets(0, 1, Modality.BICYCLE)
            multipleTracksBuckets.addAll(generateTrackBuckets(1, 1, Modality.BICYCLE))
            val multipleTracksMeasurement = generateMeasurement(2, arrayOf(Modality.BICYCLE))

            // Multiple buckets in one track
            val multipleBucketsBuckets: List<TrackBucket> = generateTrackBuckets(0, 2, Modality.BICYCLE)
            val multipleBucketsMeasurement = generateMeasurement(1, arrayOf(Modality.BICYCLE))

            return Stream.of(
                Arguments.of(singleMeasurementBuckets, singleMeasurement),
                Arguments.of(multipleTracksBuckets, multipleTracksMeasurement),
                Arguments.of(multipleBucketsBuckets, multipleBucketsMeasurement)
            )
        }

        @Throws(ParseException::class)
        private fun generateTrackBuckets(
            trackId: Int,
            numberOfTrackBuckets: Int, modality: Modality
        ): MutableList<TrackBucket> {
            Validate.isTrue(numberOfTrackBuckets <= 3, "Not implemented for larger data sets")

            val metaData = metaData()
            val identifier = metaData.identifier

            val locations = ArrayList<RawRecord>()
            locations.add(
                RawRecord(
                    identifier, 1608650009000L, 51.075295000000004, 13.772176666666667, null, 27.04,
                    13.039999961853027, modality
                )
            )
            locations.add(
                RawRecord(
                    identifier, 1608650010000L, 51.0753, 13.77215, null, 16.85,
                    13.039999961853027, modality
                )
            )
            locations.add(
                RawRecord(
                    identifier, 1608650010000L, 51.0753, 13.77215, null, 27.25,
                    13.039999961853027, modality
                )
            )

            val trackBuckets = ArrayList<TrackBucket>()
            for (i in 0 until numberOfTrackBuckets) {
                val isLastBucket = i == numberOfTrackBuckets - 1
                val minute = 13 + i
                val locationsSlice = ArrayList<RawRecord>()
                if (isLastBucket) {
                    locationsSlice.addAll(locations)
                } else {
                    locationsSlice.add(locations[0])
                    locations.removeAt(0)
                }
                // noinspection SpellCheckingInspection
                val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse("2020-12-22T15:$minute:00Z")
                val track = Track(locationsSlice, ArrayList(), ArrayList(), ArrayList())
                trackBuckets.add(TrackBucket(trackId, date, track, metaData))
            }

            return trackBuckets
        }

        private fun generateMeasurement(numberOfTracks: Int, modalities: Array<Modality>): Measurement {
            Validate.isTrue(modalities.size <= 2, "Not implemented")

            val expectedMetaData = metaData()
            val measurementIdentifier = metaData().identifier
            val expectedTracks = ArrayList<Track>()
            for (i in 0 until numberOfTracks) {
                val modality = if (modalities.size == 1) modalities[0] else if (i == 0) modalities[0] else modalities[1]
                val expectedLocations = ArrayList<RawRecord>()
                expectedLocations.add(
                    RawRecord(
                        measurementIdentifier, 1608650009000L,
                        51.075295000000004, 13.772176666666667, 27.04, 13.039999961853027, modality
                    )
                )
                expectedLocations.add(
                    RawRecord(
                        measurementIdentifier, 1608650010000L,
                        51.0753, 13.77215, 16.85, 13.039999961853027, modality
                    )
                )
                expectedLocations.add(
                    RawRecord(
                        measurementIdentifier, 1608650010000L,
                        51.0753, 13.77215, 27.25, 13.039999961853027, modality
                    )
                )
                expectedTracks.add(Track(expectedLocations, ArrayList(), ArrayList(), ArrayList()))
            }
            return Measurement(expectedMetaData, expectedTracks)
        }

        /**
         * @return A static set of metadata to be used by test [Measurement] instances
         */
        private fun metaData(): MetaData {
            return MetaData.Companion.create(
                MeasurementIdentifier(DEVICE_IDENTIFIER, MEASUREMENT_IDENTIFIER),
                "Android SDK built for x86",
                "Android 8.0.0",
                "2.7.0-beta1",
                0.0,
                TEST_USER_ID,
                MetaData.CURRENT_VERSION,
                uploadDate,
            )
        }
    }
}
