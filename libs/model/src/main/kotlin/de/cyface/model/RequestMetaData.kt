/*
 * Copyright 2021-2023 Cyface GmbH
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

import java.io.Serializable
import java.nio.charset.Charset

/**
 * The metadata as transmitted in the request header or pre-request body.
 *
 * @author Armin Schnabel
 * @version 2.0.0
 * @since 6.0.0
 * @property deviceType The worldwide unique identifier of the device uploading the data.
 * @property measurementIdentifier The device wide unique identifier of the uploaded measurement.
 * @property operatingSystemVersion The operating system version, such as Android 9.0.0 or iOS 11.2.
 * @property deviceType The type of device uploading the data, such as Pixel 3 or iPhone 6 Plus.
 * @property applicationVersion The version of the app that transmitted the measurement.
 * @property length The length of the measurement in meters.
 * @property locationCount The count of geolocations in the transmitted measurement.
 * @property startLocation The `GeoLocation` at the beginning of the track represented by the transmitted measurement.
 * @property endLocation The `GeoLocation` at the end of the track represented by the transmitted measurement.
 * @property modality The modality type used to capture the measurement.
 * @property formatVersion The format version of the upload file.
 * @property logCount The number of log files captured for this measurement, e.g. metrics captured during distance-based image capturing.
 * @property imageCount The number of images captured for this measurement. This allows the backend to notice when all images are transmitted.
 * @property videoCount The number of videos captured for this measurement. This allows the backend to notice when all videos are transmitted.
 * @property filesSize The number of bytes of the files collected for this measurement (log, image and video data).
 */
@Suppress("unused") // Part of the API
data class RequestMetaData(
    val deviceIdentifier: String,
    val measurementIdentifier: String,
    val operatingSystemVersion: String,
    val deviceType: String,
    val applicationVersion: String,
    val length: Double,
    val locationCount: Long,
    val startLocation: GeoLocation?,
    val endLocation: GeoLocation?,
    val modality: String,
    val formatVersion: Int,
    val logCount: Int,
    val imageCount: Int,
    val videoCount: Int,
    val filesSize: Long
) : Serializable {

    init {
        require(deviceIdentifier.toByteArray(Charset.forName(DEFAULT_CHARSET)).size == UUID_LENGTH) {
            "Field deviceId was not exactly 128 Bit, which is required for UUIDs!"
        }
        require(deviceType.isNotEmpty() && deviceType.length <= MAX_GENERIC_METADATA_FIELD_LENGTH) {
            "Field deviceType had an invalid length of ${deviceType.length.toLong()}"
        }
        require(measurementIdentifier.isNotEmpty() && measurementIdentifier.length <= MAX_MEASUREMENT_ID_LENGTH) {
            "Field measurementId had an invalid length of ${measurementIdentifier.length.toLong()}"
        }
        require(operatingSystemVersion.isNotEmpty() && operatingSystemVersion.length <= MAX_GENERIC_METADATA_FIELD_LENGTH) {
            "Field osVersion had an invalid length of ${operatingSystemVersion.length.toLong()}"
        }
        require(applicationVersion.isNotEmpty() && applicationVersion.length <= MAX_GENERIC_METADATA_FIELD_LENGTH) {
            "Field applicationVersion had an invalid length of ${applicationVersion.length.toLong()}"
        }
        require(length >= MINIMUM_TRACK_LENGTH) {
            "Field length had an invalid value smaller then 0.0: $length"
        }
        require(locationCount >= MINIMUM_LOCATION_COUNT) {
            "Field locationCount had an invalid value smaller then 0: $locationCount"
        }
        require(locationCount == MINIMUM_LOCATION_COUNT || startLocation != null) {
            "Start location should only be defined if there is at least one location in the uploaded track!"
        }
        require(locationCount == MINIMUM_LOCATION_COUNT || endLocation != null) {
            "End location should only be defined if there is at least one location in the uploaded track!"
        }
        require(modality.isNotEmpty() && modality.length <= MAX_GENERIC_METADATA_FIELD_LENGTH) {
            "Field modality had an invalid length of ${modality.length.toLong()}"
        }
        require(formatVersion == CURRENT_TRANSFER_FILE_FORMAT_VERSION) {
            "Unsupported formatVersion: ${formatVersion.toLong()}"
        }
        require(logCount >= 0) { "Invalid logCount: $logCount" }
        require(imageCount >= 0) { "Invalid imageCount: $imageCount" }
        require(videoCount >= 0) { "Invalid videoCount: $videoCount" }
        require(filesSize >= 0) { "Invalid filesSize: $filesSize" }
    }

    /**
     * This class represents a geolocation at the start or end of a track.
     *
     * @author Armin Schnabel
     * @version 1.0.0
     * @since 6.0.0
     * @property timestamp The timestamp this location was captured on in milliseconds since 1st January 1970 (epoch).
     * @property latitude Geographical latitude in coordinates (decimal fraction) raging from -90° (south) to 90° (north).
     * @property longitude Geographical longitude in coordinates (decimal fraction) ranging from -180° (west) to 180° (east).
     */
    data class GeoLocation(
        val timestamp: Long,
        val latitude: Double,
        val longitude: Double
    )

    companion object {
        /**
         * Used to serialize objects of this class. Only change this value if this classes attribute set changes.
         */
        private const val serialVersionUID = -1700430112854515404L

        /**
         * The length of a universal unique identifier.
         */
        private const val UUID_LENGTH = 36

        /**
         * The default char set to use for encoding and decoding strings transmitted as metadata.
         */
        private const val DEFAULT_CHARSET = "UTF-8"

        /**
         * Maximum size of a metadata field, with plenty space for future development. This prevents attackers from putting
         * arbitrary long data into these fields.
         */
        const val MAX_GENERIC_METADATA_FIELD_LENGTH = 30

        /**
         * The maximum length of the measurement identifier in characters (this is the amount of characters of
         * {@value Long#MAX_VALUE}).
         */
        private const val MAX_MEASUREMENT_ID_LENGTH = 20

        /**
         * The minimum length of a track stored with a measurement.
         */
        private const val MINIMUM_TRACK_LENGTH = 0.0

        /**
         * The minimum valid amount of locations stored inside a measurement.
         */
        private const val MINIMUM_LOCATION_COUNT = 0L

        /**
         * The current version of the transferred file. This is always specified by the first two bytes of the file
         * transferred and helps compatible APIs to process data from different client versions.
         */
        const val CURRENT_TRANSFER_FILE_FORMAT_VERSION = 3
    }
}