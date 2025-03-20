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
package de.cyface.model.v3_0_0;

import static de.cyface.model.Json.jsonArray;
import static de.cyface.model.Json.jsonKeyValue;
import static de.cyface.model.Json.jsonObject;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import de.cyface.model.DataType;
import de.cyface.model.ExportOptions;
import de.cyface.model.Json;
import de.cyface.model.Modality;
import de.cyface.model.Point3DImpl;
import de.cyface.model.RawRecord;
import de.cyface.model.TimestampNotFound;
import de.cyface.model.Track;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A single measurement captured by a Cyface measurement device.
 * <p>
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
public class Measurement implements Serializable {

    /**
     * The logger used by objects of this class. Configure it using <tt>src/main/resources/logback.xml</tt>.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Measurement.class);
    /**
     * Used to serialize objects of this class. Only change this value if this classes attribute set changes.
     */
    private static final long serialVersionUID = 4195718001652533383L;
    /**
     * The context of this {@code Measurement}.
     */
    private MetaData metaData;
    /**
     * The data collected for this {@code Measurement} in {@code Track}-slices, ordered by timestamp.
     */
    private List<Track> tracks;

    /**
     * Creates a new uninitialized {@code Measurement}. This is only necessary for Flink serialisation and should never
     * be called from your own code.
     */
    public Measurement() {
        this.tracks = Collections.emptyList();
        // Nothing to do here.
    }

    /**
     * Creates a new completely initialized {@code Measurement}.
     *
     * @param metaData The context of this {@code Measurement}.
     * @param tracks The data collected for this {@code Measurement} in {@code Track}-slices, ordered by timestamp.
     */
    public Measurement(final MetaData metaData, final List<Track> tracks) {
        Validate.notNull(metaData);

        this.metaData = metaData;
        this.tracks = new ArrayList<>(tracks);
    }

    public Measurement(final List<TrackBucket> buckets) {
        if (buckets.isEmpty()) {
            throw new IllegalArgumentException("Cannot create a measurement from 0 buckets!");
        }
        final var metaData = buckets.get(0).getMetaData();
        Validate.notNull(metaData);

        this.metaData = metaData;
        final var tracks = tracks(buckets);
        this.tracks = new ArrayList<>(tracks);
    }

    /**
     * Merges {@link TrackBucket}s into {@link Track}s.
     *
     * @param trackBuckets the data to merge
     * @return the tracks
     */
    private List<Track> tracks(final List<TrackBucket> trackBuckets) {

        // Group by trackId
        final var groupedBuckets = trackBuckets.stream()
                .collect(groupingBy(TrackBucket::getTrackId));

        // Sort bucket groups by trackId
        final var sortedBucketGroups = groupedBuckets.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue,
                        LinkedHashMap::new));

        // Convert buckets to Track
        final var tracks = new ArrayList<Track>();
        sortedBucketGroups.forEach((trackId, bucketGroup) -> {
            // Sort buckets
            final var sortedBuckets = bucketGroup.stream()
                    .sorted(Comparator.comparing(TrackBucket::getBucket))
                    .collect(toList());

            // Merge buckets
            final var locations = sortedBuckets.stream()
                    .flatMap(bucket -> bucket.getTrack().getLocationRecords().stream())
                    .collect(toList());
            final var accelerations = sortedBuckets.stream()
                    .flatMap(bucket -> bucket.getTrack().getAccelerations().stream())
                    .collect(toList());
            final var rotations = sortedBuckets.stream()
                    .flatMap(bucket -> bucket.getTrack().getRotations().stream())
                    .collect(toList());
            final var directions = sortedBuckets.stream()
                    .flatMap(bucket -> bucket.getTrack().getDirections().stream())
                    .collect(toList());

            final var track = new Track(locations, accelerations, rotations, directions);
            tracks.add(track);
        });
        return tracks;
    }

    /**
     * @return The context of this {@code Measurement}.
     */
    public MetaData getMetaData() {
        return metaData;
    }

    /**
     * @return The data collected for this {@code Measurement} in {@code Track}-slices, ordered by timestamp.
     */
    public List<Track> getTracks() {
        return List.copyOf(tracks);
    }

    /**
     * Required by Apache Flink.
     *
     * @param metaData The context of this {@code Measurement}.
     */
    @SuppressWarnings("unused") // Required by Apache Flink.
    public void setMetaData(final MetaData metaData) {
        this.metaData = metaData;
    }

    /**
     * Required by Apache Flink.
     *
     * @param tracks The data collected for this {@code Measurement} in {@code Track}-slices, ordered by timestamp.
     */
    @SuppressWarnings("unused") // Required by Apache Flink.
    public void setTracks(final List<Track> tracks) {
        this.tracks = new ArrayList<>(tracks);
    }

    /**
     * Exports this measurement as a CSV file.
     *
     * @param options The options which describe which data should be exported.
     * @param handler A handler that gets one line of CSV output per call
     */
    @SuppressWarnings("unused") // API used by backend/executables/cyface-to-csv
    public void asCsv(final ExportOptions options, final Consumer<String> handler) {
        asCsv(options, null, handler);
    }

    /**
     * Exports this measurement as a CSV file.
     *
     * @param options The options which describe which data should be exported.
     * @param username The name of the user who uploaded the data or {@code null} to not export this field
     * @param handler A handler that gets one line of CSV output per call
     */
    public void asCsv(final ExportOptions options, final String username, final Consumer<String> handler) {
        Validate.isTrue(!options.getIncludeUsername() || username != null);

        if (options.getIncludeHeader()) {
            csvHeader(options, handler);
        }

        switch (options.getType()) {
            case LOCATION:
                locationDataAsCsv(options, username, handler);
                break;
            case ACCELERATION:
            case ROTATION:
            case DIRECTION:
                sensorDataAsCsv(options, username, handler);
                break;
            default:
                throw new IllegalArgumentException(String.format("Unsupported type: %s", options.getType()));
        }
    }

    private void locationDataAsCsv(ExportOptions options, String username, Consumer<String> handler) {
        Modality lastModality = Modality.UNKNOWN;

        // Iterate through tracks
        var modalityTypeDistance = 0.0;
        var totalDistance = 0.0;
        var modalityTypeTravelTime = 0L;
        var totalTravelTime = 0L;
        for (var trackId = 0; trackId < tracks.size(); trackId++) {
            final var track = tracks.get(trackId);

            // Iterate through locations
            RawRecord lastLocation = null;
            for (final var locationRecord : track.getLocationRecords()) {
                if (lastLocation != null) {
                    final var newDistance = lastLocation.distanceTo(locationRecord) / 1000.;
                    modalityTypeDistance += newDistance;
                    totalDistance += newDistance;
                    final var timeTraveled = locationRecord.getTimestamp()
                            - lastLocation.getTimestamp();
                    modalityTypeTravelTime += timeTraveled;
                    totalTravelTime += timeTraveled;
                }

                // Check if the modalityType changed
                if (locationRecord.getModality() != null && locationRecord.getModality() != lastModality) {
                    lastModality = locationRecord.getModality();
                    modalityTypeDistance = 0.0;
                    modalityTypeTravelTime = 0L;
                }

                handler.accept(csvRow(options, username, getMetaData(), locationRecord, trackId,
                        modalityTypeDistance, totalDistance,
                        modalityTypeTravelTime, totalTravelTime));
                handler.accept("\r\n");

                lastLocation = locationRecord;
            }
        }
    }

    private void sensorDataAsCsv(ExportOptions options, String username, Consumer<String> handler) {
        // Iterate through tracks
        for (var trackId = 0; trackId < tracks.size(); trackId++) {
            final var track = tracks.get(trackId);

            // Iterate through sensor points
            final var points = options.getType().equals(DataType.ACCELERATION) ? track.getAccelerations()
                    : options.getType().equals(DataType.ROTATION) ? track.getRotations()
                    : options.getType().equals(DataType.DIRECTION) ? track.getDirections()
                    : null;
            Validate.notNull(points, "Unsupported type: " + options.getType());
            for (final var point : points) {
                handler.accept(csvSensorRow(options, username, getMetaData(), point, trackId));
                handler.accept("\r\n");
            }
        }
    }

    /**
     * Exports this measurement as GeoJSON feature.
     *
     * @param handler A handler that gets the GeoJson feature as string
     */
    public void asGeoJson(final Consumer<String> handler) {
        // We decided to generate a String instead of using a JSON library to avoid dependencies in the model library

        // measurement = geoJson "feature"
        handler.accept("{");
        handler.accept(jsonKeyValue("type", "Feature").getStringValue());
        handler.accept(",");

        // All tracks = geometry (MultiLineString)
        handler.accept("\"geometry\":{");
        handler.accept(jsonKeyValue("type", "MultiLineString").getStringValue());
        handler.accept(",");
        handler.accept("\"coordinates\":");
        final var tracksCoordinates = convertToLineStringCoordinates(getTracks());
        handler.accept(tracksCoordinates);
        handler.accept("},");

        final var deviceId = jsonKeyValue("deviceId", getMetaData().getIdentifier().getDeviceIdentifier());
        final var measurementId = jsonKeyValue("measurementId",
                getMetaData().getIdentifier().getMeasurementIdentifier());
        final var length = jsonKeyValue("length", getMetaData().getLength());
        final var properties = jsonObject(deviceId, measurementId, length);
        handler.accept(jsonKeyValue("properties", properties).getStringValue());

        handler.accept("}");
    }

    /**
     * Exports this measurement as Json <b>without sensor data</b>.
     *
     * @param handler A handler that gets the Json as string
     */
    public void asJson(final Consumer<String> handler) {
        asJson(null, handler);
    }

    /**
     * Exports this measurement as Json <b>without sensor data</b>.
     *
     * @param username The name of the user who uploaded the data or {@code null} to omit this field
     * @param handler A handler that gets the Json as string
     */
    public void asJson(final String username, final Consumer<String> handler) {
        // We decided to generate a String instead of using a JSON library to avoid dependencies in the model library
        handler.accept("{");

        handler.accept(jsonKeyValue("metaData", asJson(username, metaData)).getStringValue());
        handler.accept(",");

        handler.accept("\"tracks\":[");
        for (int i = 0; i < tracks.size(); i++) {
            final var track = tracks.get(i);
            handler.accept(featureCollection(track).getStringValue());
            if (i != tracks.size() - 1) {
                handler.accept(",");
            }
        }
        handler.accept("]");

        handler.accept("}");
    }

    private Json.JsonObject asJson(final String username, final MetaData metaData) {
        List<Json.KeyValuePair> jsonElements = new ArrayList<>();
        jsonElements.add(jsonKeyValue("userId", metaData.getUserId().toString()));
        if (username != null) {
            jsonElements.add(jsonKeyValue("username", username));
        }
        jsonElements.add(jsonKeyValue("deviceId", metaData.getIdentifier().getDeviceIdentifier()));
        jsonElements.add(jsonKeyValue("measurementId", metaData.getIdentifier().getMeasurementIdentifier()));
        jsonElements.add(jsonKeyValue("length", metaData.getLength()));
        return jsonObject(jsonElements.toArray(new Json.KeyValuePair[0]));
    }

    /**
     * Converts a {@link Track} to a {@code GeoJson} "FeatureCollection" with "Point" "Features".
     *
     * @param track the {@code Track} to convert
     * @return the converted {@code Track}
     */
    private Json.JsonObject featureCollection(final Track track) {
        final var points = geoJsonPointFeatures(track.getLocationRecords());
        final var type = jsonKeyValue("type", "FeatureCollection");
        final var features = jsonKeyValue("features", points);
        return jsonObject(type, features);
    }

    private Json.JsonArray geoJsonPointFeatures(final List<RawRecord> list) {
        return jsonArray(list.stream().map(l -> geoJsonPointFeature(l).getStringValue()).toArray(String[]::new));
    }

    private Json.JsonObject geoJsonPointFeature(final RawRecord record) {
        final var type = jsonKeyValue("type", "Feature");

        final var geometryType = jsonKeyValue("type", "Point");
        final var lat = String.valueOf(record.getLatitude());
        final var lon = String.valueOf(record.getLongitude());
        final var coordinates = jsonKeyValue("coordinates", jsonArray(lon, lat));
        final var geometry = jsonKeyValue("geometry", jsonObject(geometryType, coordinates));

        final var timestamp = jsonKeyValue("timestamp", record.getTimestamp());
        final var speed = jsonKeyValue("speed", record.getSpeed());
        final var accuracy = jsonKeyValue("accuracy", record.getAccuracy());
        final var modality = jsonKeyValue("modality", record.getModality().getDatabaseIdentifier());
        final var properties = jsonKeyValue("properties", jsonObject(timestamp, speed, accuracy, modality));

        return jsonObject(type, geometry, properties);
    }

    /**
     * Clears the data within this measurement starting at the provided <code>timestamp</code> in milliseconds since the
     * 01.01.1970 (UNIX Epoch).
     * <p>
     * This call modifies the called measurement.
     *
     * @param timestamp The timestamp in milliseconds since the first of January 1970 to begin clearing the data at
     * @return This cleared <code>Measurement</code>
     * @throws TimestampNotFound If the timestamp is not within the timeframe of this measurement
     */
    @SuppressWarnings("unused") // Part of the API
    public Measurement clearAfter(final long timestamp) throws TimestampNotFound {
        final var trackIndex = getIndexOfTrackContaining(timestamp);
        while (tracks.size() - 1 > trackIndex) {
            tracks.remove(tracks.size() - 1);
        }
        tracks.get(trackIndex).clearAfter(timestamp);
        return this;
    }

    /**
     * Tries to find the track from this measurement containing the provided timestamp.
     *
     * @param timestamp A timestamp in milliseconds since the first of January 1970
     * @return The index of the track containing the provided timestamp
     * @throws TimestampNotFound If the timestamp is not within the timeframe of this measurement
     */
    private int getIndexOfTrackContaining(final long timestamp) throws TimestampNotFound {
        LOGGER.trace("Getting track index for timestamp: {} ({})!",
                SimpleDateFormat.getDateTimeInstance().format(new Date(timestamp)), timestamp);
        for (var i = 0; i < tracks.size(); i++) {
            var track = tracks.get(i);
            var minRotationsTimestamp = track.getRotations().isEmpty() ? Long.MAX_VALUE
                    : track.getRotations().get(0).getTimestamp();
            var minDirectionsTimestamp = track.getDirections().isEmpty() ? Long.MAX_VALUE
                    : track.getDirections().get(0).getTimestamp();
            var minAccelerationsTimestamp = track.getAccelerations().isEmpty() ? Long.MAX_VALUE
                    : track.getAccelerations().get(0).getTimestamp();
            var minLocationsTimestamp = track.getLocationRecords().isEmpty() ? Long.MAX_VALUE
                    : track.getLocationRecords().get(0).getTimestamp();
            var minTrackTimestamp = Math.min(minRotationsTimestamp,
                    Math.min(minDirectionsTimestamp, Math.min(minAccelerationsTimestamp, minLocationsTimestamp)));
            Validate.isTrue(minTrackTimestamp < Long.MAX_VALUE);

            var maxRotationsTimestamp = track.getRotations().isEmpty() ? Long.MIN_VALUE
                    : track.getRotations().get(track.getRotations().size() - 1).getTimestamp();
            var maxDirectionsTimestamp = track.getDirections().isEmpty() ? Long.MIN_VALUE
                    : track.getDirections().get(track.getDirections().size() - 1).getTimestamp();
            var maxAccelerationsTimestamp = track.getAccelerations().isEmpty() ? Long.MIN_VALUE
                    : track.getAccelerations().get(track.getAccelerations().size() - 1).getTimestamp();
            var maxLocationsTimestamp = track.getLocationRecords().isEmpty() ? Long.MIN_VALUE
                    : track.getLocationRecords().get(track.getLocationRecords().size() - 1).getTimestamp();
            var maxTrackTimestamp = Math.max(maxRotationsTimestamp,
                    Math.max(maxDirectionsTimestamp, Math.max(maxAccelerationsTimestamp, maxLocationsTimestamp)));
            Validate.isTrue(maxTrackTimestamp > Long.MIN_VALUE);

            LOGGER.trace("Min timestamp for index {} is {} ({}).", i,
                    SimpleDateFormat.getDateTimeInstance().format(new Date(minTrackTimestamp)), minTrackTimestamp);
            LOGGER.trace("Max timestamp for index {} is {} ({}).", i,
                    SimpleDateFormat.getDateTimeInstance().format(new Date(maxTrackTimestamp)), maxTrackTimestamp);
            if (timestamp >= minTrackTimestamp && timestamp <= maxTrackTimestamp) {
                LOGGER.trace("Selected index {}.", i);
                return i;
            }
        }
        throw new TimestampNotFound(String.format("Unable to find track index for timestamp %s (%s) in measurement %s!",
                SimpleDateFormat.getDateTimeInstance().format(new Date(timestamp)), timestamp,
                getMetaData()));
    }

    /**
     * Creates a CSV header for this measurement.
     *
     * @param options The options describing which data is exported
     * @param handler The handler that is notified of the new CSV row.
     */
    public static void csvHeader(final ExportOptions options, final Consumer<String> handler) {

        final var elements = new ArrayList<String>();
        if (options.getIncludeUserId()) {
            elements.add("userId");
        }
        if (options.getIncludeUsername()) {
            elements.add("username");
        }
        elements.addAll(List.of("deviceId", "measurementId", "trackId", "timestamp [ms]"));
        switch (options.getType()) {
            case LOCATION:
                elements.addAll(List.of("latitude", "longitude",
                        "speed [m/s]", "accuracy [m]", "modalityType", "modalityTypeDistance [m]", "distance [m]",
                        "modalityTypeTravelTime [ms]", "travelTime [ms]"));
                break;
            case ACCELERATION:
                elements.addAll(List.of("x [m/s^2]", "y [m/s^2]", "z [m/s^2]"));
                break;
            case ROTATION:
                elements.addAll(List.of("x [rad/s]", "y [rad/s]", "z [rad/s]"));
                break;
            case DIRECTION:
                elements.addAll(List.of("x [uT]", "y [uT]", "z [uT]"));
                break;
            default:
                throw new IllegalArgumentException(String.format("Unsupported type: %s", options.getType()));
        }

        final var csvHeaderRow = String.join(",", elements);
        handler.accept(csvHeaderRow);
        handler.accept("\r\n");
    }

    /**
     * Converts one location entry annotated with metadata to a CSV row.
     *
     * @param options The options which describe which data should be exported.
     * @param username the name of the user who uploaded the data or {@code null} to not annotate a username
     * @param metaData the {@code Measurement} of the {@param location}
     * @param locationRecord the {@code GeoLocationRecord} to be processed
     * @param trackId the id of the sub track starting at 1
     * @param modalityTypeDistance the distance traveled so far with this {@param modality} type
     * @param totalDistance the total distance traveled so far
     * @param modalityTypeTravelTime the time traveled so far with this {@param modality} type
     * @param totalTravelTime the time traveled so far
     * @return the csv row as String
     */
    private String csvRow(ExportOptions options, final String username, final MetaData metaData,
                          final RawRecord locationRecord,
                          final int trackId, final double modalityTypeDistance, final double totalDistance,
                          final long modalityTypeTravelTime, final long totalTravelTime) {

        final var userId = metaData.getUserId();
        final var deviceId = metaData.getIdentifier().getDeviceIdentifier();
        final var measurementId = String.valueOf(metaData.getIdentifier().getMeasurementIdentifier());

        final var elements = new ArrayList<String>();
        if (options.getIncludeUserId()) {
            elements.add(userId.toString());
        }
        if (options.getIncludeUsername()) {
            Validate.notNull(username);
            elements.add(username);
        }
        elements.addAll(List.of(deviceId, measurementId, String.valueOf(trackId),
                String.valueOf(locationRecord.getTimestamp()),
                String.valueOf(locationRecord.getLatitude()),
                String.valueOf(locationRecord.getLongitude()), String.valueOf(locationRecord.getSpeed()),
                String.valueOf(locationRecord.getAccuracy()),
                locationRecord.getModality().getDatabaseIdentifier(),
                String.valueOf(modalityTypeDistance), String.valueOf(totalDistance),
                String.valueOf(modalityTypeTravelTime), String.valueOf(totalTravelTime)));
        return String.join(",", elements);
    }

    private String csvSensorRow(ExportOptions options, final String username, final MetaData metaData,
                                final Point3DImpl pointRecord,
                                final int trackId) {

        final var userId = metaData.getUserId();
        final var deviceId = metaData.getIdentifier().getDeviceIdentifier();
        final var measurementId = String.valueOf(metaData.getIdentifier().getMeasurementIdentifier());

        final var elements = new ArrayList<String>();
        if (options.getIncludeUserId()) {
            elements.add(userId.toString());
        }
        if (options.getIncludeUsername()) {
            Validate.notNull(username);
            elements.add(username);
        }
        elements.addAll(List.of(deviceId, measurementId, String.valueOf(trackId),
                String.valueOf(pointRecord.getTimestamp()),
                String.valueOf(pointRecord.getX()),
                String.valueOf(pointRecord.getY()),
                String.valueOf(pointRecord.getZ())));
        return String.join(",", elements);
    }

    /**
     * Converts a single track to geoJson "coordinates".
     *
     * @param tracks the {@code Track}s to be processed
     * @return the string representation of the geoJson coordinates
     */
    private String convertToLineStringCoordinates(final List<Track> tracks) {
        final var builder = new StringBuilder("[");

        // Each track is a LineString
        tracks.forEach(track -> {
            final var points = jsonArray(
                    track.getLocationRecords().stream().map(l -> geoJsonCoordinates(l).getStringValue())
                            .toArray(String[]::new));
            builder.append(points.getStringValue());
            builder.append(",");
        });
        builder.deleteCharAt(builder.length() - 1); // delete last ","
        builder.append("]");

        return builder.toString();
    }

    private Json.JsonArray geoJsonCoordinates(final RawRecord record) {
        return jsonArray(String.valueOf(record.getLongitude()), String.valueOf(record.getLatitude()));
    }

    @Override
    public String toString() {
        return "Measurement{" +
                "metaData=" + metaData +
                ", tracks=" + tracks +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        final var that = (Measurement)o;
        return metaData.equals(that.metaData) &&
                tracks.equals(that.tracks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metaData, tracks);
    }
}