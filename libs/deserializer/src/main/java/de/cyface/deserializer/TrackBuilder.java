/*
 * Copyright 2020-2021 Cyface GmbH
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
package de.cyface.deserializer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

import de.cyface.deserializer.exceptions.InvalidLifecycleEvents;
import de.cyface.model.Point3DImpl;
import org.apache.commons.lang3.Validate;

import de.cyface.model.DataPoint;
import de.cyface.model.Event;
import de.cyface.model.GeoLocation;
import de.cyface.model.MeasurementIdentifier;
import de.cyface.model.Modality;
import de.cyface.model.RawRecord;
import de.cyface.model.Track;

/**
 * Builds {@code Track}s from cyface measurement data.
 * <p>
 * This allows to analyse and visualize measurements without the need to handle {@code Event}s:
 * - {@code GeoLocation}s are collected into {@code Track}s and can be visualized by simply connecting all locations
 * - {@code GeoLocation}s are annotated with the {@code Modality} used to collect the location
 * - {@code Point3D} data is collected into {@code Track}s and can be analyzed together with the location data
 *
 * @author Armin Schnabel
 * @author Klemens Muthmann
 */
final class TrackBuilder {
    /**
     * Extracts the tracks from a measurement using its LIFECYCLE Events.
     *
     * @param locationRecords of the {@code Measurement} to export the tracks for
     * @param events the {@code Event}s used to slice the tracks
     * @param accelerations of the {@code Measurement}
     * @param rotations of the {@code Measurement}
     * @param directions of the {@code Measurement}
     * @return a list of {@link Track}s.
     * @throws InvalidLifecycleEvents when the sequence of {@code de.cyface.model.Event}s is invalid.
     */
    List<Track> build(final List<RawRecord> locationRecords, final List<Event> events,
            final List<Point3DImpl> accelerations, final List<Point3DImpl> rotations, final List<Point3DImpl> directions)
            throws InvalidLifecycleEvents {
        final Iterator<Event> pauseResumeEventsIterator = events
                .parallelStream()
                .filter(event -> event.getType()
                        .equals(Event.EventType.LIFECYCLE_PAUSE)
                        || event.getType()
                                .equals(Event.EventType.LIFECYCLE_RESUME))
                .iterator();
        final Iterator<Event> modalityTypeChangesIterator = events
                .parallelStream()
                .filter(event -> event.getType()
                        .equals(Event.EventType.MODALITY_TYPE_CHANGE))
                .iterator();
        final ListIterator<RawRecord> geoLocationIterator = locationRecords.listIterator();
        final var accelerationsIterator = accelerations.listIterator();
        final var rotationsIterator = rotations.listIterator();
        final var directionsIterator = directions.listIterator();
        final List<Track> tracks = loadTracks(geoLocationIterator, pauseResumeEventsIterator, accelerationsIterator,
                rotationsIterator, directionsIterator);
        return annotateModalityTypes(tracks, modalityTypeChangesIterator);
    }

    List<Track> build(final List<de.cyface.serializer.GeoLocation> locations, final List<Event> events,
            final List<Point3DImpl> accelerations, final List<Point3DImpl> rotations, final List<Point3DImpl> directions,
            final MeasurementIdentifier identifier) throws InvalidLifecycleEvents {

        final var rawRecords = locations.stream().map(l -> new RawRecord(identifier, l.getTimestamp(), l.getLat(),
                l.getLon(), l.getAccuracy(), l.getSpeed(), null)).collect(Collectors.toList());

        return build(rawRecords, events, accelerations, rotations, directions);
    }

    /**
     * Sets the {@link Modality} types for all locations of a measurement's track list.
     *
     * @param tracks The ordered tracks of a measurement to be annotated.
     * @param modalityTypeChangesIterator The {@code Event} cursor pointing to the events of modality type changes.
     * @return the annotated tracks
     */
    private List<Track> annotateModalityTypes(final List<Track> tracks,
            final Iterator<Event> modalityTypeChangesIterator) {

        // ModalityType annotation - UNKNOWN if all ModalityTypeChanges were deleted by the user
        final Modality[] modalityType = {modalityTypeChangesIterator.hasNext()
                ? Modality.valueOf(modalityTypeChangesIterator.next().getValue())
                : Modality.UNKNOWN};
        final Event[] nextModalityTypeChange = {
                modalityTypeChangesIterator.hasNext() ? modalityTypeChangesIterator.next()
                        : null};

        tracks.forEach(track -> track.getLocationRecords().forEach(location -> {
            // Check if the modalityType changed
            if (nextModalityTypeChange[0] != null
                    && location.getTimestamp() >= nextModalityTypeChange[0].getTimestamp()) {
                final String modalityValue = nextModalityTypeChange[0].getValue();
                Validate.notNull(modalityValue, "Value in ModalityTypeChange event is null");
                Validate.notEmpty(modalityValue, "Empty value in ModalityTypeChange event");
                modalityType[0] = Modality.valueOf(modalityValue);
                Validate.notNull(modalityType[0], "Modality is null");
                nextModalityTypeChange[0] = modalityTypeChangesIterator.hasNext()
                        ? modalityTypeChangesIterator.next()
                        : null;
            }

            // Annotate location
            location.setModality(modalityType[0]);
        }));

        return tracks;
    }

    /**
     * Loads the {@link Track}s for the provided {@link GeoLocation} cursor sliced using the provided
     * {@link Event} cursor.
     * <p>
     * From Android SDK 5.0.0-beta1's PersistenceLayer.loadTracks(Cursor, Cursor) implementation.
     *
     * @param locationIterator The {@code GeoLocation} cursor which points to the locations to be loaded.
     * @param eventIterator The {@code Event} cursor pointing to the events of the track to be loaded.
     * @param accelerationsIterator The cursor pointing to the accelerations to be loaded into the track
     * @param rotationsIterator The cursor pointing to the rotations to be loaded into the track
     * @param directionsIterator The cursor pointing to the directions to be loaded into the track
     * @return The {@link Track}s for the corresponding {@code Measurement} loaded.
     * @throws InvalidLifecycleEvents when the sequence of {@code de.cyface.model.Event}s is invalid.
     */
    private List<Track> loadTracks(final ListIterator<RawRecord> locationIterator,
            final Iterator<Event> eventIterator, final ListIterator<Point3DImpl> accelerationsIterator,
            final ListIterator<Point3DImpl> rotationsIterator, final ListIterator<Point3DImpl> directionsIterator)
            throws InvalidLifecycleEvents {
        final List<Track> tracks = new ArrayList<>();

        // Slice Tracks before resume events
        Long pauseEventTime = null;
        // `geoLocationIterator.next()` always points to the first GeoLocation of the next sub track or else to null
        while (eventIterator.hasNext() && locationIterator.hasNext()) {
            final Event event = eventIterator.next();

            // Search for next resume event and capture it's previous pause event
            if (event.getType() != Event.EventType.LIFECYCLE_RESUME) {
                if (event.getType() == Event.EventType.LIFECYCLE_PAUSE) {
                    pauseEventTime = event.getTimestamp();
                }
                continue;
            }
            if (pauseEventTime == null) {
                throw new InvalidLifecycleEvents("Resume event without prior pause event collected.");
            }
            final long resumeEventTime = event.getTimestamp();

            // Collect all GeoLocations until the pause event.
            // The geoLocationIterator then points to the next location after the pause event or to null if finished
            final Track track = collectNextSubTrack(locationIterator, pauseEventTime, accelerationsIterator,
                    rotationsIterator, directionsIterator);
            // Add sub-track to track
            if (track.getLocationRecords().size() > 0) {
                tracks.add(track);
            }

            // The iterator's next points to the second point of the next track
            // If there is no second point we won't create a track out of one point so we can skip this
            if (locationIterator.hasNext()) {
                // Pause reached: Move geoLocationIterator to the first location of the next sub-track
                // We do this to ignore locations between pause and resume event (STAD-140)
                moveIteratorToLastBefore(locationIterator, resumeEventTime);
                moveIteratorToLastBefore(accelerationsIterator, resumeEventTime);
                moveIteratorToLastBefore(rotationsIterator, resumeEventTime);
                moveIteratorToLastBefore(directionsIterator, resumeEventTime);
            }
        }

        // Collect tail sub track
        // This is either the track between start[, pause] and stop or resume[, pause] and stop.
        final Track tail = collectTail(locationIterator, accelerationsIterator, rotationsIterator,
                directionsIterator);
        if (tail.getLocationRecords().size() > 0) {
            tracks.add(tail);
        }
        return tracks;
    }

    /**
     * Collects all remaining entries.
     *
     * @param geoLocationIterator The {@code GeoLocation} cursor which points to the locations to be loaded.
     * @param accelerationsIterator The cursor pointing to the accelerations to be loaded into the track
     * @param rotationsIterator The cursor pointing to the rotations to be loaded into the track
     * @param directionsIterator The cursor pointing to the directions to be loaded into the track
     * @return the track with the remaining entries
     */
    private Track collectTail(final ListIterator<RawRecord> geoLocationIterator,
            final ListIterator<Point3DImpl> accelerationsIterator, final ListIterator<Point3DImpl> rotationsIterator,
            final ListIterator<Point3DImpl> directionsIterator) {
        final var locations = new ArrayList<RawRecord>();
        while (geoLocationIterator.hasNext()) {
            locations.add(geoLocationIterator.next());
        }
        final var accelerations = new ArrayList<Point3DImpl>();
        while (accelerationsIterator.hasNext()) {
            accelerations.add(accelerationsIterator.next());
        }
        final var rotations = new ArrayList<Point3DImpl>();
        while (rotationsIterator.hasNext()) {
            rotations.add(rotationsIterator.next());
        }
        final var directions = new ArrayList<Point3DImpl>();
        while (directionsIterator.hasNext()) {
            directions.add(directionsIterator.next());
        }
        return new Track(locations, accelerations, rotations, directions);
    }

    /**
     * Collects a sub {@link Track} of a {@code Measurement}.
     *
     * @param geoLocationIterator The {@code Iterator} of which the {@code next()} method points to the
     *            first {@code GeoLocation} of the sub track to be collected.
     * @param pauseEventTime the Unix timestamp of the {@link Event.EventType#LIFECYCLE_PAUSE} which defines the end of
     *            this sub Track.
     * @param accelerationsIterator The {@code Iterator} of which the {@code next()} method points to the first
     *            acceleration of the sub track to be collected.
     * @param rotationsIterator The {@code Iterator} of which the {@code next()} method points to the first
     *            rotation of the sub track to be collected.
     * @param directionsIterator The {@code Iterator} of which the {@code next()} method points to the first
     *            direction of the sub track to be collected.
     * @return The sub {@code Track}. The {@code geoLocationIterator#next()} now points to the first
     *         {@code GeoLocation} which is later in time than the {@code pauseEventTime} or {@code null} if the
     *         earlier does not exist. The same is the case for the {@code Point3D} iterators supplied.
     */
    private Track collectNextSubTrack(final ListIterator<RawRecord> geoLocationIterator,
            final Long pauseEventTime, final ListIterator<Point3DImpl> accelerationsIterator,
            final ListIterator<Point3DImpl> rotationsIterator,
            final ListIterator<Point3DImpl> directionsIterator) {

        final var locations = collectUntil(pauseEventTime, geoLocationIterator);
        final var accelerations = collectUntil(pauseEventTime, accelerationsIterator);
        final var rotations = collectUntil(pauseEventTime, rotationsIterator);
        final var directions = collectUntil(pauseEventTime, directionsIterator);

        return new Track(locations, accelerations, rotations, directions);
    }

    /**
     * Iterates through a given iterator and collects all entries until the iterator points to an entry which was
     * captured after a given timestamp.
     *
     * @param <T> the Type of the entries to iterator over.
     * @param iterator of which the {@code next()} method points to the first entry to be collected.
     * @param pauseEventTime the Unix timestamp which defines the barrier until which entries are collected.
     * @return the list of entries collected
     */
    private <T extends DataPoint> List<T> collectUntil(final Long pauseEventTime,
            final ListIterator<T> iterator) {
        List<T> collection = new ArrayList<>();
        T entry = iterator.next();
        while (entry != null && entry.getTimestamp() <= pauseEventTime) {
            collection.add(entry);

            // Load next entry to check its timestamp in next iteration
            entry = iterator.hasNext() ? iterator.next() : null;
        }
        return collection;
    }

    /**
     * Moves the {@code iterator} to the last {@code DataPoint} before {@code resumeEventTime}.
     * <p>
     * If there is no such {@code DataPoint} then the iterator points to {@code null}.
     * <p>
     * Also points to {@code null} if it points to a null element in the beginning.
     *
     * @param <T> the Type of the entries to iterator over.
     * @param iterator The {@code Iterator} pointing to the {@code DataPoint}s.
     * @param resumeEventTime the Unix timestamp, e.g. of {@link Event.EventType#LIFECYCLE_RESUME}
     */
    <T extends DataPoint> void moveIteratorToLastBefore(final ListIterator<T> iterator, final long resumeEventTime) {
        iterator.previous();
        DataPoint point = iterator.next();

        // When the iterator's next() points to "before the event"
        if (point != null && point.getTimestamp() < resumeEventTime) {
            // Move forward until we reach the event time
            while (point != null && point.getTimestamp() < resumeEventTime) {
                // Load next location to check it's timestamp
                point = iterator.hasNext() ? iterator.next() : null;
            }
            iterator.previous();
            return;
        }

        // When the iterator's next() points to "after the event"
        // Move backward until we pass the event time
        while (point != null && point.getTimestamp() >= resumeEventTime) {
            // Load previous location to check it's timestamp
            point = iterator.hasPrevious() ? iterator.previous() : null;
        }
        iterator.next();
    }
}
