= Serialization

![https://vertx.io](https://img.shields.io/badge/vert.x-4.1.2-purple.svg)
![https://github.com/cyface-de/serialization/actions/workflows/gradle_build.yml](https://github.com/cyface-de/serialization/actions/workflows/gradle_build.yml/badge.svg)
![https://github.com/cyface-de/serialization/actions/workflows/gradle_publish.yml](https://github.com/cyface-de/serialization/actions/workflows/gradle_publish.yml/badge.svg)

This application represents the [Cyface](https://cyface.de) serialization software.

It is used to de-/serialize the Cyface Binary Format which contains traffic data from Cyface measurement devices, such as our sensor box or our smartphone application.

Our smartphone SDK is available as GPL application for [Android](https://github.com/cyface-de/android-backend) and [iOS](https://github.com/cyface-de/ios-backend) as well.

If you require this software under a closed source license for you own projects, please [contact us](https://www.cyface.de/#kontakt).

Changes between versions are found in the [Release Section](https://github.com/cyface-de/serialization/releases).

The project uses [Gradle](https://gradle.org/) as the build system.
It is set up as a Gradle multi project build.
The following sections provide an overview of the details of each sub-projects.

Overview
========

* [Model](#Model)
* [Serializer](#Serializer)
* [Release a new Version](#Release-A-New-Version)
* [Publishing Artifacts to GitHub Packages manually](#Publishing-Artifacts-To-Github-Packages-Manually)
* [Licensing](#Licensing)

Model
=====

Model classes shared between multiple projects.

Serializer
==========

This project provides a library to transform raw captured data, usually produced on a smartphone, into the Cyface binary format.
The following explanation is a description of what this library does. 
Reading them is only important if you need to understand the binary format produced by this project.
If you have no interest in the details, just use our `deserializer` to deserialize binaries produced by this project.

## Storage Format

The data is usually placed into a file using ZLIB compression, with compression level 5 and the nowrap flag set to true.
The reason for using this level and the non standard value for the nowrap flag being that on iOS these are the standard unchangeable values.
Since there is no way to change them on iOS, compression level and nowrap are set for compatibility reasons.

## The Cyface Binary Format
The structure of a file in  the Cyface Binary Format in the most recent version is based on a Protobuf serializer.
Please use the appropriate Protobuf deserializer to decode such a file.
For an example on how to achieve this see the deserializer sub-project of this project or our [Protobuf Schemes](https://github.com/cyface-de/protos).
However values provided to this serializer should be preprocessed, according to the following specification.

The first two bytes specify the version of the Cyface Binary Format using Big Endian.
The current version is **version 3**. 
So the header should read `0000 0000 0000 0011`.

The following sections contain the three data areas: *Events*, *Locations* and *Sensor Data*.

Before going into the detailed descriptions it is important to understand the epoch format, fixed point values and run length encoding.

The epoch format is a timestamp as a 64-Bit integer counting milliseconds since *01/01/1970 00:00:00,000*.

Fixed point values are integers representing a floating point value with an implicit comma at a certain point.
A fixed point value of `1234` with two places after the comma should be handled like `12.34` after deserialization.

Finally run length encoding stores values not with their actual value but by difference to the previous value in a timeline.
So `1.83792349,4.378237850,2.75263407` becomes `1838,2540,915`.

### Events
Events are user interactions, that occurred with the measuring device.
They consist of a tuple of three values.
The first is the type of the event, given by the protobuf scheme.
A type can be for example a user pressing the start button on the measuring device.
The second value is the events timestamp in epoch format.
The third is an optional event value.
The modality type changed event for example requires the value of the new transportation mode, if the user switches from walking to using a bus, for example.

### Location Records
Location records are stored as tuples of five values.
The first is a timestamp in epoch format using run length encoding.
The second is the latitude value as a fixed point value with 6 places after the comma and run length encoding.
The third is the longitude value, which is encoded exactly as the latitude.
The fourth is the GNSS accuracy value using two places after the command and run length encoding.
The fifth finally is the traveling speed in meters per second using two places after the comma and run length encoding.

### Sensor Data
Sensor data is stored in batches, as this data is also captured and saved using batches.
There is one section for accelerations, one for rotations and one for directions.
All three are formatted very similar.

Each batch contains multiple tuples of a timestamp in epoch format with run length encoding and a fixed point value for each of the three space dimensions *x*, *y* and *z*.
The fixed point value is an integer using run length encoding with three places after the comma for accelerations and rotations and two places for directions.

Release a new Version
=====================

See [Cyface Collector Readme](https://github.com/cyface-de/data-collector#release-a-new-version)

* `version` in root `build.gradle` is automatically set by the CI
* Just tag the release and push the tag to Github
* The Github package is automatically published when a new version is tagged and pushed by our
[Github Actions](https://github.com/cyface-de/serialization/actions) to
the [Github Registry](https://github.com/cyface-de/serialization/packages)
* The tag is automatically marked as a 'new Release' on [Github](https://github.com/cyface-de/serialization/releases)


Publishing artifacts to GitHub Packages manually
================================================

The artifacts produced by this project are distributed via [GitHubPackages](https://github.com/features/packages).
Before you can publish artifacts you need to rename `gradle.properties.template` to `gradle.properties` and enter your GitHub credentials.
How to obtain these credentials is described [here](https://help.github.com/en/github/managing-packages-with-github-packages/about-github-packages#about-tokens).

To publish a new version of an artifact you need to:

1. Increase the version number of the sub-project within the `build.gradle` file
2. Call `./gradlew publish`

This will upload a new artifact to GitHub packages with the new version.
GitHub Packages will not accept to overwrite an existing version or to upload a lower version.
This project uses [semantic versioning](https://semver.org/).

Licensing
=========

Copyright 2018-2024 Cyface GmbH

This file is part of the Cyface Serialization.

The Cyface Serialization is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

The Cyface Serialization is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with the Cyface Serialization.  If not, see http://www.gnu.org/licenses/.
