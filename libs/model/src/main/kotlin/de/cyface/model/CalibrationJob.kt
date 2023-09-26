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
package de.cyface.model

import java.util.UUID

/**
 * A [Job] which contains details about filtered data during calibration.
 *
 * @author Armin Schnabel
 * @version 2.0.1
 * @since 2.3.1
 */
@Suppress("unused") // Part of the API
class CalibrationJob : Job {
    /**
     * `true` when the measurement contains processable tracks.
     */
    var isProcessable: Boolean
        private set

    /**
     * The number of locations to be processed for this job.
     */
    @Suppress("MemberVisibilityCanBePrivate") // Part of the API
    val totalLocations: Int

    /**
     * The number of locations which were filtered due to a rotated device.
     */
    var rotatedLocations = 0
        private set

    /**
     * The number of locations which were filtered during interpolation.
     */
    var nonInterpolatableLocations = 0
        private set

    /**
     * Constructs a fully initialized instance of this class.
     *
     * @param id The id of the job to update about the status and progress of the processing.
     * @param startedBy The id of the user who triggered the pipeline and will own the result data.
     * @param processable `true` when the measurement contains processable tracks.
     * @param totalLocations The number of locations to be processed for this job.
     */
    constructor(
        id: String?, startedBy: UUID?, processable: Boolean,
        totalLocations: Int
    ) : super(id, startedBy) {
        isProcessable = processable
        this.totalLocations = totalLocations
    }

    /**
     * Constructs a fully initialized instance of this class.
     *
     * @param job The job which is processed.
     * @param processable `true` when the measurement contains processable tracks.
     * @param totalLocations The number of locations to be processed for this job.
     */
    constructor(job: Job, processable: Boolean, totalLocations: Int) : super(job.id, job.startedBy) {
        isProcessable = processable
        this.totalLocations = totalLocations
    }

    /**
     * @param rotatedLocations The number of tracks which were filtered due to a rotated device.
     * @return This for chaining.
     */
    fun setRotatedLocations(rotatedLocations: Int): CalibrationJob {
        this.rotatedLocations = rotatedLocations
        return this
    }

    /**
     * @param nonInterpolatableLocations The number of locations which were filtered during interpolation.
     * @return This for chaining.
     */
    fun setNonInterpolatableLocations(nonInterpolatableLocations: Int): CalibrationJob {
        this.nonInterpolatableLocations = nonInterpolatableLocations
        return this
    }

    /**
     * @param processable `true` when the measurement contains processable tracks.
     * @return This for chaining.
     */
    fun setProcessable(processable: Boolean): CalibrationJob {
        isProcessable = processable
        return this
    }
}