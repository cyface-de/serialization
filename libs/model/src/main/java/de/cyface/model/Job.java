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

import java.io.Serializable;
import java.util.Objects;

import org.apache.commons.lang3.Validate;

/**
 * The job which triggered pipeline processing.
 *
 * @author Armin Schnabel
 * @version 1.0.0
 * @since 2.3.0
 */
@SuppressWarnings("unused") // Part of the API
public class Job implements Serializable {

    /**
     * Used to serialize objects of this class. Only change this value if this classes attribute set changes.
     */
    private static final long serialVersionUID = 3954109074386838577L;
    /**
     * The id of the job to update about the status and progress of the processing.
     */
    private final String id;
    /**
     * The id of the user who triggered the pipeline and will own the result data.
     */
    private final String startedBy;

    /**
     * Constructs a fully initialized instance of this class.
     *
     * @param id The id of the job to update about the status and progress of the processing.
     * @param startedBy The id of the user who triggered the pipeline and will own the result data.
     */
    @SuppressWarnings("unused") // Part of the API
    public Job(final String id, final String startedBy) {
        this.id = Validate.notEmpty(id);
        this.startedBy = Validate.notEmpty(startedBy);
    }

    /**
     * @return The id of the job to update about the status and progress of the processing.
     */
    @SuppressWarnings("unused") // Part of the API
    public String getId() {
        return id;
    }

    /**
     * @return The id of the user who triggered the pipeline and will own the result data.
     */
    @SuppressWarnings("unused") // Part of the API
    public String getStartedBy() {
        return startedBy;
    }

    @Override
    public String toString() {
        return "Job{" +
                "id='" + id + '\'' +
                ", startedBy='" + startedBy + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Job job = (Job)o;
        return id.equals(job.id) && startedBy.equals(job.startedBy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
