/*
 * Copyright 2022 Cyface GmbH
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
 */
@SuppressWarnings("unused") // Part of the API
public class CalibrationJob extends Job {

    /**
     * {@code true} when the measurement contains processable tracks.
     */
    private boolean processable;
    /**
     * The number of tracks which were filtered due to a rotated device.
     */
    private int rotatedTracks = 0;
    /**
     * The total number of tracks, to calculate the share of {@link #getRotatedTracks()}.
     */
    private int totalTracks = 0;

    /**
     * Constructs a fully initialized instance of this class.
     *
     * @param id The id of the job to update about the status and progress of the processing.
     * @param startedBy The id of the user who triggered the pipeline and will own the result data.
     * @param processable {@code true} when the measurement contains processable tracks.
     */
    public CalibrationJob(final String id, final String startedBy, final boolean processable) {
        super(id, startedBy);
        this.processable = processable;
    }

    /**
     * Constructs a fully initialized instance of this class.
     *
     * @param job The job which is processed.
     * @param processable {@code true} when the measurement contains processable tracks.
     */
    public CalibrationJob(final Job job, final boolean processable) {
        super(job.getId(), job.getStartedBy());
        this.processable = processable;
    }

    /**
     * @return The share of the tracks which were filtered due to a rotated device.
     */
    @SuppressWarnings("unused") // Part of the API
    public double rotatedShare() {
        return totalTracks == 0 ? 0 : (double)rotatedTracks / totalTracks;
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
    public int getRotatedTracks() {
        return rotatedTracks;
    }

    /**
     * @return The total number of tracks, to calculate the share of {@link #getRotatedTracks()}.
     */
    @SuppressWarnings("unused") // Part of the API
    public int getTotalTracks() {
        return totalTracks;
    }

    /**
     * @param rotatedTracks The number of tracks which were filtered due to a rotated device.
     * @return This for chaining.
     */
    @SuppressWarnings("unused") // Part of the API
    public CalibrationJob setRotatedTracks(final int rotatedTracks) {
        this.rotatedTracks = rotatedTracks;
        return this;
    }

    /**
     * @param totalTracks The total number of tracks, to calculate the share of {@link #getRotatedTracks()}.
     * @return This for chaining.
     */
    @SuppressWarnings("unused") // Part of the API
    public CalibrationJob setTotalTracks(int totalTracks) {
        this.totalTracks = totalTracks;
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
