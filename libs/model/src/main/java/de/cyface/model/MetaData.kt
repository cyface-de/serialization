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
 */
class MetaData : Serializable {
    /**
     * The worldwide unique identifier of the measurement.
     *
     * The setter is required by Apache Flink.
     */
    @JvmField
    var identifier: MeasurementIdentifier? = null
    /**
     * The type of device uploading the data, such as "Pixel 3" or "iPhone 6 Plus".
     *
     * The setter is required by Apache Flink.
     */
    @JvmField
    @set:Suppress("unused")
    var deviceType: String? = null
    /**
     * The operating system version, such as "Android 9.0.0" or "iOS 11.2".
     *
     * The setter is required by Apache Flink.
     */
    @JvmField
    @set:Suppress("unused")
    var osVersion: String? = null
    /**
     * The version of the app that transmitted the measurement, such as "1.2.0" or "1.2.0-beta1".
     *
     * The setter is required by Apache Flink.
     */
    @JvmField
    @set:Suppress("unused")
    var appVersion: String? = null
    /**
     * The length of the measurement in meters.
     *
     * The setter is required by Apache Flink.
     */
    @JvmField
    @set:Suppress("unused")
    var length: Double = 0.0
    /**
     * The id of the user who has uploaded this measurement.
     *
     * The setter is required by Apache Flink.
     */
    @JvmField
    @set:Suppress("unused")
    var userId: UUID? = null
    /**
     * The format version in which the `Measurement` was deserialized, e.g. "1.2.3".
     *
     * The setter is required by Apache Flink.
     */
    @JvmField
    var version: String? = null
    /**
     * The upload date when the `Measurement` was uploaded to the collector.
     *
     * The setter is required by Apache Flink.
     */
    @JvmField
    var uploadDate: Date? = null

    /**
     * Creates new completely initialized `MetaData`.
     *
     * @param identifier The worldwide unique identifier of the measurement.
     * @param deviceType The type of device uploading the data, such as "Pixel 3" or "iPhone 6 Plus".
     * @param osVersion The operating system version, such as "Android 9.0.0" or "iOS 11.2".
     * @param appVersion The version of the app that transmitted the measurement, such as "1.2.0" or "1.2.0-beta1".
     * @param length The length of the measurement in meters.
     * @param userId The id of the user who has uploaded this measurement.
     * @param version The format version in which the `Measurement` was deserialized, e.g. "2.0.0".
     * @param uploadDate The upload date when the `Measurement` was uploaded to the collector.
     */
    constructor(
        identifier: MeasurementIdentifier?,
        deviceType: String,
        osVersion: String,
        appVersion: String,
        length: Double,
        userId: UUID,
        version: String,
        uploadDate: Date,
    ) {
        require(version.matches(SUPPORTED_VERSIONS.toRegex())) { "Unsupported version: $version" }
        this.identifier = identifier
        this.deviceType = deviceType
        this.osVersion = osVersion
        this.appVersion = appVersion
        this.length = length
        this.userId = userId
        this.version = version
        this.uploadDate = uploadDate
    }

    /**
     * No argument constructor as required by Apache Flink. Do not use this in your own code.
     */
    @Suppress("unused")
    constructor()

    override fun toString(): String {
        return "MetaData{" +
                "identifier=" + identifier +
                ", deviceType='" + deviceType + '\'' +
                ", osVersion='" + osVersion + '\'' +
                ", appVersion='" + appVersion + '\'' +
                ", length=" + length +
                ", userId='" + userId + '\'' +
                ", version='" + version + '\'' +
                ", uploadDate='" + uploadDate?.time + '\'' +
                '}'
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val metaData = other as MetaData
        return metaData.length.compareTo(length) == 0 &&
                identifier!! == metaData.identifier &&
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
        const val SUPPORTED_VERSIONS: String = "3.1.0" // FIXME: Can/Should we support 3.0.0 and 3.1.0?

        /**
         * Used to serialize objects of this class. Only change this value if this classes attribute set changes.
         */
        private const  val serialVersionUID = -15050L
    }
}
