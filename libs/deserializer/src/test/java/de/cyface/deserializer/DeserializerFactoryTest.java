package de.cyface.deserializer;

import de.cyface.deserializer.factory.BinaryFormatDeserializer;
import de.cyface.deserializer.factory.DeserializerFactory;
import de.cyface.deserializer.factory.UnzippedPhoneDataDeserializer;
import de.cyface.deserializer.factory.V3UncompressedBinaryFormatDeserializer;
import de.cyface.deserializer.factory.ZippedPhoneDataDeserializer;
import de.cyface.model.MeasurementIdentifier;
import de.cyface.model.MetaData;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

public class DeserializerFactoryTest {

    /** A real {@link MetaData} instance usable across tests. No methods are called on it — only its type matters. */
    private static MetaData testMetaData() {
        return MetaData.Companion.create(
                new MeasurementIdentifier("test-device", 1L),
                "TestDevice",
                "Android 13",
                "1.0.0",
                0.0,
                UUID.randomUUID(),
                MetaData.CURRENT_VERSION,
                new Date()
        );
    }

    @Test
    public void testBuildCompressedPhoneDeserializer() throws URISyntaxException {
        // Arrange
        final var pathToEmptyTestFile = Path.of(Objects.requireNonNull(DeserializerFactoryTest.class.getResource("/emptyTestFile")).toURI());

        final var userId = UUID.randomUUID();
        final var measuresArchive = pathToEmptyTestFile;
        final var accelerationsArchive = pathToEmptyTestFile;
        final var rotationsArchive = pathToEmptyTestFile;
        final var directionsArchive = pathToEmptyTestFile;
        final var uploadDate = new Date();

        // Act
        final var result = DeserializerFactory.forCompressedData().create(userId, measuresArchive, accelerationsArchive, rotationsArchive, directionsArchive, uploadDate);

        // Assert
        assertThat(result, is(instanceOf(ZippedPhoneDataDeserializer.class)));
    }

    @Test
    public void testBuildUncompressedPhoneDeserializer() throws URISyntaxException {
        // Arrange
        final var pathToEmptyTestFile = Path.of(Objects.requireNonNull(DeserializerFactoryTest.class.getResource("/emptyTestFile")).toURI());

        final var userId = UUID.randomUUID();
        final var measuresDatabase = pathToEmptyTestFile;
        final List<Path> accelerationFiles = List.of(pathToEmptyTestFile);
        final List<Path> rotationFiles = List.of(pathToEmptyTestFile);
        final List<Path> directionFiles = List.of(pathToEmptyTestFile);
        final var uploadDate = new Date();

        // Act
        final var result = DeserializerFactory.forUncompressedData().create(userId, measuresDatabase, accelerationFiles, rotationFiles, directionFiles, uploadDate);

        // Assert
        assertThat(result, is(instanceOf(UnzippedPhoneDataDeserializer.class)));
    }

    @Test
    public void testBuildCompressedMongoDeserializer() throws IOException {
        // Arrange
        final var metadata = testMetaData();
        final var stream = InputStream.nullInputStream();

        // Act
        final var result = DeserializerFactory.forCompressedData().create(metadata, stream);

        // Assert
        assertThat(result, is(instanceOf(BinaryFormatDeserializer.class)));
    }

    @Test
    public void testBuildUncompressedMongoDeserializer() throws IOException {
        // Arrange
        final var metadata = testMetaData();
        final var stream = InputStream.nullInputStream();

        // Act
        final var result = DeserializerFactory.forUncompressedData().create(metadata, stream);

        // Assert
        assertThat(result, is(instanceOf(V3UncompressedBinaryFormatDeserializer.class)));
    }
}
