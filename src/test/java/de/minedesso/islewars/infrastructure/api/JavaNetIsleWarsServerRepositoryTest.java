package de.minedesso.islewars.infrastructure.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import de.minedesso.islewars.domain.model.IsleWarsMode;
import de.minedesso.islewars.domain.model.LobbySpawn;
import de.minedesso.islewars.domain.model.ServerConfiguration;
import de.minedesso.islewars.infrastructure.config.IsleWarsApiConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaNetIsleWarsServerRepositoryTest {
    private HttpServer server;
    private JavaNetIsleWarsServerRepository repository;
    private final AtomicReference<String> receivedApiKey = new AtomicReference<>();
    private final AtomicReference<String> receivedPutBody = new AtomicReference<>();

    @BeforeEach
    void setUp() throws IOException {
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        this.server.createContext("/api/v1/servers/test-server", this::handleServerEndpoint);
        this.server.createContext("/api/v1/servers/test-server/lobby-spawn", this::handleSpawnEndpoint);
        this.server.start();

        IsleWarsApiConfiguration configuration = new IsleWarsApiConfiguration(
                URI.create("http://127.0.0.1:" + this.server.getAddress().getPort()),
                "test-server",
                "islewars-secret",
                Duration.ofSeconds(1),
                Duration.ofSeconds(2),
                10
        );
        this.repository = new JavaNetIsleWarsServerRepository(HttpClient.newHttpClient(), configuration);
    }

    @AfterEach
    void tearDown() {
        this.server.stop(0);
    }

    @Test
    void fetchesCombinedConfigurationAndIgnoresUnknownFields() {
        ServerConfiguration result = this.repository.fetchServerConfiguration().join();

        assertEquals(IsleWarsMode.FOUR_BY_TWO, result.mode());
        assertEquals(3, result.minimumPlayers());
        assertEquals(8, result.maximumPlayers());
        assertEquals(60, result.countdownSeconds());
        assertEquals("world", result.lobbySpawn().worldName());
        assertEquals("islewars-secret", this.receivedApiKey.get());
    }

    @Test
    void persistsLobbySpawnWithDedicatedApiKey() {
        LobbySpawn spawn = new LobbySpawn("arena", 1.5, 64.0, -3.25, 90.0F, 5.0F);

        this.repository.saveLobbySpawn(spawn).join();

        assertEquals("islewars-secret", this.receivedApiKey.get());
        assertTrue(this.receivedPutBody.get().contains("\"worldName\":\"arena\""));
        assertTrue(this.receivedPutBody.get().contains("\"yaw\":90.0"));
    }

    private void handleServerEndpoint(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        this.receivedApiKey.set(exchange.getRequestHeaders().getFirst("X-API-Key"));
        byte[] body = ("{\"mode\":\"4x2\",\"minimumPlayers\":3,\"maximumPlayers\":8,"
                + "\"countdownSeconds\":60,\"unknownFutureField\":true,"
                + "\"lobbySpawn\":{\"worldName\":\"world\",\"x\":1.0,\"y\":64.0,"
                + "\"z\":2.0,\"yaw\":0.0,\"pitch\":0.0}}").getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private void handleSpawnEndpoint(HttpExchange exchange) throws IOException {
        if (!"PUT".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        this.receivedApiKey.set(exchange.getRequestHeaders().getFirst("X-API-Key"));
        this.receivedPutBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        exchange.sendResponseHeaders(204, -1);
        exchange.close();
    }
}
