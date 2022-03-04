/*
 * Copyright 2021-2022 Cyface GmbH
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class allows structuring data in the Json format without Json dependencies.
 *
 * @author Armin Schnabel
 * @since 1.1.0
 * @version 1.1.0
 */
public class Json {

    /**
     * Creates a {@link JsonArray} with the supplied {@code objects}.
     *
     * @param objects the objects to assemble as {@code JsonArray}, e.g. ["lon", "lat"]
     * @return the {@code JsonArray}
     */
    public static JsonArray jsonArray(final String... objects) {
        final var builder = new StringBuilder("[");
        Arrays.stream(objects).forEach(p -> builder.append(p).append(","));
        builder.deleteCharAt(builder.length() - 1); // remove trailing comma
        builder.append("]");
        return new JsonArray(builder.toString());
    }

    /**
     * Creates a {@link JsonObject} with the supplies {@code keyValuePairs}.
     *
     * @param keyValuePairs The key-value pairs to be injected into the object
     * @return the created {@code JsonObject}
     */
    public static JsonObject jsonObject(final KeyValuePair... keyValuePairs) {
        final var builder = new StringBuilder("{");
        Arrays.stream(keyValuePairs).forEach(p -> builder.append(p.stringValue).append(","));
        builder.deleteCharAt(builder.length() - 1); // remove trailing comma
        builder.append("}");
        return new JsonObject(builder.toString());
    }

    /**
     * Creates a {@link KeyValuePair} from the supplied key and value.
     *
     * @param key the name of the key to be used
     * @param value the value as {@link JsonArray}
     * @return the created {@code KeyValuePair}
     */
    public static KeyValuePair jsonKeyValue(@SuppressWarnings("SameParameterValue") final String key,
            final JsonArray value) {
        return new KeyValuePair("\"" + key + "\":" + value.stringValue);
    }

    /**
     * Creates a {@link KeyValuePair} from the supplied key and value.
     *
     * @param key the name of the key to be used
     * @param value the value as {@link JsonObject}
     * @return the created {@code KeyValuePair}
     */
    public static KeyValuePair jsonKeyValue(@SuppressWarnings("SameParameterValue") final String key,
            final JsonObject value) {
        return new KeyValuePair("\"" + key + "\":" + value.stringValue);
    }

    /**
     * Creates a {@link KeyValuePair} from the supplied key and value.
     *
     * @param key the name of the key to be used
     * @param value the value as {@code Long}
     * @return the created {@code KeyValuePair}
     */
    public static KeyValuePair jsonKeyValue(@SuppressWarnings("SameParameterValue") final String key,
            final long value) {
        return new KeyValuePair("\"" + key + "\":" + value);
    }

    /**
     * Creates a {@link KeyValuePair} from the supplied key and value.
     *
     * @param key the name of the key to be used
     * @param value the value as {@code Boolean}
     * @return the created {@code KeyValuePair}
     */
    public static KeyValuePair jsonKeyValue(@SuppressWarnings("SameParameterValue") final String key,
            final boolean value) {
        return new KeyValuePair("\"" + key + "\":" + value);
    }

    /**
     * Creates a {@link KeyValuePair} from the supplied key and value.
     *
     * @param key the name of the key to be used
     * @param value the value as {@code Double}
     * @return the created {@code KeyValuePair}
     */
    public static KeyValuePair jsonKeyValue(@SuppressWarnings("SameParameterValue") final String key,
            final double value) {
        return new KeyValuePair("\"" + key + "\":" + value);
    }

    /**
     * Creates a {@link KeyValuePair} from the supplied key and value.
     *
     * @param key the name of the key to be used
     * @param value the value as {@code String}
     * @return the created {@code KeyValuePair}
     */
    public static KeyValuePair jsonKeyValue(final String key, final String value) {
        return new KeyValuePair("\"" + key + "\":\"" + value + "\"");
    }

    /**
     * This class represents the typical key-value format of a JSON object.
     *
     * @author Armin Schnabel
     * @since 1.1.0
     */
    public static class KeyValuePair {
        /**
         * The {@code String} representation of the {@link KeyValuePair}.
         */
        private final String stringValue;

        /**
         * Creates a fully initialized instance of this class.
         *
         * @param stringValue the {@code String} representation of the {@link KeyValuePair}
         */
        public KeyValuePair(String stringValue) {
            this.stringValue = stringValue;
        }

        /**
         * @return the {@code String} representation of the {@link KeyValuePair}
         */
        public String getStringValue() {
            return stringValue;
        }
    }

    /**
     * This class represents the typical format of a JSON object.
     *
     * @author Armin Schnabel
     * @since 1.1.0
     * @version 1.1.0
     */
    public static class JsonObject {
        /**
         * The {@code String} representation of the {@link JsonObject}.
         */
        private final String stringValue;

        /**
         * Creates a fully initialized instance of this class.
         *
         * @param stringValue the {@code String} representation of the {@link JsonObject}
         */
        public JsonObject(String stringValue) {
            this.stringValue = stringValue;
        }

        /**
         * @return The {@code String} representation of the {@link JsonObject}.
         */
        public String getStringValue() {
            return stringValue;
        }

        /**
         * Builder for creating a {@link JsonObject}.
         *
         * @author Armin Schnabel
         * @since 1.2.0
         * @version 1.0.0
         */
        public static class Builder {

            /**
             * The object to be added to the {@link JsonObject}.
             */
            private final List<KeyValuePair> objects = new ArrayList<>();

            /**
             * Adds the supplied {@code objects} to this builder.
             *
             * @param object the {@link KeyValuePair} to add
             * @return this builder
             */
            public Builder add(final KeyValuePair object) {
                this.objects.add(object);
                return this;
            }

            /**
             * Creates a {@link JsonObject} with the supplied {@code objects}.
             *
             * @return the {@code JsonObject}
             */
            public JsonObject build() {
                final var builder = new StringBuilder("{");
                objects.forEach(p -> builder.append(p.stringValue).append(","));
                builder.deleteCharAt(builder.length() - 1); // remove trailing comma
                builder.append("}");
                return new JsonObject(builder.toString());
            }
        }
    }

    /**
     * This class represents the typical format of an JSON array.
     *
     * @author Armin Schnabel
     * @since 1.1.0
     * @version 1.0.0
     */
    public static class JsonArray {
        /**
         * The {@code String} representation of the {@link JsonArray}.
         */
        private final String stringValue;

        /**
         * Creates a fully initialized instance of this class.
         *
         * @param stringValue the {@code String} representation of the {@link JsonArray}
         */
        public JsonArray(String stringValue) {
            this.stringValue = stringValue;
        }

        /**
         * @return The {@code String} representation of the {@link JsonArray}.
         */
        public String getStringValue() {
            return stringValue;
        }
    }
}
