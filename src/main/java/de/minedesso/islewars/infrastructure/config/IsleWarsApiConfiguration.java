package de.minedesso.islewars.infrastructure.config;

import org.bukkit.configuration.file.FileConfiguration;

import java.net.URI;
import java.time.Duration;

public record IsleWarsApiConfiguration(
        URI baseUri,
        String serverId,
        String apiKey,
        Duration connectTimeout,
        Duration requestTimeout,
        long retrySeconds
) {
    public static IsleWarsApiConfiguration from(FileConfiguration configuration) {
        String path = "isle-wars-api.";
        String configuredApiKey = configuration.getString(path + "api-key", "");
        String environmentApiKey = System.getenv("ISLEWARS_API_KEY");
        String apiKey = environmentApiKey == null || environmentApiKey.isBlank()
                ? configuredApiKey
                : environmentApiKey;

        URI baseUri = URI.create(requireText(configuration.getString(path + "base-url"), "base-url"));
        if (baseUri.getScheme() == null || baseUri.getHost() == null) {
            throw new IllegalArgumentException("isle-wars-api.base-url muss eine absolute HTTP(S)-URL sein.");
        }

        long connectTimeoutMillis = Math.max(100L,
                configuration.getLong(path + "connect-timeout-millis", 3000L));
        long requestTimeoutMillis = Math.max(100L,
                configuration.getLong(path + "request-timeout-millis", 5000L));
        long retrySeconds = Math.max(1L, configuration.getLong(path + "retry-seconds", 10L));

        String serverId = requireText(configuration.getString(path + "server-id"), "server-id");
        if (!serverId.matches("[A-Za-z0-9._-]+")) {
            throw new IllegalArgumentException("isle-wars-api.server-id enthält ungültige Zeichen.");
        }

        return new IsleWarsApiConfiguration(
                baseUri,
                serverId,
                apiKey == null ? "" : apiKey.trim(),
                Duration.ofMillis(connectTimeoutMillis),
                Duration.ofMillis(requestTimeoutMillis),
                retrySeconds
        );
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("isle-wars-api." + field + " darf nicht leer sein.");
        }
        return value.trim();
    }
}
