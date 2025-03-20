/*
 * Copyright 2019-2025 Cyface GmbH
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

import de.cyface.model.Json.JsonArray
import de.cyface.model.Json.JsonObject
import org.apache.commons.lang3.Validate
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.Objects
import java.util.function.Consumer
import kotlin.math.max
import kotlin.math.min

/**
 * A single measurement captured by a Cyface measurement device.
 *
 * Even though this object has setters for all fields and a no argument constructor, it should be handled as immutable.
 * The reason for the existence of those setters and the constructor is the requirement to use objects of this class as
 * part of Apache Flink Pipelines, which require public setters and a no argument constructor to transfer objects
 * between cluster nodes.
 *
 * @author Armin Schnabel
 * @author Klemens Muthmann
 * @version 3.0.0
 * @since 1.0.0
 */
class Measurement : Serializable {
    /**
     * The context of this `Measurement`.
     *
     * Setter required for Apache Flink.
     */
    @JvmField
    @set:Suppress("unused")
    var metaData: MetaData? = null

    /**
     * The data collected for this `Measurement` in `Track`-slices, ordered by timestamp.
     */
    var tracks: MutableList<Track> = mutableListOf()
        get() = Collections.unmodifiableList(field)

    /**
     * Creates a new uninitialized `Measurement`. This is only necessary for Flink serialisation and should never
     * be called from your own code.
     */
    constructor() {
        // Nothing to do here.
    }

    /**
     * Creates a new completely initialized `Measurement`.
     *
     * @param metaData The context of this `Measurement`.
     * @param tracks The data collected for this `Measurement` in `Track`-slices, ordered by timestamp.
     */
    constructor(metaData: MetaData, tracks: MutableList<Track>) {
        this.metaData = metaData
        this.tracks = tracks.toMutableList()
    }

    constructor(buckets: List<TrackBucket>) {
        require(buckets.isNotEmpty()) { "Cannot create a measurement from 0 buckets!" }
        this.metaData = buckets[0].getMetaData().also { requireNotNull(metaData) }
        this.tracks = tracks(buckets).toMutableList()
    }

    /**
     * Merges [TrackBucket]s into [Track]s.
     *
     * @param trackBuckets the data to merge
     * @return the tracks
     */
    private fun tracks(trackBuckets: List<TrackBucket>): List<Track> {
        // Group by trackId
        val groupedBuckets = trackBuckets.groupBy { it.getTrackId() }

        // Sort bucket groups by trackId
        val sortedBucketGroups = groupedBuckets.toSortedMap()

        // Convert buckets to Track
        val tracks = mutableListOf<Track>()
        sortedBucketGroups.forEach { (_, bucketGroup) ->
            // Sort buckets
            val sortedBuckets = bucketGroup.sortedBy { it.bucket }
            // Merge buckets
            val locations = sortedBuckets.flatMap { it.track.locationRecords }.toMutableList()
            val accelerations = sortedBuckets.flatMap { it.track.accelerations }.toMutableList()
            val rotations = sortedBuckets.flatMap { it.track.rotations }.toMutableList()
            val directions = sortedBuckets.flatMap { it.track.directions }.toMutableList()
            tracks.add(Track(locations, accelerations, rotations, directions))
        }
        return tracks
    }

    /**
     * Exports this measurement as a CSV file.
     *
     * @param options The options which describe which data should be exported.
     * @param handler A handler that gets one line of CSV output per call
     */
    @Suppress("unused") // API used by backend/executables/cyface-to-csv
    fun asCsv(options: ExportOptions, handler: (String) -> Unit) =
        asCsv(options, null, handler)

    /**
     * Exports this measurement as a CSV file.
     *
     * @param options The options which describe which data should be exported.
     * @param username The name of the user who uploaded the data or `null` to not export this field
     * @param handler A handler that gets one line of CSV output per call
     */
    fun asCsv(options: ExportOptions, username: String?, handler: (String) -> Unit) {
        require(!options.includeUsername || username != null)

        if (options.includeHeader) {
            csvHeader(options, handler)
        }

        when (options.type) {
            DataType.LOCATION -> locationDataAsCsv(options, username, handler)
            DataType.ACCELERATION, DataType.ROTATION, DataType.DIRECTION ->
                sensorDataAsCsv(options, username, handler)
            else -> throw IllegalArgumentException("Unsupported type: ${options.type}")
        }
    }

    private fun locationDataAsCsv(options: ExportOptions, username: String?, handler: Consumer<String>) {
        var lastModality = Modality.UNKNOWN

        // Iterate through tracks
        var modalityTypeDistance = 0.0
        var totalDistance = 0.0
        var modalityTypeTravelTime = 0L
        var totalTravelTime = 0L

        tracks.forEachIndexed { trackId, track ->
            // Iterate through locations
            var lastLocation: RawRecord? = null
            track.locationRecords.forEach { locationRecord ->
                lastLocation?.let {
                    val newDistance = it.distanceTo(locationRecord) / 1000.0
                    modalityTypeDistance += newDistance
                    totalDistance += newDistance
                    val timeTraveled = locationRecord.timestamp - it.timestamp
                    modalityTypeTravelTime += timeTraveled
                    totalTravelTime += timeTraveled
                }

                // Check if the modalityType changed
                if (locationRecord.modality != null && locationRecord.modality != lastModality) {
                    lastModality = locationRecord.modality
                    modalityTypeDistance = 0.0
                    modalityTypeTravelTime = 0L
                }

                handler.accept(
                    csvRow(
                        options, username, metaData!!, locationRecord, trackId,
                        modalityTypeDistance, totalDistance,
                        modalityTypeTravelTime, totalTravelTime
                    )
                )
                handler.accept("\r\n")

                lastLocation = locationRecord
            }
        }
    }

    private fun sensorDataAsCsv(options: ExportOptions, username: String?, handler: Consumer<String>) {
        // Iterate through tracks
        tracks.forEachIndexed { trackId, track ->
            // Iterate through sensor points
            val points = when (options.type) {
                DataType.ACCELERATION -> track.accelerations
                DataType.ROTATION -> track.rotations
                DataType.DIRECTION -> track.directions
                else -> throw IllegalArgumentException("Unsupported type: ${options.type}")
            }
            points.forEach { point ->
                handler.accept(csvSensorRow(options, username, metaData!!, point, trackId))
                handler.accept("\r\n")
            }
        }
    }

    /**
     * Exports this measurement as GeoJSON feature.
     *
     * @param handler A handler that gets the GeoJson feature as string
     */
    fun asGeoJson(handler: Consumer<String>) {
        // We decided to generate a String instead of using a JSON library to avoid dependencies in the model library

        // measurement = geoJson "feature"

        handler.accept("{")
        handler.accept(Json.jsonKeyValue("type", "Feature").stringValue)
        handler.accept(",")

        // All tracks = geometry (MultiLineString)
        handler.accept("\"geometry\":{")
        handler.accept(Json.jsonKeyValue("type", "MultiLineString").stringValue)
        handler.accept(",")
        handler.accept("\"coordinates\":")
        val tracksCoordinates = convertToLineStringCoordinates(tracks)
        handler.accept(tracksCoordinates)
        handler.accept("},")

        val deviceId = Json.jsonKeyValue("deviceId", metaData!!.identifier!!.deviceIdentifier)
        val measurementId = Json.jsonKeyValue(
            "measurementId",
            metaData!!.identifier!!.measurementIdentifier
        )
        val length = Json.jsonKeyValue("length", metaData!!.length)
        val properties = Json.jsonObject(deviceId, measurementId, length)
        handler.accept(Json.jsonKeyValue("properties", properties).stringValue)

        handler.accept("}")
    }

    /**
     * Exports this measurement as Json **without sensor data**.
     *
     * @param handler A handler that gets the Json as string
     */
    @Suppress("unused") // Part of the API
    fun asJson(handler: Consumer<String>) {
        asJson(null, handler)
    }

    /**
     * Exports this measurement as Json **without sensor data**.
     *
     * @param username The name of the user who uploaded the data or `null` to omit this field
     * @param handler A handler that gets the Json as string
     */
    fun asJson(username: String?, handler: Consumer<String>) {
        // We decided to generate a String instead of using a JSON library to avoid dependencies in the model library
        handler.accept("{")

        handler.accept(Json.jsonKeyValue("metaData", asJson(username, metaData!!)).stringValue)
        handler.accept(",")

        handler.accept("\"tracks\":[")
        for (i in tracks.indices) {
            val track = tracks[i]
            handler.accept(featureCollection(track).stringValue)
            if (i != tracks.size - 1) {
                handler.accept(",")
            }
        }
        handler.accept("]")

        handler.accept("}")
    }

    private fun asJson(username: String?, metaData: MetaData): JsonObject {
        return Json.jsonObject(
            Json.jsonKeyValue("userId", metaData.userId.toString()),
            if (username != null) Json.jsonKeyValue("username", username) else null,
            Json.jsonKeyValue("deviceId", metaData.identifier!!.deviceIdentifier),
            Json.jsonKeyValue("measurementId", metaData.identifier!!.measurementIdentifier),
            Json.jsonKeyValue("length", metaData.length)
        )
    }

    /**
     * Converts a [Track] to a `GeoJson` "FeatureCollection" with "Point" "Features".
     *
     * @param track the `Track` to convert
     * @return the converted `Track`
     */
    private fun featureCollection(track: Track): JsonObject {
        val points = geoJsonPointFeatures(track.locationRecords)
        val type = Json.jsonKeyValue("type", "FeatureCollection")
        val features = Json.jsonKeyValue("features", points)
        return Json.jsonObject(type, features)
    }

    private fun geoJsonPointFeatures(list: List<RawRecord>): JsonArray =
        Json.jsonArray(list.map { geoJsonPointFeature(it).stringValue }.toTypedArray().toString())

    private fun geoJsonPointFeature(record: RawRecord): JsonObject {
        val type = Json.jsonKeyValue("type", "Feature")

        val geometryType = Json.jsonKeyValue("type", "Point")
        val lat = record.latitude.toString()
        val lon = record.longitude.toString()
        val coordinates = Json.jsonKeyValue("coordinates", Json.jsonArray(lon, lat))
        val geometry = Json.jsonKeyValue("geometry", Json.jsonObject(geometryType, coordinates))

        val timestamp = Json.jsonKeyValue("timestamp", record.timestamp)
        val speed = Json.jsonKeyValue("speed", record.speed)
        val accuracy = Json.jsonKeyValue("accuracy", record.accuracy)
        val modality = Json.jsonKeyValue("modality", record.modality.databaseIdentifier)
        val properties = Json.jsonKeyValue("properties", Json.jsonObject(timestamp, speed, accuracy, modality))

        return Json.jsonObject(type, geometry, properties)
    }

    /**
     * Clears the data within this measurement starting at the provided `timestamp` in milliseconds since the
     * 01.01.1970 (UNIX Epoch).
     *
     *
     * This call modifies the called measurement.
     *
     * @param timestamp The timestamp in milliseconds since the first of January 1970 to begin clearing the data at
     * @return This cleared `Measurement`
     * @throws TimestampNotFound If the timestamp is not within the timeframe of this measurement
     */
    @Suppress("unused") // Part of the API
    @Throws(TimestampNotFound::class)
    fun clearAfter(timestamp: Long): Measurement {
        val trackIndex = getIndexOfTrackContaining(timestamp)
        while (tracks.size - 1 > trackIndex) {
            tracks.removeAt(tracks.size - 1)
        }
        tracks[trackIndex].clearAfter(timestamp)
        return this
    }

    /**
     * Tries to find the track from this measurement containing the provided timestamp.
     *
     * @param timestamp A timestamp in milliseconds since the first of January 1970
     * @return The index of the track containing the provided timestamp
     * @throws TimestampNotFound If the timestamp is not within the timeframe of this measurement
     */
    @Throws(TimestampNotFound::class)
    private fun getIndexOfTrackContaining(timestamp: Long): Int {
        LOGGER.trace(
            "Getting track index for timestamp: {} ({})!",
            SimpleDateFormat.getDateTimeInstance().format(Date(timestamp)), timestamp
        )
        for (i in tracks.indices) {
            val track = tracks[i]
            val minRotationsTimestamp = if (track.rotations.isEmpty()) Long.MAX_VALUE else
                track.rotations[0].timestamp
            val minDirectionsTimestamp = if (track.directions.isEmpty()) Long.MAX_VALUE else
                track.directions[0].timestamp
            val minAccelerationsTimestamp = if (track.accelerations.isEmpty()) Long.MAX_VALUE else
                track.accelerations[0].timestamp
            val minLocationsTimestamp = if (track.locationRecords.isEmpty()) Long.MAX_VALUE else
                track.locationRecords[0].timestamp
            val minTrackTimestamp = min(
                minRotationsTimestamp.toDouble(),
                min(
                    minDirectionsTimestamp.toDouble(),
                    min(minAccelerationsTimestamp.toDouble(), minLocationsTimestamp.toDouble())
                )
            ).toLong()
            Validate.isTrue(minTrackTimestamp < Long.MAX_VALUE)

            val maxRotationsTimestamp = if (track.rotations.isEmpty()) Long.MIN_VALUE else
                track.rotations[track.rotations.size - 1].timestamp
            val maxDirectionsTimestamp = if (track.directions.isEmpty()) Long.MIN_VALUE else
                track.directions[track.directions.size - 1].timestamp
            val maxAccelerationsTimestamp = if (track.accelerations.isEmpty()) Long.MIN_VALUE else
                track.accelerations[track.accelerations.size - 1].timestamp
            val maxLocationsTimestamp = if (track.locationRecords.isEmpty()) Long.MIN_VALUE else
                track.locationRecords[track.locationRecords.size - 1].timestamp
            val maxTrackTimestamp = max(
                maxRotationsTimestamp.toDouble(),
                max(
                    maxDirectionsTimestamp.toDouble(),
                    max(maxAccelerationsTimestamp.toDouble(), maxLocationsTimestamp.toDouble())
                )
            ).toLong()
            Validate.isTrue(maxTrackTimestamp > Long.MIN_VALUE)

            LOGGER.trace(
                "Min timestamp for index {} is {} ({}).", i,
                SimpleDateFormat.getDateTimeInstance().format(Date(minTrackTimestamp)), minTrackTimestamp
            )
            LOGGER.trace(
                "Max timestamp for index {} is {} ({}).", i,
                SimpleDateFormat.getDateTimeInstance().format(Date(maxTrackTimestamp)), maxTrackTimestamp
            )
            if (timestamp >= minTrackTimestamp && timestamp <= maxTrackTimestamp) {
                LOGGER.trace("Selected index {}.", i)
                return i
            }
        }
        throw TimestampNotFound(
            String.format(
                Locale.getDefault(),
                "Unable to find track index for timestamp %s (%s) in measurement %s!",
                SimpleDateFormat.getDateTimeInstance().format(Date(timestamp)), timestamp,
                metaData
            )
        )
    }

    /**
     * Converts one location entry annotated with metadata to a CSV row.
     *
     * @param options The options which describe which data should be exported.
     * @param username the name of the user who uploaded the data or `null` to not annotate a username
     * @param metaData the `Measurement` of the {@param location}
     * @param locationRecord the `GeoLocationRecord` to be processed
     * @param trackId the id of the sub track starting at 1
     * @param modalityTypeDistance the distance traveled so far with this {@param modality} type
     * @param totalDistance the total distance traveled so far
     * @param modalityTypeTravelTime the time traveled so far with this {@param modality} type
     * @param totalTravelTime the time traveled so far
     * @return the csv row as String
     */
    private fun csvRow(
        options: ExportOptions,
        username: String?,
        metaData: MetaData,
        locationRecord: RawRecord,
        trackId: Int,
        modalityTypeDistance: Double,
        totalDistance: Double,
        modalityTypeTravelTime: Long,
        totalTravelTime: Long,
    ): String {
        val userId = metaData.userId
        val deviceId = metaData.identifier!!.deviceIdentifier
        val measurementId = metaData.identifier!!.measurementIdentifier.toString()

        val elements = ArrayList<String?>()
        if (options.includeUserId) {
            elements.add(userId.toString())
        }
        if (options.includeUsername) {
            Validate.notNull(username)
            elements.add(username)
        }
        elements.addAll(
            java.util.List.of(
                deviceId, measurementId, trackId.toString(),
                locationRecord.timestamp.toString(),
                locationRecord.latitude.toString(),
                locationRecord.longitude.toString(), locationRecord.speed.toString(),
                locationRecord.accuracy.toString(),
                locationRecord.modality.databaseIdentifier,
                modalityTypeDistance.toString(), totalDistance.toString(),
                modalityTypeTravelTime.toString(), totalTravelTime.toString()
            )
        )
        return java.lang.String.join(",", elements)
    }

    private fun csvSensorRow(
        options: ExportOptions, username: String?, metaData: MetaData,
        pointRecord: Point3DImpl,
        trackId: Int
    ): String {
        val userId = metaData.userId
        val deviceId = metaData.identifier!!.deviceIdentifier
        val measurementId = metaData.identifier!!.measurementIdentifier.toString()

        val elements = ArrayList<String?>()
        if (options.includeUserId) {
            elements.add(userId.toString())
        }
        if (options.includeUsername) {
            Validate.notNull(username)
            elements.add(username)
        }
        elements.addAll(
            listOf(
                deviceId, measurementId, trackId.toString(),
                pointRecord.timestamp.toString(),
                pointRecord.x.toString(),
                pointRecord.y.toString(),
                pointRecord.z.toString()
            )
        )
        return java.lang.String.join(",", elements)
    }

    /**
     * Converts a single track to geoJson "coordinates".
     *
     * @param tracks the `Track`s to be processed
     * @return the string representation of the geoJson coordinates
     */
    private fun convertToLineStringCoordinates(tracks: List<Track>): String {
        // Each track is a LineString
        val coordinates = tracks.map { track ->
            "[${track.locationRecords.joinToString(",") { geoJsonCoordinates(it).stringValue }}]"
        }
        return "[${coordinates.joinToString(",")}]"
    }

    private fun geoJsonCoordinates(record: RawRecord): JsonArray {
        return Json.jsonArray(record.longitude.toString(), record.latitude.toString())
    }

    override fun toString(): String {
        return "Measurement{" +
                "metaData=" + metaData +
                ", tracks=" + tracks +
                '}'
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as Measurement
        return metaData!! == that.metaData &&
                tracks == that.tracks
    }

    override fun hashCode(): Int {
        return Objects.hash(metaData, tracks)
    }

    companion object {
        /**
         * The logger used by objects of this class. Configure it using <tt>src/main/resources/logback.xml</tt>.
         */
        private val LOGGER: Logger = LoggerFactory.getLogger(Measurement::class.java)

        /**
         * Used to serialize objects of this class. Only change this value if this classes attribute set changes.
         */
        private const val serialVersionUID = 4195718001652533383L

        /**
         * Creates a CSV header for this measurement.
         *
         * @param options The options describing which data is exported
         * @param handler The handler that is notified of the new CSV row.
         */
        fun csvHeader(options: ExportOptions, handler: Consumer<String>) {
            val elements = ArrayList<String>()
            if (options.includeUserId) {
                elements.add("userId")
            }
            if (options.includeUsername) {
                elements.add("username")
            }
            elements.addAll(listOf("deviceId", "measurementId", "trackId", "timestamp [ms]"))
            when (options.type) {
                DataType.LOCATION -> elements.addAll(
                    listOf(
                        "latitude", "longitude",
                        "speed [m/s]", "accuracy [m]", "modalityType", "modalityTypeDistance [m]", "distance [m]",
                        "modalityTypeTravelTime [ms]", "travelTime [ms]"
                    )
                )

                DataType.ACCELERATION -> elements.addAll(listOf("x [m/s^2]", "y [m/s^2]", "z [m/s^2]"))
                DataType.ROTATION -> elements.addAll(listOf("x [rad/s]", "y [rad/s]", "z [rad/s]"))
                DataType.DIRECTION -> elements.addAll(listOf("x [uT]", "y [uT]", "z [uT]"))
                else -> throw IllegalArgumentException("Unsupported type: ${options.type}")
            }

            val csvHeaderRow = java.lang.String.join(",", elements)
            handler.accept(csvHeaderRow)
            handler.accept("\r\n")
        }
    }
}
