/*
 * Copyright 2019-2021 Cyface GmbH
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
package de.cyface.serializer.model;

import java.util.UUID;

import org.apache.commons.lang3.Validate;

/**
 * A class representing one device simulated by the phone data simulator.
 *
 * @author Klemens Muthmann
 * @version 2.0.0
 * @since 1.0.0
 */
public final class SimulatedDevice {
    /**
     * The world wide unique identifier of the <code>SimulatedDevice</code>
     */
    private final UUID deviceIdentifier;
    // TODO: This should either be part of the RandomDataSimulator or be extended to work with the PhoneDataSimulator.
    /**
     * The currently simulated device wide unique measurement identifier
     */
    private transient long currentMeasurementIdentifier;
    /**
     * A human readable name for the type of the simulated device
     */
    private final String deviceType;
    /**
     * The operating system version of the simulated device
     */
    private final String osVersion;
    /**
     * The simulated version of the application used to transmit the data
     */
    private final String appVersion;

    /**
     * Creates a new completely initialized object of this class with a random generated world wide unique identifier.
     *
     * @param deviceType A human readable name for the type of the simulated device
     * @param osVersion The operating system version of the simulated device
     * @param appVersion The simulated version of the application used to transmit the data
     */
    public SimulatedDevice(final String deviceType, final String osVersion, final String appVersion) {
        this(UUID.randomUUID(), deviceType, osVersion, appVersion);
    }

    /**
     * @param deviceIdentifier The world wide unique identifier of the <code>SimulatedDevice</code>
     * @param deviceType A human readable name for the type of the simulated device
     * @param osVersion The operating system version of the simulated device
     * @param appVersion The simulated version of the application used to transmit the data
     */
    public SimulatedDevice(final UUID deviceIdentifier, final String deviceType, final String osVersion,
            final String appVersion) {
        Validate.notNull(deviceIdentifier);
        Validate.notEmpty(deviceType);
        Validate.notEmpty(osVersion);
        Validate.notEmpty(appVersion);

        this.deviceIdentifier = deviceIdentifier;
        this.deviceType = deviceType;
        this.osVersion = osVersion;
        this.appVersion = appVersion;
    }

    /**
     * @return The world wide unique identifier of the <code>SimulatedDevice</code>
     */
    public UUID getDeviceIdentifier() {
        return deviceIdentifier;
    }

    /**
     * @return The currently simulated device wide unique measurement identifier. This is increased by one on each
     *         invocation of this method
     */
    public long getCurrentMeasurementIdentifierAndInc() {
        return currentMeasurementIdentifier++;
    }

    /**
     * @return The currently simulated device wide unique measurement identifier.
     */
    public long getCurrentMeasurementIdentifier() {
        return currentMeasurementIdentifier;
    }

    /**
     * @return A human readable name for the type of the simulated device
     */
    public String getDeviceType() {
        return deviceType;
    }

    /**
     * @return The operating system version of the simulated device
     */
    public String getOsVersion() {
        return osVersion;
    }

    /**
     * @return The simulated version of the application used to transmit the data
     */
    public String getAppVersion() {
        return appVersion;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((appVersion == null) ? 0 : appVersion.hashCode());
        result = prime * result + ((deviceIdentifier == null) ? 0 : deviceIdentifier.hashCode());
        result = prime * result + ((deviceType == null) ? 0 : deviceType.hashCode());
        result = prime * result + ((osVersion == null) ? 0 : osVersion.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SimulatedDevice other = (SimulatedDevice)obj;
        if (appVersion == null) {
            if (other.appVersion != null) {
                return false;
            }
        } else if (!appVersion.equals(other.appVersion)) {
            return false;
        }
        if (deviceIdentifier == null) {
            if (other.deviceIdentifier != null) {
                return false;
            }
        } else if (!deviceIdentifier.equals(other.deviceIdentifier)) {
            return false;
        }
        if (deviceType == null) {
            if (other.deviceType != null) {
                return false;
            }
        } else if (!deviceType.equals(other.deviceType)) {
            return false;
        }
        if (osVersion == null) {
            if (other.osVersion != null) {
                return false;
            }
        } else if (!osVersion.equals(other.osVersion)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "SimulatedDevice [deviceIdentifier=" + deviceIdentifier + ", deviceType=" + deviceType + ", osVersion="
                + osVersion + ", appVersion=" + appVersion + "]";
    }
}
