package de.cyface.model;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


public class JsonTest {

    /**
     * This test ensures that OSM tags which contain a quote do not break generate an unreadable Json String [DAT-1313].
     */
    @Test
    public void testJsonKeyValue_forValueWithQuotes() {
        // Arrange
        final var key = "name";
        // This tag value was found in the OSM way 207439390, OSM way state: 2022-05-14.
        //noinspection SpellCheckingInspection
        final var value = "Gewerbegebiet \"Bahnhofstraße\"";

        // Act
        final var result = Json.jsonKeyValue(key, value);

        // Assert
        //noinspection SpellCheckingInspection
        final var expected = "\"name\":\"Gewerbegebiet 'Bahnhofstraße'\"";
        assertThat(result.getStringValue(), is(equalTo(expected)));
    }
}
