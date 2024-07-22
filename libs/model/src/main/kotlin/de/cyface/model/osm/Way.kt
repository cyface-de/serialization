/*
 * Copyright 2019-2024 Cyface GmbH
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
package de.cyface.model.osm

import org.apache.commons.lang3.Validate
import java.util.Locale
import java.util.Objects
import java.util.Optional
import java.util.function.Function
import java.util.stream.Collectors

/**
 * A POJO representing an Open Street Map way.
 *
 * Objects of this class are supposed to be handled as immutable. This is not possible to enforce, since Apache Flink
 * requires public setters and a no argument constructor. But be advised, that mutating any properties is probably a
 * code smell.
 *
 * Two ways are comparable. Their natural ordering is from lowest `identifier` to highest `identifier`.
 *
 * @author Klemens Muthmann
 * @property identifier The OSM identifier of this way.
 * @property nodes The OSM nodes constituting this way.
 * @property tags A `Collection` of all the tags associated with this Open Street Map way.
 */
class Way<T : MapTag>(
    var identifier: Long = 0L,
    var nodes: Array<Node> = emptyArray(),
    private var tags: Map<String, T> = emptyMap(),
) : Comparable<Way<out MapTag>> {
    /**
     * A no argument constructor as required by Apache flink
     */
    @Suppress("unused") // Part of the API
    constructor(): this(0L, emptyArray(), emptyMap())

    /**
     * Creates a new completely initialized instance of this class.
     *
     * @param identifier The Open Street Map identifier of this way
     * @param nodes An array with nodes forming the way, in order of their occurrence
     * @param tags The tags associated with the way. This might be an empty `Collection`
     */
    @Suppress("unused") // Part of the API
    constructor(identifier: Long, nodes: Array<Node>, tags: Collection<T>) : this(identifier, nodes, emptyMap()) {
        Validate.isTrue(identifier > 0L)
        Validate.notNull(nodes)

        this.identifier = identifier
        this.nodes = nodes.copyOf()
        setTags(tags)
    }

    /**
     * @return The OSM nodes constituting this way.
     */
    fun getNodes(): Array<Node> = nodes.copyOf()

    /**
     * @param nodes The OSM nodes constituting this way.
     */
    fun setNodes(nodes: Array<Node>) {
        Validate.notNull(nodes)
        this.nodes = nodes.copyOf()
    }

    /**
     * @return The tags associated with the way. This might be an empty [Collection]
     */
    @Suppress("unused", "MemberVisibilityCanBePrivate") // Part of the API
    fun getTags(): Collection<T> = tags.values

    /**
     * Returns the Open Street Map tag associated with the provided key.
     *
     * @param key The key to look up
     * @return An [Optional] with the [T] if present; an empty [Optional] otherwise.
     */
    @Suppress("unused", "MemberVisibilityCanBePrivate") // Part of the API
    fun getTag(key: String): Optional<T> = Optional.ofNullable(tags[key])

    /**
     * @param tags The tags associated with the way. This might be an empty [Collection]
     */
    @Suppress("unused", "MemberVisibilityCanBePrivate") // Part of the API
    fun setTags(tags: Collection<T>) {
        Validate.notNull(tags)
        this.tags = tags.stream().collect(Collectors.toMap({ it.key }, Function.identity()))
    }

    override fun toString(): String {
        return "Way(identifier=$identifier, nodes=${nodes.contentToString()}, tags=${tags.values})"
    }

    override fun compareTo(other: Way<out MapTag>): Int {
        Validate.notNull(other)
        return this.identifier.compareTo(other.identifier)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Way<*>) return false
        return identifier == other.identifier && nodes.contentEquals(other.nodes) && tags == other.tags
    }

    override fun hashCode(): Int {
        return Objects.hash(identifier)
    }

    /**
     * Converts this [Way] to a `String` in the `Json` object format.
     *
     * @return The node as Json String.
     */
    @Suppress("unused") // Part of the API
    fun toJson(): String {
        val stringBuilder = StringBuilder(String.format(Locale.getDefault(), "{\"id\":%d,", identifier))

        stringBuilder.append("\"nodes\":[")
        for (node in nodes) {
            stringBuilder.append(node.toJson())
        }
        stringBuilder.append("],")

        stringBuilder.append("\"tags\":{")
        for ((key, value) in tags) {
            stringBuilder.append("\"$key\":$value")
        }
        stringBuilder.append("}}")

        return stringBuilder.toString()
    }
}
