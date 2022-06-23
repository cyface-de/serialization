/*
 * Copyright 2019-2022 Cyface GmbH
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
package de.cyface.model.osm;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;

/**
 * A POJO representing an Open Street Map way.
 * <p>
 * Objects of this class are supposed to be handled as immutable. This is not possible to enforce, since Apache Flink
 * requires public setters and a no argument constructor. But be advised, that mutating any properties is probably a
 * code smell.
 * <p>
 * Two ways are comparable. Their natural ordering is from lowest <code>identifier</code> to highest
 * <code>identifier</code>.
 *
 * @author Klemens Muthmann
 * @version 1.0.0
 * @since 2.2.0
 */
public final class Way<T extends MapTag> implements Comparable<Way<? extends MapTag>> {

    /**
     * The OSM identifier of this way.
     */
    private long identifier;
    /**
     * The OSM nodes constituting this way.
     */
    private Node[] nodes;
    /**
     * A <code>Collection</code> of all the tags associated with this Open Street Map way.
     */
    private Map<String, T> tags;

    /**
     * A no argument constructor as required by Apache flink
     */
    @SuppressWarnings("unused") // Part of the API
    public Way() {
        // Nothing to do here.
    }

    /**
     * Creates a new completely initialized instance of this class.
     *
     * @param identifier The Open Street Map identifier of this way
     * @param nodes An array with nodes forming the way, in order of their occurrence
     * @param tags The tags associated with the way. This might be an empty <code>Collection</code>
     */
    @SuppressWarnings("unused") // Part of the API
    public Way(final long identifier, final Node[] nodes, final Collection<T> tags) {
        Validate.isTrue(identifier > 0L);
        Validate.notNull(nodes);

        this.identifier = identifier;
        this.nodes = Arrays.copyOf(nodes, nodes.length);
        setTags(tags);
    }

    /**
     * @return The OSM identifier of this way.
     */
    public long getIdentifier() {
        return identifier;
    }

    /**
     * @param identifier The OSM identifier of this way.
     */
    public void setIdentifier(final long identifier) {
        this.identifier = identifier;
    }

    /**
     * @return The OSM nodes constituting this way.
     */
    @SuppressWarnings("unused") // Part of the API
    public Node[] getNodes() {
        return Arrays.copyOf(nodes, nodes.length);
    }

    /**
     * @param nodes The OSM nodes constituting this way.
     */
    @SuppressWarnings("unused") // Part of the API
    public void setNodes(final Node[] nodes) {
        this.nodes = Arrays.copyOf(nodes, nodes.length);
    }

    /**
     * @return The tags associated with the way. This might be an empty <code>Collection</code>
     */
    public Collection<T> getTags() {
        return tags.values();
    }

    /**
     * Returns the Open Street Map tag associated with the provided key.
     *
     * @param key The key to look up
     * @return An <code>Optional</code> with the {@link T} if present; an empty <code>Optional</code> otherwise.
     */
    @SuppressWarnings("unused") // Part of the API
    public Optional<T> getTag(final String key) {
        return Optional.ofNullable(tags.get(key));
    }

    /**
     * @param tags The tags associated with the way. This might be an empty <code>Collection</code>
     */
    public void setTags(final Collection<T> tags) {
        Validate.notNull(tags);

        this.tags = tags.stream().collect(Collectors.toMap(T::getKey, Function.identity()));
    }

    @Override
    public String toString() {
        return "Way{" +
                "identifier=" + identifier +
                ", nodes=" + Arrays.toString(nodes) +
                ", tags=" + Arrays.toString(getTags().toArray()) +
                '}';
    }

    @Override
    public int compareTo(final Way<? extends MapTag> way) {
        Validate.notNull(way);

        return Long.compare(this.getIdentifier(), way.getIdentifier());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Way<?> way = (Way<?>)o;
        return identifier == way.identifier &&
                Arrays.equals(nodes, way.nodes) &&
                Objects.equals(tags, way.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier);
    }

    @SuppressWarnings("unused") // Part of the API
    public String toJson() {
        final var stringBuilder = new StringBuilder(String.format("{\"id\":%d,", getIdentifier()));

        stringBuilder.append("\"nodes\":[");
        for (final Node node : nodes) {
            node.toJson();
        }
        stringBuilder.append("],");

        stringBuilder.append("\"tags\":{");
        for (var tag : tags.entrySet()) {
            stringBuilder.append(String.format("\"%s\":%s", tag.getKey(), tag.getValue().toString()));
        }
        stringBuilder.append("}");

        return stringBuilder.toString();
    }
}
