/*
 * Copyright 2021-2025 Cyface GmbH
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

import java.util.function.Consumer

/**
 * This class allows structuring data in the Json format without Json dependencies.
 *
 * @author Armin Schnabel
 * @since 1.1.0
 * @version 1.2.1
 */
object Json {
    /**
     * Creates a [JsonArray] with the supplied `objects`.
     *
     * @param objects the objects to assemble as `JsonArray`, e.g. ["lon", "lat"]
     * @return the `JsonArray`
     */
    fun jsonArray(objects: List<String>): JsonArray {
        val builder = StringBuilder("[")
        if (objects.isNotEmpty()) {
            objects.forEach { p: String -> builder.append(p).append(",") }
            builder.deleteCharAt(builder.length - 1) // remove trailing comma
        }
        builder.append("]")
        return JsonArray(builder.toString())
    }

    /**
     * Creates a [JsonObject] with the supplies `keyValuePairs`.
     *
     * @param keyValuePairs The key-value pairs to be injected into the object
     * @return the created `JsonObject`
     */
    fun jsonObject(keyValuePairs: List<KeyValuePair>): JsonObject {
        val builder = StringBuilder("{")
        if (keyValuePairs.isNotEmpty()) {
            keyValuePairs.forEach { p: KeyValuePair -> builder.append(p.stringValue).append(",") }
            builder.deleteCharAt(builder.length - 1) // remove trailing comma
        }
        builder.append("}")
        return JsonObject(builder.toString())
    }

    /**
     * Creates a [KeyValuePair] from the supplied key and value.
     *
     * @param key the name of the key to be used
     * @param value the value as [JsonArray]
     * @return the created `KeyValuePair`
     */
    fun jsonKeyValue(
        key: String,
        value: JsonArray
    ): KeyValuePair {
        return KeyValuePair("\"" + key + "\":" + value.stringValue)
    }

    /**
     * Creates a [KeyValuePair] from the supplied key and value.
     *
     * @param key the name of the key to be used
     * @param value the value as [JsonObject]
     * @return the created `KeyValuePair`
     */
    fun jsonKeyValue(
        key: String,
        value: JsonObject
    ): KeyValuePair {
        return KeyValuePair("\"" + key + "\":" + value.stringValue)
    }

    /**
     * Creates a [KeyValuePair] from the supplied key and value.
     *
     * @param key the name of the key to be used
     * @param value the value as `Long`
     * @return the created `KeyValuePair`
     */
    fun jsonKeyValue(
        key: String,
        value: Long
    ): KeyValuePair {
        return KeyValuePair("\"$key\":$value")
    }

    /**
     * Creates a [KeyValuePair] from the supplied key and value.
     *
     * @param key the name of the key to be used
     * @param value the value as `Boolean`
     * @return the created `KeyValuePair`
     */
    fun jsonKeyValue(
        key: String,
        value: Boolean
    ): KeyValuePair {
        return KeyValuePair("\"$key\":$value")
    }

    /**
     * Creates a [KeyValuePair] from the supplied key and value.
     *
     * @param key the name of the key to be used
     * @param value the value as `Double`
     * @return the created `KeyValuePair`
     */
    fun jsonKeyValue(
        key: String,
        value: Double
    ): KeyValuePair {
        return KeyValuePair("\"$key\":$value")
    }

    /**
     * Creates a [KeyValuePair] from the supplied key and value.
     *
     *
     * Replaces double quotes in `String` values with single quotes to ensure the String is parsable [DAT-1313].
     *
     * @param key the name of the key to be used
     * @param value the value as `String`
     * @return the created `KeyValuePair`
     */
    @JvmStatic
    fun jsonKeyValue(key: String, value: String): KeyValuePair {
        val processedValue = value.replace('"', '\'')
        return KeyValuePair("\"$key\":\"$processedValue\"")
    }

    /**
     * @return the `String` representation of the [KeyValuePair]
     */
    /**
     * This class represents the typical key-value format of a JSON object.
     *
     * @author Armin Schnabel
     * @since 1.1.0
     */
    class KeyValuePair
    /**
     * Creates a fully initialized instance of this class.
     *
     * @param stringValue the `String` representation of the [KeyValuePair]
     */(
        /**
         * The `String` representation of the [KeyValuePair].
         */
        @JvmField val stringValue: String
    )

    /**
     * This class represents the typical format of a JSON object.
     *
     * @author Armin Schnabel
     * @since 1.1.0
     * @version 1.1.1
     */
    class JsonObject
    /**
     * Creates a fully initialized instance of this class.
     *
     * @param stringValue the `String` representation of the [JsonObject]
     */(
        /**
         * The `String` representation of the [JsonObject].
         */
        val stringValue: String
    ) {
        /**
         * @return The `String` representation of the [JsonObject].
         */

        override fun toString(): String {
            return stringValue
        }

        /**
         * Builder for creating a [JsonObject].
         *
         * @author Armin Schnabel
         * @since 1.2.0
         * @version 1.0.1
         */
        class Builder {
            /**
             * The [KeyValuePair]s to be added to the [JsonObject].
             */
            private val keyValuePairs: MutableList<KeyValuePair> = ArrayList()

            /**
             * Adds the supplied `pair` to this builder.
             *
             * @param pair the [KeyValuePair] to add
             * @return this builder
             */
            fun add(pair: KeyValuePair): Builder {
                keyValuePairs.add(pair)
                return this
            }

            /**
             * Adds the supplied `pairs` to this builder.
             *
             * @param pairs the [KeyValuePair]s to add
             * @return this builder
             */
            fun addAll(vararg pairs: KeyValuePair): Builder {
                keyValuePairs.addAll(listOf(*pairs))
                return this
            }

            /**
             * Creates a [JsonObject] with the supplied `keyValuePairs`.
             *
             * @return the `JsonObject`
             */
            fun build(): JsonObject {
                val builder = StringBuilder("{")
                if (keyValuePairs.size > 0) {
                    keyValuePairs.forEach(Consumer { p: KeyValuePair -> builder.append(p.stringValue).append(",") })
                    builder.deleteCharAt(builder.length - 1) // remove trailing comma
                }
                builder.append("}")
                return JsonObject(builder.toString())
            }
        }
    }

    /**
     * This class represents the typical format of an JSON array.
     *
     * @author Armin Schnabel
     * @since 1.1.0
     * @version 1.1.1
     */
    class JsonArray
    /**
     * Creates a fully initialized instance of this class.
     *
     * @param stringValue the `String` representation of the [JsonArray]
     */(
        /**
         * The `String` representation of the [JsonArray].
         */
        val stringValue: String
    ) {
        /**
         * @return The `String` representation of the [JsonArray].
         */

        override fun toString(): String {
            return stringValue
        }

        /**
         * Builder for creating a [JsonArray].
         *
         * @author Armin Schnabel
         * @since 1.3.0
         * @version 1.0.0
         */
        class Builder {
            /**
             * The objects to be added to the [JsonArray].
             */
            private val objects: MutableList<JsonObject> = ArrayList()

            /**
             * Adds the supplied `object` to this builder.
             *
             * @param `object` the [JsonObject] to add
             * @return this builder
             */
            fun add(jsonObject: JsonObject): Builder {
                objects.add(jsonObject)
                return this
            }

            /**
             * Adds the supplied `objects` to this builder.
             *
             * @param objects the [JsonObject]s to add
             * @return this builder
             */
            fun addAll(vararg objects: JsonObject): Builder {
                this.objects.addAll(listOf(*objects))
                return this
            }

            /**
             * Creates a [JsonArray] with the supplied `objects`.
             *
             * @return the `JsonArray`
             */
            fun build(): JsonArray {
                val builder = StringBuilder("[")
                if (objects.size > 0) {
                    objects.forEach(Consumer { o: JsonObject -> builder.append(o.stringValue).append(",") })
                    builder.deleteCharAt(builder.length - 1) // remove trailing comma
                }
                builder.append("]")
                return JsonArray(builder.toString())
            }
        }
    }
}
