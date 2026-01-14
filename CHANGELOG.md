# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project will eventually adhere to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [unreleased]

## [0.1.12] - 2026-01-14

### Added 🚀

- Version display in system tray menu

### Documentation

- Add installation instructions to README

### Chore 👨‍💻 👩‍💻

- Update kotlinx-serialization-json to v1.9.0
- Update actions/upload-artifact to v6
- Update ktlint to v12.3.0
- Update junit-jupiter to v6
- Update actions/setup-java to v5
- Update actions/checkout to v6
- Update Kotlin and compose 

## [0.1.11] - 2026-01-14

### Fixed

- Fix release workflow triggers (release notes)

## [0.1.10] - 2026-01-14

### Fixed

- Fix release workflow triggers (conveyor version)

## [0.1.9] - 2026-01-14

### Changed

- Update Kotlin and Compose dependencies

### Chore 👨‍💻 👩‍💻

- Renovate: Group JDK updates

## [0.1.8] - 2026-01-14

### Changed

- Update Ktor to v2.3.13

### Chore 👨‍💻 👩‍💻

- GH Actions: Separate triggers for pipeline and release
- GH Actions: Run release after pipeline

## [0.1.7] - 2026-01-13

### Added 🚀

- Conveyor integration for cross-platform packaging (macOS, Windows, Linux)
- Separate release workflow with manual trigger

### Changed

- Simplified CI pipeline by moving release logic to dedicated workflow

### Chore 👨‍💻 👩‍💻

- Add LICENSE

## [0.1.6] - 2026-01-13

The first version where the release worked :)

### Chore 👨‍💻 👩‍💻

- GH Actions: Add release publish permissions
- GH Actions: Do a sparse checkout for the gh cli tool
- GH Actions: Tags should cancel commits, pull requests should cancel branch commits

## [0.1.5] - 2026-01-13

### Chore 👨‍💻 👩‍💻

- GH Actions: Do a sparse checkout for the gh cli tool
- GH Actions: Tags should cancel commits, pull requests should cancel branch commits

## [0.1.4] - 2026-01-13

### Chore 👨‍💻 👩‍💻

- Renovate: Add full mode, timezone, pin of version, vulnerability merging

## [0.1.3] - 2026-01-13

### Chore 👨‍💻 👩‍💻

- Remove build-windows for now

## [0.1.2] - 2026-01-13

### Chore 👨‍💻 👩‍💻

- Add renovate.json

## [0.1.1] - 2026-01-13

### Chore 👨‍💻 👩‍💻

- Add wix toolset for windows msi installer

## [0.1.0] - 2026-01-13

### Added 🚀

- Initial menu bar icon application
- GitLab-CI connection support
- Settings UI, notifications, and failed job display
