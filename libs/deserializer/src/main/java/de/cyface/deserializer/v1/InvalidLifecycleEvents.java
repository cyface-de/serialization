/*
 * Copyright (C) 2020 Cyface GmbH - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */
package de.cyface.deserializer.v1;

/**
 * This <code>Exception</code> is thrown if the sequence of {@code de.cyface.model.Event}s is invalid.
 * <p>
 * E.g.:
 * - {@link de.cyface.model.Event.EventType#LIFECYCLE_RESUME} event recorded without prior pause event. [VIC-263]
 *
 * @author Armin Schnabel
 * @version 1.0.0
 * @since 1.3.0
 */
public class InvalidLifecycleEvents extends Exception {
    /**
     * Used to serialize objects of this class. Only change this value if the attribute set changes.
     */
    private static final long serialVersionUID = 1971945670723702382L;

    /**
     * Creates a new instance of this class.
     *
     * @param message An error message to display
     */
    @SuppressWarnings("unused") // API
    public InvalidLifecycleEvents(String message) {
        super(message);
    }

    /**
     * Creates a new instance of this class.
     *
     * @param message An error message to display
     * @param cause The stacktrace which lead to this exception
     */
    @SuppressWarnings("unused") // API
    public InvalidLifecycleEvents(String message, Throwable cause) {
        super(message, cause);
    }
}