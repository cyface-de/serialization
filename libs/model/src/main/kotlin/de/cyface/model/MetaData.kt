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

import java.io.Serializable
import java.util.Date
import java.util.Objects
import java.util.UUID

/**
 * The context of a deserialized `Measurement`.
 *
 * The no-arg constructor and property setters are required for Apache Flink.
 *
 * @property identifier The worldwide unique identifier of the measurement.
 * @property deviceType The type of device uploading the data, such as "Pixel 3" or "iPhone 6 Plus".
 * @property osVersion The operating system version, such as "Android 9.0.0" or "iOS 11.2".
 * @property appVersion The version of the app that transmitted the measurement, such as "1.2.0" or "1.2.0-beta1".
 * @property length The length of the measurement in meters.
 * @property userId The id of the user who has uploaded this measurement. This needs to be in the UUID-format.
 * @property version The format version in which the `Measurement` was deserialized, e.g. "1.2.3".
 * @property uploadDate The upload date when the `Measurement` was uploaded to the collector.
 */
// @NoArg The kotlin no-arg plugin (together with all-open) does not add a no-argument constructor to the generated
// MetaData.class file, so we have to add the no-argument constructor manually for Apache Flink.
@SuppressWarnings("LongParameterList")
class MetaData: Serializable {

    lateinit var identifier: MeasurementIdentifier
    lateinit var deviceType: String
    lateinit var osVersion: String
    lateinit var appVersion: String
    var length: Double = -1.0 // Default value required for no-arg constructor (Flink). Validated in [validate()]
    lateinit var userId: String // Cannot use `UUID` type as it is not serializable by Flink (GenericType warning)
    lateinit var version: String
    lateinit var uploadDate: Date

    fun validate() {
        require(version.matches(SUPPORTED_VERSIONS.toRegex())) { "Unsupported version: $version" }
        require(length != -1.0) { "Length must be set before usage" }
        require(runCatching { UUID.fromString(userId) }.isSuccess) {
            "userId must be a valid UUID string, was: $userId"
        }
    }

    override fun toString(): String {
        return "MetaData{" +
            "identifier=" + identifier +
            ", deviceType='" + deviceType + '\'' +
            ", osVersion='" + osVersion + '\'' +
            ", appVersion='" + appVersion + '\'' +
            ", length=" + length +
            ", userId='" + userId + '\'' +
            ", version='" + version + '\'' +
            ", uploadDate='" + uploadDate.time + '\'' +
            '}'
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val metaData = other as MetaData
        return metaData.length.compareTo(length) == 0 &&
                identifier == metaData.identifier &&
                deviceType == metaData.deviceType &&
                osVersion == metaData.osVersion &&
                appVersion == metaData.appVersion &&
                userId == metaData.userId &&
                version == metaData.version &&
                uploadDate == metaData.uploadDate
    }

    override fun hashCode(): Int {
        return Objects.hash(identifier, deviceType, osVersion, appVersion, length, userId, version, uploadDate)
    }

    companion object {
        /**
         * The current version of the deserialized measurement model.
         *
         * To be able to read measurements deserialized by different deserializer versions.
         */
        const val CURRENT_VERSION: String = "3.1.0"

        /**
         * Regex of supported [MetaData] versions of this class.
         */
        const val SUPPORTED_VERSIONS: String = "3.1.0"

        /**
         * Used to serialize objects of this class. Only change this value if this classes attribute set changes.
         */
        private const val serialVersionUID = -22272L

        /**
         * Factory which ensures the `validate` is called during initialization.
         *
         * We cannot use `init{}` because of the no-arg constructor required for Apache Flink.
         */
        fun create(
            identifier: MeasurementIdentifier,
            deviceType: String,
            osVersion: String,
            appVersion: String,
            length: Double,
            userId: UUID,
            version: String,
            uploadDate: Date,
        ) = MetaData().apply {
            this.identifier = identifier
            this.deviceType = deviceType
            this.osVersion = osVersion
            this.appVersion = appVersion
            this.length = length
            this.userId = userId.toString()
            this.version = version
            this.uploadDate = uploadDate
            validate()
        }
    }
}
