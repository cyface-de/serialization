/*
 * Copyright (C) 2021 Cyface GmbH - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */
package de.cyface.deserializer.v1;

/**
 * This {@code Exception} is thrown when a file of a deprecated or not yet supported file format is to be processed.
 *
 * @author Armin Schnabel
 * @version 1.0.0
 * @since 5.0.0
 */
public class UnsupportedFileVersion extends Exception {
    /**
     * Used to serialize objects of this class. Only change this value if the attribute set changes.
     */
    private static final long serialVersionUID = -3574350415381849173L;

    /**
     * Creates a new instance of this class.
     *
     * @param message An error message to display
     */
    @SuppressWarnings("unused") // API
    public UnsupportedFileVersion(String message) {
        super(message);
    }

    /**
     * Creates a new instance of this class.
     *
     * @param message An error message to display
     * @param cause The stacktrace which lead to this exception
     */
    @SuppressWarnings("unused") // API
    public UnsupportedFileVersion(String message, Throwable cause) {
        super(message, cause);
    }
}