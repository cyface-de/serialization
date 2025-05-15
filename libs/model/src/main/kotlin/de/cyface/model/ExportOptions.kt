/*
 * Copyright 2024 Cyface GmbH
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

/**
 * The options which describe which data should be exported.
 *
 * @author Armin Schnabel
 * @version 1.0.0
 * @since 3.3.0
 * @property format The format in which the data should be exported.
 * @property type The type of data which should be exported.
 * @property includeHeader `true` if the export should start with a header line, if supported by the [format].
 * @property includeUserId `true` if the user id should be included in the export.
 * @property includeUsername `true` if the username should be included in the export.
 */
class ExportOptions {
    var format: DataFormat = DataFormat.UNDEFINED
    var type: DataType = DataType.UNDEFINED
    var includeHeader: Boolean = false
    var includeUserId: Boolean = false
    var includeUsername: Boolean = false
    var includeModalityType: Boolean = false
    var includeModalityTypeDistance: Boolean = false
    var includeTotalDistance: Boolean = false
    var includeModalityTypeTravelTime: Boolean = false
    var includeTotalTravelTime: Boolean = false

    fun format(format: DataFormat): ExportOptions {
        this.format = format
        return this
    }

    fun type(type: DataType): ExportOptions {
        this.type = type
        return this
    }

    fun includeHeader(includeHeader: Boolean): ExportOptions {
        require(!includeHeader || format == DataFormat.CSV) { "Format without header support: $format" }
        this.includeHeader = includeHeader
        return this
    }

    fun includeUserId(includeUserId: Boolean): ExportOptions {
        this.includeUserId = includeUserId
        return this
    }

    fun includeUsername(includeUsername: Boolean): ExportOptions {
        this.includeUsername = includeUsername
        return this
    }

    fun includeModalityType(include: Boolean): ExportOptions {
        this.includeModalityType = include
        return this
    }

    fun includeModalityTypeDistance(include: Boolean): ExportOptions {
        this.includeModalityTypeDistance = include
        return this
    }

    fun includeTotalDistance(include: Boolean): ExportOptions {
        this.includeTotalDistance = include
        return this
    }

    fun includeModalityTypeTravelTime(include: Boolean): ExportOptions {
        this.includeModalityTypeTravelTime = include
        return this
    }

    fun includeTotalTravelTime(include: Boolean): ExportOptions {
        this.includeTotalTravelTime = include
        return this
    }
}

/**
 * Supported export data format.
 *
 * @author Armin Schnabel
 * @version 1.0.0
 * @since 3.3.0
 * @param parameterValue The parameter value which represents the enum option.
 */
enum class DataFormat(
    val parameterValue: String
) {
    /**
     * Default value when no format was specified.
     */
    UNDEFINED("undefined"),

    /**
     * Comma separated values.
     */
    CSV("csv"),

    /**
     * JSON format.
     */
    @Suppress("unused") // Used by Provider
    JSON("json"),

    /**
     * JSON-LINES format: a streamable text format consisting of one JSON document per line.
     *
     * Each line is a complete and valid JSON object. The format is designed for efficient streaming
     * and incremental parsing, especially for large datasets. Unlike standard JSON arrays,
     * this format avoids the need for opening/closing brackets or commas between elements.
     *
     * This format is widely supported by tools like jq, pandas, and many log processors.
     *
     * Common file extension: .jsonl
     * MIME type: application/x-ndjson
     *
     * Note: Not a standard IANA-registered MIME type, but commonly used in APIs.
     */
    JSONL("jsonl"),

    /**
     * BSON-LINES format: a non-standard, streamable format consisting of
     * multiple consecutive BSON documents written one after another.
     *
     * Each document is a valid standalone BSON object, prefixed with its
     * own length. This format is analogous to JSON Lines (.jsonl), but
     * uses BSON's binary encoding.
     *
     * Note: This is not compatible with mongodump/mongorestore.
     * Consumers must read and parse each BSON document individually.
     *
     * Common file extension: .bsonl
     * MIME type (non-standard): application/x-bsonl
     */
    @Suppress("unused", "SpellCheckingInspection") // Used by Provider
    BSONL("bsonl");

    companion object {
        private val BY_PARAMETER_VALUE: MutableMap<String, DataFormat> = HashMap()

        init {
            entries.forEach { format ->
                BY_PARAMETER_VALUE[format.parameterValue] = format
            }
        }

        /**
         * Returns the [DataFormat] from it's `#parameterValue` value.
         *
         * @param parameterValue The `String` value of the parameterValue.
         * @return The `DataFormat` for the parameterValue.
         */
        @Suppress("unused") // Part of the API
        fun of(parameterValue: String): DataFormat? {
            return BY_PARAMETER_VALUE[parameterValue]
        }
    }
}

/**
 * Supported data types which are used during data collection.
 *
 * @author Armin Schnabel
 * @version 1.0.0
 * @since 3.3.0
 * @param parameterValue The parameter value which represents the enum option.
 */
enum class DataType(@Suppress("MemberVisibilityCanBePrivate") val parameterValue: String) {
    /**
     * Default value when no type was specified.
     */
    UNDEFINED("undefined"),

    /**
     * Data collected by the accelerometer.
     */
    ACCELERATION("acceleration"),

    /**
     * Data collected by the gyroscope.
     */
    ROTATION("rotation"),

    /**
     * Data collected by the magnetometer.
     */
    DIRECTION("direction"),

    /**
     * Location data, e.g. collected from a GNSS signal.
     */
    LOCATION("location");

    companion object {
        private val BY_PARAMETER_VALUE: MutableMap<String, DataType> = HashMap()

        init {
            entries.forEach { entry ->
                BY_PARAMETER_VALUE[entry.parameterValue] = entry
            }
        }

        /**
         * Returns the [DataType] from it's `#parameterValue` value.
         *
         * @param parameterValue The `String` value of the parameterValue.
         * @return The `DataType` for the parameterValue.
         */
        @Suppress("unused") // Part of the API
        fun of(parameterValue: String): DataType? {
            return BY_PARAMETER_VALUE[parameterValue]
        }
    }
}