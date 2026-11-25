# Initial repository baseline

The starting point was commit [1ff7a4232abab5e441930e43a0673a38dbdda99a](https://github.com/rusanoph/clastor-distributed-lock/tree/1ff7a4232abab5e441930e43a0673a38dbdda99a). It contains the Java domain contracts, two ZooKeeper adapters, a Spring Boot REST application, a Compose environment and the former version rotation script.

## Reproduced before changes

- Gradle 8.14 with JDK 21 failed in `:adapter:zookeeper:javadoc` because Javadoc interpreted `<name>` in a path as an HTML tag.
- Compose emitted warnings and injected empty variables when run without environment overrides.
- A second successful acquire could overwrite the first handle in the controller. An exception during release dropped the handle from the registry. Both cases have controller regression tests.
- The original README documented a different route shape and claimed safe online rotation. The rotation script had a gap between observing an empty tree and switching versions.

## Scope of the first pass

This pass covers the initial M0 build, documentation, packaging and CI work plus targeted correctness fixes. The build runs controller regressions and tests backed by an embedded ZooKeeper server, including the raw ZooKeeper REST lifecycle and metadata reads for both adapters. The CI workflow adds a container smoke test.

The backlog in the local `task/` bundle tracks the remaining work. Open protocol issues include release authorization (CL-018), Curator thread affinity (CL-019), ambiguous creation and session-loss handling (CL-023, CL-024), and safe online version rotation (CL-029).

## M0 verification

| Check | Local evidence |
| --- | --- |
| Build and dependencies | JDK 21 with the Gradle 8.14 wrapper, version catalog and checked-in lockfiles. `clean build` runs Checkstyle and the embedded ZooKeeper tests. |
| API and examples | The HTTP integration test reads route paths from OpenAPI and checks acquire, release, held keys, metadata, timeout and invalid input. |
| Packaging | Built the image with pinned base images. In Compose, both instances reached HTTP readiness on the same image under UID `10001`. While one instance held the key, the other returned `FAILED`. After release, the other instance acquired and released it. |
| Candidate release | Version `0.1.0-alpha.1`, executable JAR, release notes and CI checksum step. [Candidate notes](release-notes.md) |

The milestone requires a published experimental prerelease and required remote CI checks. Repository administration covers branch and tag protection, GitHub private vulnerability reporting and published artifact verification.

The maintainer scoped M0 to this repository. The separate `clastor-lab` bootstrap moves to M1 with CL-036. GitHub private vulnerability reporting is the initial channel. A separate reporting address or form is deferred from CL-016.
