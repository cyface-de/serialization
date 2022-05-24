package de.cyface.deserializer.v1;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.cyface.model.v1.Event;
import de.cyface.model.v1.Measurement;
import de.cyface.model.v1.MeasurementIdentifier;
import de.cyface.model.v1.MetaData;

/**
 * A {@link Deserializer} for a file in Cyface binary format. The deserializer requires one file containing the data and
 * another containing the user events. Constructs a new measurement from a ZLIB compressed <code>InputStream</code> of
 * data.
 * <p>
 * A {@link DeserializerFactory} is necessary to create such a <code>Deserializer</code>.
 *
 * @author Klemens Muthmann
 * @see DeserializerFactory
 */
public class BinaryFormatDeserializer implements Deserializer {
    /**
     * Used to serialize objects of this class. Only change this if this classes attribute set has changed.
     */
    private static final long serialVersionUID = -706300845329533657L;
    /**
     * Logger used to log messages from objects of this class. Configure it using <tt>/src/main/resources</tt>.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Deserializer.class);
    /**
     * The default value for the parameter "nowrap" used in the Cyface Binary Format. This parameter needs to be passed
     * to the Inflater's constructor to decompress the compressed bytes.
     */
    private static final boolean NOWRAP = true;
    /**
     * The current version of the transferred file. This is always specified by the first two bytes of the file
     * transferred and helps compatible APIs to process data from different client versions.
     */
    public static final short TRANSFER_FILE_FORMAT_VERSION = 1;
    /**
     * The current version of the transferred file which contains {@link Event}s. This is always specified by the first
     * two bytes of the file transferred and helps compatible APIs to process data from different client versions.
     */
    private static final short EVENT_TRANSFER_FILE_FORMAT_VERSION = 1;

    /**
     * The meta information about the {@link Measurement}. This information is not part of the datafiles but is usually
     * stored
     * alongside the binary data. It is usually used to get a glimpse into what data to expect.
     */
    private final MetaData metaData;
    /**
     * The events captured for this measurement
     */
    private final List<Event> events;
    /**
     * The stream of compressed data to load locations and measured data points from
     */
    private final InputStream compressedData;

    // FIXME: It makes no sense, that events are provided as a list and the data is provided as a compressed stream.
    /**
     * Creates a new completely initialized object of this class. The constructor is not public, since one should use a
     * {@link DeserializerFactory} to create instances of this class.
     *
     * @param metaData The meta information about the {@link Measurement}. This information is not part of the datafiles
     *            but is usually stored
     *            * alongside the binary data. It is usually used to get a glimpse into what data to expect
     * @param events The events captured for this measurement
     * @param compressedData The stream of compressed data to load locations and measured data points from
     */
    BinaryFormatDeserializer(final MetaData metaData, final List<Event> events,
                             final InputStream compressedData) {
        Objects.requireNonNull(metaData);
        Objects.requireNonNull(events);
        Objects.requireNonNull(compressedData);

        this.metaData = metaData;
        this.events = new ArrayList<>(events);
        this.compressedData = compressedData;
    }

    @Override
    public Measurement read() throws IOException, InvalidLifecycleEvents {
        try (InflaterInputStream uncompressedInput = new InflaterInputStream(compressedData, new Inflater(NOWRAP))) {
            final var version = BinaryFormatParser.readShort(uncompressedInput);
            if (version != TRANSFER_FILE_FORMAT_VERSION) {
                LOGGER.error("Encountered data in invalid format. Only Cyface Data Format Version 1 is supported!");
                return null;
            }

            final var geoLocationsCount = BinaryFormatParser.readInt(uncompressedInput);
            final var accelerationsCount = BinaryFormatParser.readInt(uncompressedInput);
            final var rotationsCount = BinaryFormatParser.readInt(uncompressedInput);
            final var directionsCount = BinaryFormatParser.readInt(uncompressedInput);
            final var identifier = metaData.getIdentifier();
            LOGGER.debug(
                    "Reading {} geo locations, {} accelerations, {} rotations, {} directions from measurement {}.",
                    geoLocationsCount, accelerationsCount, rotationsCount, directionsCount, identifier);
            final var locations = BinaryFormatParser.readGeoLocations(uncompressedInput, geoLocationsCount,
                    identifier);
            final var accelerations = BinaryFormatParser.readPoint3Ds(uncompressedInput, accelerationsCount);
            final var rotations = BinaryFormatParser.readPoint3Ds(uncompressedInput, rotationsCount);
            final var directions = BinaryFormatParser.readPoint3Ds(uncompressedInput, directionsCount);
            final var builder = new TrackBuilder();
            final var tracks = builder.build(locations, events, accelerations, rotations, directions);
            return new Measurement(metaData, tracks);
        }
    }

    @Override
    public List<MeasurementIdentifier> peakIntoDatabase() throws IOException {
        return Collections.singletonList(metaData.getIdentifier());
    }

    /**
     * Constructs new events from a ZLIB compressed <code>InputStream</code> of data.
     *
     * @param input The stream of compressed data to load events data from
     * @return The list of newly created {@link Event}s
     * @throws IOException If reading the <code>InputStream</code>, was not successful
     */
    static List<Event> readEvents(final InputStream input) throws IOException {
        final var ret = new ArrayList<Event>();
        try (var uncompressedInput = new InflaterInputStream(input, new Inflater(NOWRAP))) {
            var version = BinaryFormatParser.readShort(uncompressedInput);
            if (version != EVENT_TRANSFER_FILE_FORMAT_VERSION) {
                throw new IllegalStateException(
                        "Encountered data in invalid format. Only Cyface Data Format Version 1 is supported!");
            }

            final var eventsCount = BinaryFormatParser.readInt(uncompressedInput);
            LOGGER.debug("Reading {} events from events file.", eventsCount);
            ret.addAll(readEvents(uncompressedInput, eventsCount));
        }
        return ret;
    }

    /**
     * Reads <code>count</code> events from the <code>input</code> and
     * provides them in the form of a new <code>BufferedDataTable</code>.
     *
     * @param input The stream to read the events from
     * @param count The number of events to read
     * @throws IOException If reading the stream fails
     */
    static List<Event> readEvents(final InputStream input, final int count) throws IOException {
        final var events = new ArrayList<Event>(count);
        for (var i = 0; i < count; i++) {
            final var timestamp = BinaryFormatParser.readLong(input);
            final var serializedEventType = BinaryFormatParser.readShort(input);
            final var valueByteLength = BinaryFormatParser.readShort(input);

            final var eventType = deserializeEventType(serializedEventType);
            // Value String
            final var value = BinaryFormatParser.readString(input, valueByteLength);
            final var event = new Event(eventType, timestamp, value);
            events.add(event);
        }
        LOGGER.debug("Read {} events!", events.size());
        return events;
    }

    /**
     * Converts the {@param serializedEventType} back to it's actual {@link Event.EventType} as defined in
     * {@link #EVENT_TRANSFER_FILE_FORMAT_VERSION}.
     * <p>
     * <b>Attention:</b> Do not break the compatibility in here without increasing the
     * {@link #EVENT_TRANSFER_FILE_FORMAT_VERSION}.
     *
     * @param serializedEventType the serialized value of the actual {@code Event.EventType}
     * @return the deserialized {@code EventType}
     */
    private static Event.EventType deserializeEventType(final short serializedEventType) {
        switch (serializedEventType) {
            case 1:
                return Event.EventType.LIFECYCLE_START;
            case 2:
                return Event.EventType.LIFECYCLE_STOP;
            case 3:
                return Event.EventType.LIFECYCLE_RESUME;
            case 4:
                return Event.EventType.LIFECYCLE_PAUSE;
            case 5:
                return Event.EventType.MODALITY_TYPE_CHANGE;
            default:
                throw new IllegalArgumentException("Unknown EventType short representation: " + serializedEventType);
        }
    }
}