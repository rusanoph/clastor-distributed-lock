# Clastor Distributed Lock Service

Clastor is a modular, extensible, production-grade distributed locking system built around a pluggable backend architecture.  
It is intentionally designed to be simple, explicit, and transparent — while exposing low-level control and advanced capabilities such as version rotation, per-resource namespaces, Curator/ZooKeeper compatibility, and a clean hexagonal architecture.

This repository contains:

- A standalone **lock service application** (REST API now, gRPC planned)
- A pure Java **distributed lock library** decoupled from transport and app layer
- Pluggable backends (currently **ZooKeeper** and **Curator**)
- A versioned locking protocol supporting **safe resource-level schema migrations**
- A clear extension path for alternative backends (etcd, Redis, Consul, SQL-based fences, etc.)

---

## Features

### ✔ Per-resource lock namespaces
Each logical resource (`transaction`, `user`, etc.) has its own isolated lock tree and its own version lifecycle.

Example ZK layout:
```
/locks/zk-provider/{resourceKind}/{version}/{resourceId}/lock-00000...
/locks/zk-provider/{resourceKind}/version
```

This ensures:
- independent version rotation per resource
- no cross-resource interference
- support for heterogeneous feature sets per resource type

---

### ✔ Safe Schema Rotation (Version Migration)
The system supports fully safe version upgrades for a resource:

1. Create the new version subtree
2. Wait until **all ephemeral locks** under the old version disappear
3. Switch the resource’s `version` node
4. Remove the old subtree

This guarantees **zero risk of double-locking** the same `resourceId` across two versions.

---

### ✔ Clean Hexagonal Architecture

**Domain layer:**
- `DistributedLock`
- `DistributedLockManager`
- `DistributedLockVersionAccessor`

**Infrastructure layer (pluggable):**
- ZooKeeper backend
- Curator backend
- Future: etcd / Consul / Redis / JDBC fencing tokens

**Application layer:**
- Spring Boot REST API
- gRPC API (planned)
- Optional deployment as a library (drop-in integration)

---

### ✔ REST API (current)
Examples:
```
POST /api/v1/locks/{resource}/{id}/acquire?timeoutMs=5000
POST /api/v1/locks/{resource}/{id}/release
GET /api/v1/locks/{resource}/version
GET /api/v1/locks/held
```

---

### ✔ Fully Local Development Environment

Using docker-compose:

- ZooKeeper
- ZooNavigator UI
- 2–N instances of the lock service
- Shared ZK network for cluster simulation

---

## Project Structure

- app/ → Spring Boot application (REST)
- domain/ → Domain layer (pure Java)
- adapter/zookeeper/ → Raw ZooKeeper backend
- adapter/curator/ → Curator backend
- scripts/ → Admin utilities, including safe rotation tool

---

## Getting Started

### 1. Build

```bash
./gradlew clean build
```

### 2. Run with Docker
```bash
docker compose up --build
```

The service will start on multiple ports (8078/8079 by default), connecting to ZooKeeper at zk:2181.

---

## Example: Acquiring a Lock
```bash
curl -X POST http://localhost:8079/locks/resource/rsc-123/acquire
```


If successful:
```json
{
    "resource": "resource",
    "resourceId": "tx-123",
    "status": "ACQUIRED"
}
```

---

## Safe Version Rotation Script

The repository contains a portable bash script:
```bash
scripts/rotate_lock_version.sh {resource-name} {old-version} {new-version}
```

It performs:
1. Creation of {resource}/{new-version}
2. Waiting for draining of {resource}/{old-version}
3. Switching {resource}/version
4. Removal of the old subtree

It strictly guarantees no double-locking across versions.

---

## TODO / Roadmap

### 🔧 Fixes & Enhancements (near-term)
- Add resource mode: (normal, draining) to block new locks during rotation (prevents accidental acquisition while waiting for draining)
- Expose ZK connectivity and lock metrics via actuator/Prometheus
- Expand logging: distributed trace ID propagation

### 🚀 API & Protocol Extensions
- gRPC API (streaming and unary RPCs)
- Binary RPC protocol for extremely low latency (Netty direct)
- Optional async/reactive API for lock acquisition

### 🧩 Backend Plug-ins

Support additional backends:
- etcd (native lease + revision fencing)
- Redis RedLock (with optional strong fencing tokens)
- Consul Sessions
- PostgreSQL advisory locks (for monolith / hybrid workloads)
- In-memory cluster backend for tests

Unified backend SPI:
```text
DistributedLockManager
DistributedLockVersionAccessor
LockSchemaManager (planned)
```

### 📦 Library Mode

Allow using the lock engine without running the app, via:
```gradle
implementation("io.sagittarius.simplelock:distributed-lock-core")
```

This enables embedding inside other systems (routing engines, schedulers, state machines, etc).

### 🏗 Architectural Improvements
- Add LockMode (exclusive/shared) — optional
- Add fencing tokens for linearizable safety under partial failures
- Add quorum-based mode (if backend supports it — etcd/ZK multi-node ensembles)

---

## Contributing

Contributions, proposals, and backend implementations are welcome.
The repository is intentionally designed to be hackable and easy to extend.

--- 

## 📄 License

MIT — see [LICENSE](./LICENSE).