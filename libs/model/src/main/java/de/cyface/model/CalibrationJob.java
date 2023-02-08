/*
 * Copyright 2022-2023 Cyface GmbH
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
package de.cyface.model;

/**
 * A {@link Job} which contains details about filtered data during calibration.
 *
 * @author Armin Schnabel
 * @version 1.0.0
 * @since 2.3.1
 */
@SuppressWarnings("unused") // Part of the API
public class CalibrationJob extends Job {

    /**
     * {@code true} when the measurement contains processable tracks.
     */
    private boolean processable;
    /**
     * The number of locations to be processed for this job.
     */
    private final int totalLocations;
    /**
     * The number of locations which were filtered due to a rotated device.
     */
    private int rotatedLocations = 0;
    /**
     * The number of locations which were filtered during interpolation.
     */
    private int nonInterpolatableLocations = 0;

    /**
     * Constructs a fully initialized instance of this class.
     *
     * @param id The id of the job to update about the status and progress of the processing.
     * @param startedBy The id of the user who triggered the pipeline and will own the result data.
     * @param processable {@code true} when the measurement contains processable tracks.
     * @param totalLocations The number of locations to be processed for this job.
     */
    public CalibrationJob(final String id, final String startedBy, final boolean processable,
            final int totalLocations) {
        super(id, startedBy);
        this.processable = processable;
        this.totalLocations = totalLocations;
    }

    /**
     * Constructs a fully initialized instance of this class.
     *
     * @param job The job which is processed.
     * @param processable {@code true} when the measurement contains processable tracks.
     * @param totalLocations The number of locations to be processed for this job.
     */
    public CalibrationJob(final Job job, final boolean processable, final int totalLocations) {
        super(job.getId(), job.getStartedBy());
        this.processable = processable;
        this.totalLocations = totalLocations;
    }

    /**
     * @return {@code true} when the measurement contains processable tracks.
     */
    @SuppressWarnings("unused") // Part of the API
    public boolean isProcessable() {
        return processable;
    }

    /**
     * @return The number of tracks which were filtered due to a rotated device.
     */
    @SuppressWarnings("unused") // Part of the API
    public int getRotatedLocations() {
        return rotatedLocations;
    }

    /**
     * @return The number of locations which were filtered during interpolation.
     */
    public int getNonInterpolatableLocations() {
        return nonInterpolatableLocations;
    }

    /**
     * @return The number of locations to be processed for this job.
     */
    public int getTotalLocations() {
        return totalLocations;
    }

    /**
     * @param rotatedLocations The number of tracks which were filtered due to a rotated device.
     * @return This for chaining.
     */
    @SuppressWarnings("unused") // Part of the API
    public CalibrationJob setRotatedLocations(final int rotatedLocations) {
        this.rotatedLocations = rotatedLocations;
        return this;
    }

    /**
     * @param nonInterpolatableLocations The number of locations which were filtered during interpolation.
     * @return This for chaining.
     */
    public CalibrationJob setNonInterpolatableLocations(int nonInterpolatableLocations) {
        this.nonInterpolatableLocations = nonInterpolatableLocations;
        return this;
    }

    /**
     * @param processable {@code true} when the measurement contains processable tracks.
     * @return This for chaining.
     */
    @SuppressWarnings("unused") // Part of the API
    public CalibrationJob setProcessable(boolean processable) {
        this.processable = processable;
        return this;
    }
}
