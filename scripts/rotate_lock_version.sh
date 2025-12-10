#!/usr/bin/env bash
set -euo pipefail

ZK_ADDR=${ZK_ADDR:-"localhost:2181"}

LOCK_KIND=${1:-"transaction"}
OLD_VER=${2:-"v1"}
NEW_VER=${3:-"v2"}

ROOT="/locks/zk-provider"

RESOURCE_ROOT="${ROOT}/${LOCK_KIND}"
OLD_ROOT="${RESOURCE_ROOT}/${OLD_VER}"
NEW_ROOT="${RESOURCE_ROOT}/${NEW_VER}"

VERSION_NODE="${RESOURCE_ROOT}/version"

echo "Rotating lock namespace '${LOCK_KIND}' from ${OLD_VER} to ${NEW_VER} on ${ZK_ADDR}"
echo "OLD_ROOT=${OLD_ROOT}"
echo "NEW_ROOT=${NEW_ROOT}"
echo "VERSION_NODE=${VERSION_NODE}"

### 1. Create /locks/zk-provider/{LOCK_KIND}/{NEW_VER}

echo "Ensuring ${NEW_ROOT} exists..."
zkCli.sh -server "${ZK_ADDR}" <<EOF >/dev/null || true
create ${ROOT} ""
create ${RESOURCE_ROOT} ""
create ${NEW_ROOT} ""
EOF

### 2. Waiting until there are no active resource-id nodes under OLD_ROOT

echo "Waiting for all ephemeral lock nodes under ${OLD_ROOT} to disappear..."

while true; do
  RAW_RESOURCES=$(echo "ls ${OLD_ROOT}" | zkCli.sh -server "${ZK_ADDR}" 2>/dev/null || true)

  # Берём строку со списком детей, но НЕ промпт [zk: ...]
  RESOURCE_LINE=$(echo "${RAW_RESOURCES}" | grep '^\[' | grep -v 'zk:' | head -n1 || true)

  if [[ -z "${RESOURCE_LINE}" ]]; then
    echo "No resource nodes under ${OLD_ROOT}, assuming empty."
    break
  fi

  # [id1, id2] -> "id1 id2"
  RESOURCES=$(echo "${RESOURCE_LINE}" | sed -e 's/^\[//' -e 's/\]$//' -e 's/,//g' -e 's/^[[:space:]]*//' )

  ACTIVE=0

  for u in ${RESOURCES}; do
    RESOURCE_PATH="${OLD_ROOT}/${u}"

    RAW_CHILDREN=$(echo "ls ${RESOURCE_PATH}" | zkCli.sh -server "${ZK_ADDR}" 2>/dev/null || true)
    CHILDREN_LINE=$(echo "${RAW_CHILDREN}" | grep '^\[' | grep -v 'zk:' | head -n1 || true)

    if [[ -z "${CHILDREN_LINE}" || "${CHILDREN_LINE}" == "[]" ]]; then
      continue
    fi

    ACTIVE=1
    echo "Still has children under ${RESOURCE_PATH}: ${CHILDREN_LINE}"
    break
  done

  if [[ "${ACTIVE}" -eq 0 ]]; then
    echo "No children with locks under ${OLD_ROOT}, safe to delete."
    break
  fi

  sleep 5
done

### 3. Update /locks/zk-provider/{LOCK_KIND}/version to {NEW_VER}

echo "Updating ${VERSION_NODE} to ${NEW_VER}..."
zkCli.sh -server "${ZK_ADDR}" <<EOF >/dev/null || true
create ${VERSION_NODE} "${NEW_VER}"
set ${VERSION_NODE} "${NEW_VER}"
EOF

### 4. Revert old subtree '/locks/zk-provider/{LOCK_KIND}/{OLD_VER}'

echo "Deleting old subtree ${OLD_ROOT}..."
echo "deleteall ${OLD_ROOT}" | zkCli.sh -server "${ZK_ADDR}" >/dev/null || \
echo "rmr ${OLD_ROOT}"      | zkCli.sh -server "${ZK_ADDR}" >/dev/null || true

echo "Rotation completed."
