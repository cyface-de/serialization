package de.cyface.deserializer;

import de.cyface.deserializer.factory.BinaryFormatDeserializer;
import de.cyface.deserializer.factory.DeserializerFactory;
import de.cyface.deserializer.factory.UnzippedPhoneDataDeserializer;
import de.cyface.deserializer.factory.V3UncompressedBinaryFormatDeserializer;
import de.cyface.deserializer.factory.ZippedPhoneDataDeserializer;
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
import static org.mockito.Mockito.mock;

public class DeserializerFactoryTest {

    @Test
    public void testBuildCompressedPhoneDeserializer() throws URISyntaxException {
        // Arrange
        final var pathToEmptyTestFile = Path.of(Objects.requireNonNull(DeserializerFactoryTest.class.getResource("/emptyTestFile")).toURI());

        final var userId = mock(UUID.class);
        final var measuresArchive = pathToEmptyTestFile;
        final var accelerationsArchive = pathToEmptyTestFile;
        final var rotationsArchive = pathToEmptyTestFile;
        final var directionsArchive = pathToEmptyTestFile;
        final var uploadDate = mock(Date.class);

        // Act
        final var result = DeserializerFactory.forCompressedData().create(userId, measuresArchive, accelerationsArchive, rotationsArchive, directionsArchive, uploadDate);

        // Assert
        assertThat(result, is(instanceOf(ZippedPhoneDataDeserializer.class)));
    }
    @Test
    public void testBuildUncompressedPhoneDeserializer() throws URISyntaxException {
        // Arrange
        final var pathToEmptyTestFile = Path.of(Objects.requireNonNull(DeserializerFactoryTest.class.getResource("/emptyTestFile")).toURI());

        final var userId = mock(UUID.class);
        final var measuresDatabase = pathToEmptyTestFile;
        final List<Path> accelerationFiles = List.of(pathToEmptyTestFile);
        final List<Path> rotationFiles = List.of(pathToEmptyTestFile);
        final List<Path> directionFiles = List.of(pathToEmptyTestFile);
        final var uploadDate = mock(Date.class);

        // Act
        final var result = DeserializerFactory.forUncompressedData().create(userId, measuresDatabase, accelerationFiles, rotationFiles, directionFiles, uploadDate);

        // Assert
        assertThat(result, is(instanceOf(UnzippedPhoneDataDeserializer.class)));
    }
    @Test
    public void testBuildCompressedMongoDeserializer() throws IOException {
        // Arrange
        final var metadata = mock(MetaData.class);
        final var stream = mock(InputStream.class);

        // Act
        final var result = DeserializerFactory.forCompressedData().create(metadata, stream);

        // Assert
        assertThat(result, is(instanceOf(BinaryFormatDeserializer.class)));
    }
    @Test
    public void testBuildUncompressedMongoDeserializer() throws IOException {
        // Arrange
        final var metadata = mock(MetaData.class);
        final var stream = mock(InputStream.class);

        // Act
        final var result = DeserializerFactory.forUncompressedData().create(metadata, stream);

        // Assert
        assertThat(result, is(instanceOf(V3UncompressedBinaryFormatDeserializer.class)));
    }
}
