/*
 * Copyright (C) 2020 Cyface GmbH - All Rights Reserved
 *
 * This file is part of the Cyface Server Backend.
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */
package de.cyface.deserializer.v1;

/**
 * An <code>Exception</code> that is thrown when a {@link Deserializer} can not read the requested
 * {@link de.cyface.model.Measurement}.
 * This may only occur on <code>Deserializer</code>s reading from a source with multiple <code>Measurement</code>s such
 * as a phone data export.
 *
 * @author Klemens Muthmann
 */
public class NoSuchMeasurement extends Throwable {
}