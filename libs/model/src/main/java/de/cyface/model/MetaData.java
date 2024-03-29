/*
 * Copyright 2020-2023 Cyface GmbH
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

import org.apache.commons.lang3.Validate;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * The context of a deserialized {@code Measurement}.
 *
 * @author Armin Schnabel
 * @version 3.0.0
 * @since 1.2.0
 */
public class MetaData implements Serializable {

    /**
     * The current version of the deserialized measurement model.
     * <p>
     * To be able to read measurements deserialized by different deserializer versions.
     */
    public static final String CURRENT_VERSION = "3.0.0";
    /**
     * Regex of supported {@link MetaData} versions of this class.
     */
    public static final String SUPPORTED_VERSIONS = "3.0.0";
    /**
     * Used to serialize objects of this class. Only change this value if this classes attribute set changes.
     */
    private static final long serialVersionUID = -2781311916778609965L;
    /**
     * The worldwide unique identifier of the measurement.
     */
    private MeasurementIdentifier identifier;
    /**
     * The type of device uploading the data, such as "Pixel 3" or "iPhone 6 Plus".
     */
    private String deviceType;
    /**
     * The operating system version, such as "Android 9.0.0" or "iOS 11.2".
     */
    private String osVersion;
    /**
     * The version of the app that transmitted the measurement, such as "1.2.0" or "1.2.0-beta1".
     */
    private String appVersion;
    /**
     * The length of the measurement in meters.
     */
    private double length;
    /**
     * The id of the user who has uploaded this measurement.
     */
    private UUID userId;
    /**
     * The format version in which the {@code Measurement} was deserialized, e.g. "1.2.3".
     */
    private String version;

    /**
     * Creates new completely initialized <code>MetaData</code>.
     *
     * @param identifier The worldwide unique identifier of the measurement.
     * @param deviceType The type of device uploading the data, such as "Pixel 3" or "iPhone 6 Plus".
     * @param osVersion The operating system version, such as "Android 9.0.0" or "iOS 11.2".
     * @param appVersion The version of the app that transmitted the measurement, such as "1.2.0" or "1.2.0-beta1".
     * @param length The length of the measurement in meters.
     * @param userId The id of the user who has uploaded this measurement.
     * @param version The format version in which the {@code Measurement} was deserialized, e.g. "2.0.0".
     */
    public MetaData(final MeasurementIdentifier identifier, final String deviceType, final String osVersion,
            final String appVersion, final double length, final UUID userId, final String version) {

        Validate.isTrue(version.matches(SUPPORTED_VERSIONS), "Unsupported version: %s", version);
        this.identifier = identifier;
        this.deviceType = deviceType;
        this.osVersion = osVersion;
        this.appVersion = appVersion;
        this.length = length;
        this.userId = userId;
        this.version = version;
    }

    /**
     * No argument constructor as required by Apache Flink. Do not use this in your own code.
     */
    @SuppressWarnings("unused")
    public MetaData() {
        // Nothing to do
    }

    /**
     * @return The worldwide unique identifier of the measurement.
     */
    public MeasurementIdentifier getIdentifier() {
        return identifier;
    }

    /**
     * @return The type of device uploading the data, such as "Pixel 3" or "iPhone 6 Plus".
     */
    public String getDeviceType() {
        return deviceType;
    }

    /**
     * @return The operating system version, such as "Android 9.0.0" or "iOS 11.2".
     */
    public String getOsVersion() {
        return osVersion;
    }

    /**
     * @return The version of the app that transmitted the measurement, such as "1.2.0" or "1.2.0-beta1".
     */
    public String getAppVersion() {
        return appVersion;
    }

    /**
     * @return The length of the measurement in meters.
     */
    public double getLength() {
        return length;
    }

    /**
     * @return The id of the user who has uploaded this measurement.
     */
    public UUID getUserId() {
        return userId;
    }

    /**
     * @return The version of this {@code Measurement} model, such as "1.1.1" or "2.0.0".
     */
    public String getVersion() {
        return version;
    }

    /**
     * Required by Apache Flink.
     *
     * @param identifier The worldwide unique identifier of the measurement.
     */
    public void setIdentifier(final MeasurementIdentifier identifier) {
        this.identifier = identifier;
    }

    /**
     * Required by Apache Flink.
     *
     * @param deviceType The type of device uploading the data, such as "Pixel 3" or "iPhone 6 Plus".
     */
    @SuppressWarnings("unused")
    public void setDeviceType(final String deviceType) {
        this.deviceType = deviceType;
    }

    /**
     * Required by Apache Flink.
     *
     * @param osVersion The operating system version, such as "Android 9.0.0" or "iOS 11.2".
     */
    @SuppressWarnings("unused")
    public void setOsVersion(final String osVersion) {
        this.osVersion = osVersion;
    }

    /**
     * Required by Apache Flink.
     *
     * @param appVersion The version of the app that transmitted the measurement, such as "1.2.0" or "1.2.0-beta1".
     */
    @SuppressWarnings("unused")
    public void setAppVersion(final String appVersion) {
        this.appVersion = appVersion;
    }

    /**
     * Required by Apache Flink.
     *
     * @param length The length of the measurement in meters.
     */
    @SuppressWarnings("unused")
    public void setLength(final double length) {
        this.length = length;
    }

    /**
     * Required by Apache Flink.
     *
     * @param userId The name of the user who has uploaded this measurement.
     */
    @SuppressWarnings("unused")
    public void setUserId(final UUID userId) {
        this.userId = userId;
    }

    /**
     * Required by Apache Flink.
     *
     * @param version The version of this {@code Measurement} model, such as "1.1.1" or "2.0.0".
     */
    public void setVersion(final String version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "MetaData{" +
                "identifier=" + identifier +
                ", deviceType='" + deviceType + '\'' +
                ", osVersion='" + osVersion + '\'' +
                ", appVersion='" + appVersion + '\'' +
                ", length=" + length +
                ", userId='" + userId + '\'' +
                ", version='" + version + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        MetaData metaData = (MetaData)o;
        return Double.compare(metaData.length, length) == 0 &&
                identifier.equals(metaData.identifier) &&
                deviceType.equals(metaData.deviceType) &&
                osVersion.equals(metaData.osVersion) &&
                appVersion.equals(metaData.appVersion) &&
                userId.equals(metaData.userId) &&
                version.equals(metaData.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, deviceType, osVersion, appVersion, length, userId, version);
    }
}
