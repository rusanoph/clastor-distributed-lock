# Clastor Distributed Lock Service

Clastor is an **experimental** Java service for exclusive locks backed by ZooKeeper. The REST service uses the raw ZooKeeper adapter by default. The Curator adapter uses a thread-owned mutex and is suitable for same-thread Java callers.

## Quickstart

To build and test locally, install JDK 21 and run one of these commands from the repository root.

```bash
sh ./gradlew clean build
```

```powershell
.\gradlew.bat clean build
```

To start the service and a local ZooKeeper with Docker Compose v2, run:

```bash
docker compose -f app/docker-compose.yml up --build -d
```

The image builds the application from source. The REST API is available on `127.0.0.1:8079` after startup. Add `--profile cluster` before `up` to start a second app instance on `127.0.0.1:8078` against the same ZooKeeper. Send release to the instance that handled acquire. Stop the environment with `docker compose -f app/docker-compose.yml down`.

## REST API

| Method and path | Result |
| --- | --- |
| `POST /api/v1/locks/{resource}/acquire?resourceId={id}&timeoutMs=5000` | `ACQUIRED` or `FAILED` |
| `POST /api/v1/locks/{resource}/release?resourceId={id}` | `RELEASED` or `NOT_HELD` |
| `GET /api/v1/locks/{resource}/version` | ZooKeeper path and version for the resource. Initializes absent metadata to `v1`. |
| `GET /api/v1/locks/held` | Keys held by this service instance as `resource/resourceId`. |

`timeoutMs` defaults to 5000 and controls the acquire wait. `0` checks without waiting for a predecessor. Backend calls may take additional time. Resource names and IDs start with an ASCII letter, digit, underscore or hyphen. Subsequent characters may also include dots. Invalid inputs return HTTP 400. Acquire and release outcomes use HTTP 200 with a `status` field. Backend exceptions surface as HTTP errors.

```bash
curl -X POST 'http://127.0.0.1:8079/api/v1/locks/jobs/acquire?resourceId=job-1'
curl 'http://127.0.0.1:8079/api/v1/locks/held'
curl -X POST 'http://127.0.0.1:8079/api/v1/locks/jobs/release?resourceId=job-1'
```

On Windows, use `curl.exe` for these examples.

The first and last requests return `ACQUIRED` and `RELEASED`. The middle request returns `["jobs/job-1"]`. For the complete request and response schema, see [OpenAPI](docs/openapi.yaml).

## Configuration

| Environment variable | Default |
| --- | --- |
| `DISTRIBUTED_LOCK_PROVIDER` | `zookeeper`. `curator` selects the thread-owned adapter. |
| `CONNECTION_STRING` | `localhost:2181` for the JAR. Compose supplies `zookeeper:2181`. |
| `ROOT_PATH` | `/locks/zk-provider` or `/locks/curator-provider` |
| `CONNECTION_TIMEOUT` | `5s` |
| `SESSION_TIMEOUT` | `10s` |
| `ACQUIRE_RETRY_DELAY` | `200ms` for raw ZooKeeper |

Backend-specific variables take precedence over the shared variables. See [application.yml](app/src/main/resources/application.yml).

## Modules and limitations

- `domain` defines the Java lock contracts. `adapter/zookeeper` and `adapter/curator` implement them. `app` wires one adapter into Spring Boot and stores acquired handles in process memory.
- The raw ZooKeeper adapter queues ephemeral sequential nodes under `{root}/{resource}/{version}/{resourceId}/lock-…`. Both adapters read version metadata from `{root}/{resource}/version`. Keep each provider on its own tree.
- Use the ZooKeeper REST workflow in a trusted local environment. The API has no caller authentication or ownership check on release. The held list reflects local handles and can become stale after session loss. Curator release requires the acquiring thread.
- Live rotation, fencing, and recovery of ambiguous ZooKeeper operations remain open work. The former rotation script was removed because it allowed new acquisitions during a version switch.

See the [initial baseline](docs/baseline.md) for the original snapshot and the scope of this pass. The local planning bundle under `task/` contains ADR-0001 and the M0–M5 backlog.

[Architecture](docs/architecture.md) · [Contributing](CONTRIBUTING.md) · [Governance](docs/governance.md) · [Security policy](SECURITY.md) · [Release notes](docs/release-notes.md)

## License

[MIT](LICENSE)
