package io.sagittarius.clastor.distributedlock.app;

import io.sagittarius.clastor.distributedlock.curator.CuratorLockConfig;
import io.sagittarius.clastor.distributedlock.curator.CuratorLockManager;
import io.sagittarius.clastor.distributedlock.domain.DistributedLockManager;
import io.sagittarius.clastor.distributedlock.zookeeper.ZkClient;
import io.sagittarius.clastor.distributedlock.zookeeper.ZookeeperLockConfig;
import io.sagittarius.clastor.distributedlock.zookeeper.ZookeeperLockManager;
import org.apache.curator.test.TestingServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.SpringApplication;
import org.yaml.snakeyaml.Yaml;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ZooKeeperIntegrationTest {

    private static TestingServer server;

    @BeforeAll
    static void startZooKeeper() throws Exception {
        server = new TestingServer();
    }

    @AfterAll
    static void stopZooKeeper() throws Exception {
        if (server != null) {
            server.close();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void readsExistingVersionAndRejectsEmptyMetadata(boolean curator) throws Exception {
        Duration connectionTimeout = Duration.ofSeconds(5);
        Duration sessionTimeout = Duration.ofSeconds(10);
        String root = curator ? "/version-tests/curator" : "/version-tests/zookeeper";
        try (var client = new ZkClient(server.getConnectString(), connectionTimeout, sessionTimeout)) {
            DistributedLockManager manager = curator
                    ? new CuratorLockManager(new CuratorLockConfig(server.getConnectString(), connectionTimeout, sessionTimeout, root))
                    : new ZookeeperLockManager(new ZookeeperLockConfig(server.getConnectString(), connectionTimeout, sessionTimeout, root, Duration.ofMillis(200)));
            try {
                var versions = manager.versionAccessor();
                assertEquals("v1", versions.currentVersion("jobs"));

                var zk = client.getRaw();
                String path = root + "/jobs/version";
                zk.setData(path, "v2".getBytes(StandardCharsets.UTF_8), -1);
                assertEquals("v2", versions.currentVersion("jobs"));

                zk.setData(path, new byte[0], -1);
                assertThrows(IllegalStateException.class, () -> versions.currentVersion("jobs"));
                assertEquals(0, zk.getData(path, false, null).length);
            } finally {
                manager.close();
            }
        }
    }

    @Test
    void runsRestLifecycleAndRemovesTimedOutContender() throws Exception {
        try (var app = SpringApplication.run(ClastorDistributedLockApplication.class,
                "--server.port=0", "--server.address=127.0.0.1", "--CONNECTION_STRING=" + server.getConnectString(),
                "--DISTRIBUTED_LOCK_PROVIDER=zookeeper");
             var http = HttpClient.newHttpClient()) {
            String base = "http://127.0.0.1:" + app.getEnvironment().getProperty("local.server.port");
            var paths = contractPaths();
            String acquire = base + paths.get("acquire").replace("{resource}", "jobs");
            String release = base + paths.get("release").replace("{resource}", "jobs");
            String versionUri = base + paths.get("getVersion").replace("{resource}", "jobs");
            var json = JsonMapper.builder().build();
            var version = http.send(HttpRequest.newBuilder(URI.create(versionUri)).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, version.statusCode());
            assertEquals("v1", json.readTree(version.body()).get("version").stringValue());
            assertEquals("ACQUIRED", postStatus(http, json, acquire + "?resourceId=one&timeoutMs=0"));
            assertEquals("FAILED", postStatus(http, json, acquire + "?resourceId=one"));

            var held = http.send(HttpRequest.newBuilder(URI.create(base + paths.get("listLocalHandles"))).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, held.statusCode());
            assertEquals("jobs/one", json.readTree(held.body()).get(0).stringValue());

            var manager = app.getBean(DistributedLockManager.class);
            try (var contender = manager.lock("jobs", "one")) {
                assertFalse(contender.tryLock(Duration.ofMillis(100)));
                assertEquals("RELEASED", postStatus(http, json, release + "?resourceId=one"));
                assertEquals("ACQUIRED", postStatus(http, json, acquire + "?resourceId=one&timeoutMs=0"));
            }

            assertEquals("RELEASED", postStatus(http, json, release + "?resourceId=one"));
            assertEquals("NOT_HELD", postStatus(http, json, release + "?resourceId=one"));
            assertEquals(400, post(http, acquire + "?resourceId=bad%2Fid").statusCode());
            assertEquals(400, post(http, acquire + "?resourceId=one&timeoutMs=-1").statusCode());
        }
    }

    private static Map<String, String> contractPaths() throws Exception {
        try (var input = Objects.requireNonNull(ZooKeeperIntegrationTest.class.getResourceAsStream("/openapi.yaml"))) {
            Map<String, Object> document = new Yaml().load(input);
            var result = new java.util.HashMap<String, String>();
            var paths = (Map<?, ?>) document.get("paths");
            paths.forEach((path, methods) -> ((Map<?, ?>) methods).values().forEach(operation ->
                    result.put((String) ((Map<?, ?>) operation).get("operationId"), (String) path)));
            return result;
        }
    }

    private static String postStatus(HttpClient client, JsonMapper json, String uri) throws Exception {
        var response = post(client, uri);
        assertEquals(200, response.statusCode(), response.body());
        return json.readTree(response.body()).get("status").stringValue();
    }

    private static HttpResponse<String> post(HttpClient client, String uri) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(uri)).timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.noBody()).build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
