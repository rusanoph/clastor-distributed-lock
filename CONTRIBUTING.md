# Contributing

Use JDK 21. Run `sh ./gradlew clean build` (or `.\gradlew.bat clean build` on Windows) and the Compose quickstart before opening a change that affects startup. The Gradle build runs Checkstyle, unit and embedded ZooKeeper tests.

For a bug report, include the revision, backend, how to reproduce, expected and actual results, and relevant logs. For protocol changes, describe the existing behavior, proposed invariants, failure cases, and client and stored-data compatibility. Document any breaking change and its upgrade path before merging.

Open a pull request with validation results and compatibility impact. Maintainers review changes in the repository. Use the private reporting channel for security issues. [Security policy](SECURITY.md)

The maintainer handles maintenance and release decisions. Use GitHub issues for support and roadmap proposals. Discuss larger changes to the lock protocol in a short decision record before implementing them. [Governance](docs/governance.md) · [ADR template](docs/adr-template.md)
