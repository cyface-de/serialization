Serialization
=============

![https://github.com/cyface-de/serialization/actions/workflows/gradle_build.yml](https://github.com/cyface-de/serialization/actions/workflows/gradle_build.yml/badge.svg)
![https://github.com/cyface-de/serialization/actions/workflows/gradle_publish.yml](https://github.com/cyface-de/serialization/actions/workflows/gradle_publish.yml/badge.svg)

This application represents the [Cyface](https://cyface.de) serialization software.

It is used to de-/serialize the Cyface Binary Format which contains traffic data from Cyface measurement devices, such as our sensor box or our smartphone application.

Our smartphone SDK is available as GPL application for [Android](https://github.com/cyface-de/android-backend) and [iOS](https://github.com/cyface-de/ios-sensor-library) as well.

If you require this software under a closed source license for you own projects, please [contact us](https://www.cyface.de/#kontakt).

Changes between versions are found in the [Release Section](https://github.com/cyface-de/serialization/releases).

The project uses [Gradle](https://gradle.org/) as the build system.
It is set up as a Gradle multi project build.
The following sections provide an overview of the details of each sub-projects.

Overview
========

* [Model](#Model)
* [Serializer](#Serializer)
* [Deserializer](#Deserializer)
* [Development](#Development)
* [GitHub Workflows](#GitHub-Workflows)
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

The following transportation modes are supported as the value of a modality type changed event.
The serialized string is the identifier expected in the event value, while the enum constant is the
representation used inside the deserialized model (`de.cyface.model.Modality`):

| Enum constant | Serialized identifier |
|---------------|-----------------------|
| `BICYCLE`     | `BICYCLE`             |
| `EBIKE`       | `EBike`               |
| `CARGO_BIKE`  | `CargoBike`           |
| `ECARGO_BIKE` | `ECargoBike`          |
| `CAR`         | `CAR`                 |
| `MOTORBIKE`   | `MOTORBIKE`           |
| `BUS`         | `BUS`                 |
| `TRAIN`       | `TRAIN`               |
| `WALKING`     | `WALKING`             |
| `UNKNOWN`     | `UNKNOWN`             |

Note that the serialized identifier is not always identical to the enum constant — for example
`EBIKE` is persisted as `EBike` for compatibility with the capturing applications.

The following legacy identifiers are accepted on read and normalized to their canonical constant
(they are never written):

| Legacy identifier | Resolved to |
|-------------------|-------------|
| `Bike`            | `BICYCLE`   |
| `Other`           | `UNKNOWN`   |

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

Deserializer
============

Decodes the Cyface Binary Format back into the model classes. It reads raw binary streams (`BinaryFormatDeserializer`), phone export zip files (`ZippedPhoneDataDeserializer`) and extracted SQLite databases of the phone (`UnzippedPhoneDataDeserializer`). `DeserializerFactory` creates the correct deserializer.

Module dependencies: `deserializer -> serializer -> model`. The serializer exports the Protobuf classes with `api`.

Development
===========

Java 11 (source and target), Kotlin 2.1.20, Gradle 8.13, Protobuf 3.22.2 (java-lite). The serializer downloads the `.proto` files of [cyface-de/protos](https://github.com/cyface-de/protos) (`cyfaceProtosVersion` in `build.gradle`, now 2.3.0) at build time.

Copy `gradle.properties.template` to `gradle.properties` and add your GitHub credentials.

```bash
./gradlew clean build                  # build all modules and run the tests
./gradlew :libs:model:test             # tests of one module
./gradlew :libs:deserializer:test --tests "de.cyface.deserializer.BinaryFormatDeserializerTest"   # one test class
```

`Measurement` has setters and a constructor without arguments, because Apache Flink needs them for its serialization. Use it as an immutable object.

Tests: JUnit 5 (also parameterized), Hamcrest, Mockito 5. Test names follow `testMethodName_forScenario`, and the tests use the Arrange-Act-Assert pattern. The deserializer tests use binary files and SQLite phone exports in `src/test/resources/`.

GitHub Workflows
================

* `gradle_build.yml` builds and tests each push and pull request to `main` and `release-*`. Pull requests from forks are skipped.
* `gradle_publish.yml` runs on version tags: it publishes the artifacts to GitHub Packages and creates the GitHub release.

Rules of the workflows (hardening of CY-6748): do not use `${{ github.ref_name }}` or other event values directly in a `run:` script, pass them through `env:`; pass secrets through `env:` of the step that needs them; use `runs-on: ubuntu-26.04`, not `ubuntu-latest`.

The organization-wide cleanup in `cyface-de/production` (`.github/workflows/registry_cleanup.yml`) deletes old package versions. A version that a build file of the organization names stays.

Release a new Version
=====================

* The CI sets the `version` in the root `build.gradle` from the tag. Do not change it manually.
* Create an annotated tag with a semantic version (for example `6.0.1`) on `main` and push it. The body of the tag message becomes the release notes.
* The workflow publishes the artifacts to the [Github Registry](https://github.com/cyface-de/serialization/packages) and creates the [GitHub release](https://github.com/cyface-de/serialization/releases).
* A tag with a suffix (for example `6.0.1-beta1`) becomes a pre-release.


Publishing artifacts to GitHub Packages manually
================================================

The artifacts produced by this project are distributed via [GitHubPackages](https://github.com/features/packages).
Before you can publish artifacts you need to rename `gradle.properties.template` to `gradle.properties` and enter your GitHub credentials.
Use a personal access token (classic) with the scope `write:packages`, as described [here](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry#authenticating-to-github-packages).

To publish a new version of an artifact you need to:

1. Increase the version number in the root `build.gradle` file
2. Call `./gradlew publish`

This will upload a new artifact to GitHub packages with the new version.
GitHub Packages will not accept to overwrite an existing version or to upload a lower version.
This project uses [semantic versioning](https://semver.org/).

Licensing
=========

Copyright 2018-2026 Cyface GmbH

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
