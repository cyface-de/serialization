# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Cyface Serialization library for de-/serializing the Cyface Binary Format, which contains traffic measurement data from smartphones and sensor boxes. The format uses Protobuf serialization with compression (ZLIB, GZIP, or raw DEFLATE depending on the sender platform).

## Build Commands

```bash
./gradlew build                # Full build with tests
./gradlew test                 # Run all tests
./gradlew :libs:deserializer:test --tests "de.cyface.deserializer.factory.BinaryFormatDeserializerTest"  # Single test class
./gradlew :libs:deserializer:test --tests "*.BinaryFormatDeserializerTest.testMethodName"        # Single test method
./gradlew clean build          # Clean rebuild
./gradlew publish              # Publish to GitHub Packages (requires credentials in gradle.properties)
```

Protobuf definitions are auto-downloaded from `cyface-de/protos` during build via the `downloadAndUnzipProto` task.

## Module Structure

Gradle multi-project build with three modules (dependency flows downward):

```
libs/deserializer  →  libs/serializer  →  libs/model
```

- **model** (`libs/model/`): Shared domain objects — `RawRecord`, `GeoLocationRecord`, `Point3DImpl`, `Event`, `Track`, `Modality`, `MeasurementIdentifier`. No external dependencies beyond commons-lang.
- **serializer** (`libs/serializer/`): Serializes data into Cyface Binary Format v3. Entry point is `Serializer` (compression) and `DataSerializable` (binary format). Uses Protobuf java-lite generated from `cyface-de/protos`.
- **deserializer** (`libs/deserializer/`): Deserializes from binary format. Key classes: `BinaryFormatDeserializer` (compressed protobuf), `V3UncompressedBinaryFormatDeserializer` (uncompressed variant), `DeserializerFactory`, `TrackBuilder` (reconstructs tracks from lifecycle events).

## Architecture Notes

- **Binary format**: 2-byte Big Endian version header (`0x0003`), then Protobuf-encoded sections for events, locations, and sensor data (accelerations/rotations/directions).
- **Data encoding**: Locations and sensor data use fixed-point integers (e.g., 6 decimal places for lat/lon) and run-length (delta) encoding for timestamps and sequential values.
- **Compression**: `BinaryFormatDeserializer` tries four formats in order to maximise compatibility across sender platforms:
  1. **GZIP** — detected by magic bytes `0x1F 0x8B`
  2. **ZLIB** — detected by CMF/FLG header check (`nowrap=false`)
  3. **Raw DEFLATE** — same Inflater with `nowrap=true` (iOS/Android cross-platform mode)
  4. **Uncompressed protobuf** — last-resort fallback for partner iOS uploads that send raw protobuf bytes without any compression
- **TrackBuilder**: Slices measurements into `Track` segments using LIFECYCLE events (START/PAUSE/RESUME/STOP) and annotates with `Modality` from MODALITY_TYPE_CHANGE events.

## Tech Stack

- Java 11, Kotlin 2.1.20, Gradle 8.13
- Protobuf 3.22.2 (java-lite), proto definitions v2.3.0 from `cyface-de/protos`
- Protobuf Gradle plugin 0.9.4 — note: 0.9.x registers generated sources into source sets differently from 0.8.x, which requires `duplicatesStrategy = DuplicatesStrategy.EXCLUDE` on the `sourcesJar` task (already configured in `libs/serializer/build.gradle`)
- Testing: JUnit 5, Hamcrest, Mockito
- Preconditions: Apache Commons Lang `Validate`

## Environment Requirements

- **Gradle JVM**: Gradle 8 requires JVM 17 or higher to run; JVM 21 (e.g. via Homebrew on macOS) is recommended.
- **IntelliJ**: Set *Settings → Build → Gradle → Gradle JVM* to the same JDK 21 installation used on the command line, otherwise IntelliJ may invoke Gradle with an older JVM and produce deprecation warnings.

## Conventions

- Test classes use `*Test` suffix; integration tests use `*IT` suffix
- Assertions use Hamcrest matchers (`assertThat(value, is(expected))`)
- Version is `0.0.0` in source; CI overrides from git tag on release
- Semantic versioning; releases are triggered by pushing a version tag

## Testing Conventions

- **Do not mock JDK final classes** (e.g. `UUID`, `Date`) or Kotlin classes (which are `final` by default). Mockito's byte-buddy agent cannot instrument them under the Java 11 module system without `--add-opens` flags that are not configured here. Use real instances instead (e.g. `UUID.randomUUID()`, `new Date()`).
- `junit-platform-launcher` must be declared explicitly as `testRuntimeOnly` in every subproject's `dependencies` block (already in `build.gradle`). Without this, Gradle 8 prints an auto-loading deprecation warning; it will become an error in Gradle 9.

## Javadoc Conventions

- Every public API method must have an opening description sentence before any `@param`/`@return`/`@throws` tags, otherwise Javadoc emits a "Keine Hauptbeschreibung" (no main description) warning.
- Utility classes and classes with only static methods must declare an explicit `private` constructor with a Javadoc comment explaining they are not meant to be instantiated, to avoid the "Verwendung von Standardkonstruktor" (implicit constructor) warning.
- Abstract base classes with protected constructors should declare them explicitly with Javadoc.
- Generated protobuf sources under `de/cyface/protos/` are excluded from the Javadoc task in `libs/serializer/build.gradle` — do not add Javadoc there.
