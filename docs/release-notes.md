# 0.1.0-alpha.1

This candidate targets local experimentation with the ZooKeeper REST workflow. It includes a Java 21 server JAR, a container build, and a Compose environment.

- Acquiring the same key on one server instance preserves the original handle.
- ZooKeeper version data is initialized only when absent. Invalid metadata and failed reads surface as errors.
- The online rotation utility has been removed. Keep existing versions fixed while serving requests.
- The REST API uses `/api/v1/locks` and query parameter `resourceId`. See [OpenAPI](openapi.yaml).

The app's lock registry is process-local. Release authorization, session recovery, fencing and online rotation remain open. Curator handles remain bound to the acquiring thread.

## Candidate verification

Run `sh ./gradlew clean build` with JDK 21. The server JAR is `app/build/libs/app.jar`. CI checks the Compose stack and collects the JAR and its SHA-256 checksum. The Compose quickstart exercises the same API against a container built from the current source. [Quickstart](../README.md#quickstart)

Gradle dependencies are locked per module. To update a dependency, change the catalog and refresh the corresponding lockfile with Gradle's `dependencies --update-locks group:name` before running the full build. Wrapper upgrades must update the ZIP checksum and the wrapper JAR checksum in CI from Gradle's published reference. [Gradle checksums](https://gradle.org/release-checksums/)

Publication requires a reviewed source revision, the versioned tag, a registry digest for the server image, and a check of the published JAR and image. Repository administrators manage required CI checks, tag protection and private vulnerability reporting.

## Publish the prerelease

1. After required CI passes on `main`, create an annotated tag with a GitHub-verified signature on the selected commit. Check that the tag is unused on the remote and push it explicitly. For this version use `v0.1.0-alpha.1`.
2. From **Actions → Prepare prerelease**, run the workflow on `main` and enter the tag. The workflow verifies the signature and source revision, runs the build, publishes the versioned GHCR image and checks that exact image by digest.
3. Download the workflow artifact. Verify `SHA256SUMS` and confirm `manifest.json` identifies the selected commit and image. For public distribution, set the GHCR package visibility and confirm anonymous pull by digest.
4. Create a draft GitHub Release for the existing tag. Attach `app.jar`, `SHA256SUMS` and `manifest.json`. Use these notes and select **This is a pre-release** before publishing. Download and verify the JAR and manifest from the published release.
