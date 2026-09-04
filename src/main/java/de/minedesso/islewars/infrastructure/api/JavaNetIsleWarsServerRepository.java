package de.minedesso.islewars.infrastructure.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.minedesso.islewars.application.port.out.IsleWarsServerRepository;
import de.minedesso.islewars.domain.model.IsleWarsMode;
import de.minedesso.islewars.domain.model.LobbySpawn;
import de.minedesso.islewars.domain.model.ServerConfiguration;
import de.minedesso.islewars.infrastructure.config.IsleWarsApiConfiguration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class JavaNetIsleWarsServerRepository implements IsleWarsServerRepository {
    private static final String API_KEY_HEADER = "X-API-Key";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final IsleWarsApiConfiguration configuration;
    private final URI serverEndpoint;
    private final URI lobbySpawnEndpoint;

    public JavaNetIsleWarsServerRepository(HttpClient httpClient, IsleWarsApiConfiguration configuration) {
        this(httpClient, configuration, new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false));
    }

    JavaNetIsleWarsServerRepository(
            HttpClient httpClient,
            IsleWarsApiConfiguration configuration,
            ObjectMapper objectMapper
    ) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");

        String baseUrl = configuration.baseUri().toString().replaceAll("/+$", "");
        this.serverEndpoint = URI.create(baseUrl + "/api/v1/servers/" + configuration.serverId());
        this.lobbySpawnEndpoint = URI.create(this.serverEndpoint + "/lobby-spawn");
    }

    @Override
    public CompletableFuture<ServerConfiguration> fetchServerConfiguration() {
        HttpRequest request;
        try {
            request = this.requestBuilder(this.serverEndpoint).GET().build();
        } catch (RuntimeException exception) {
            return CompletableFuture.failedFuture(exception);
        }

        return this.httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(response -> {
                    this.requireStatus(response, 200);
                    try {
                        ServerConfigurationResponse payload = this.objectMapper.readValue(
                                response.body(), ServerConfigurationResponse.class);
                        return payload.toDomain();
                    } catch (JsonProcessingException | IllegalArgumentException exception) {
                        throw new IsleWarsApiException("Ungültige Serverkonfiguration von der IsleWars-API.", exception);
                    }
                });
    }

    @Override
    public CompletableFuture<Void> saveLobbySpawn(LobbySpawn lobbySpawn) {
        Objects.requireNonNull(lobbySpawn, "lobbySpawn");
        final String body;
        try {
            body = this.objectMapper.writeValueAsString(LobbySpawnPayload.from(lobbySpawn));
        } catch (JsonProcessingException exception) {
            return CompletableFuture.failedFuture(
                    new IsleWarsApiException("Lobby-Spawn konnte nicht serialisiert werden.", exception));
        }

        HttpRequest request;
        try {
            request = this.requestBuilder(this.lobbySpawnEndpoint)
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
        } catch (RuntimeException exception) {
            return CompletableFuture.failedFuture(exception);
        }

        return this.httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(response -> {
                    if (response.statusCode() != 200 && response.statusCode() != 204) {
                        throw new IsleWarsApiException("IsleWars-API antwortete mit HTTP " + response.statusCode() + ".");
                    }
                    return null;
                });
    }

    private HttpRequest.Builder requestBuilder(URI uri) {
        if (this.configuration.apiKey().isBlank()) {
            throw new IsleWarsApiException("Für das IsleWars-Backend ist kein X-API-Key konfiguriert.");
        }
        return HttpRequest.newBuilder(uri)
                .timeout(this.configuration.requestTimeout())
                .header("Accept", "application/json")
                .header(API_KEY_HEADER, this.configuration.apiKey());
    }

    private void requireStatus(HttpResponse<String> response, int expectedStatus) {
        if (response.statusCode() != expectedStatus) {
            throw new IsleWarsApiException("IsleWars-API antwortete mit HTTP " + response.statusCode() + ".");
        }
    }

    private record ServerConfigurationResponse(
            String mode,
            int minimumPlayers,
            int maximumPlayers,
            int countdownSeconds,
            LobbySpawnPayload lobbySpawn
    ) {
        private ServerConfiguration toDomain() {
            return new ServerConfiguration(
                    IsleWarsMode.fromApiValue(this.mode),
                    this.minimumPlayers,
                    this.maximumPlayers,
                    this.countdownSeconds,
                    this.lobbySpawn == null ? null : this.lobbySpawn.toDomain()
            );
        }
    }

    private record LobbySpawnPayload(
            String worldName,
            double x,
            double y,
            double z,
            float yaw,
            float pitch
    ) {
        private static LobbySpawnPayload from(LobbySpawn spawn) {
            return new LobbySpawnPayload(spawn.worldName(), spawn.x(), spawn.y(), spawn.z(), spawn.yaw(), spawn.pitch());
        }

        private LobbySpawn toDomain() {
            return new LobbySpawn(this.worldName, this.x, this.y, this.z, this.yaw, this.pitch);
        }
    }
}
