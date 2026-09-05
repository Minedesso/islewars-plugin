package de.minedesso.islewars;

import de.minedesso.islewars.application.lobby.LobbyCoordinator;
import de.minedesso.islewars.application.lobby.FactionSelectionMenu;
import de.minedesso.islewars.application.lobby.LobbyItemService;
import de.minedesso.islewars.application.lobby.LobbyPlayerService;
import de.minedesso.islewars.application.port.out.IsleWarsApiPort;
import de.minedesso.islewars.application.service.BootstrapService;
import de.minedesso.islewars.application.service.CountdownService;
import de.minedesso.islewars.application.service.GameStateService;
import de.minedesso.islewars.application.service.FactionSelectionService;
import de.minedesso.islewars.application.service.ServerRuntimeService;
import de.minedesso.islewars.domain.model.GameState;
import de.minedesso.islewars.domain.state.PreGameStateHandler;
import de.minedesso.islewars.infrastructure.api.JavaNetIsleWarsApiAdapter;
import de.minedesso.islewars.infrastructure.bukkit.BukkitGameStartAction;
import de.minedesso.islewars.infrastructure.bukkit.BukkitLobbyAudience;
import de.minedesso.islewars.infrastructure.bukkit.BukkitTaskScheduler;
import de.minedesso.islewars.infrastructure.config.IsleWarsApiConfiguration;
import de.minedesso.islewars.trigger.command.SetLobbyCommand;
import de.minedesso.islewars.trigger.command.StartCommand;
import de.minedesso.islewars.trigger.listener.LobbyProtectionListener;
import de.minedesso.islewars.trigger.listener.InventoryProtectionListener;
import de.minedesso.islewars.trigger.listener.PlayerLifecycleListener;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class IslewarsPlugin extends JavaPlugin {
    private ExecutorService apiExecutor;
    private BootstrapService bootstrapService;
    private CountdownService countdownService;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();

        final IsleWarsApiConfiguration apiConfiguration;
        try {
            apiConfiguration = IsleWarsApiConfiguration.from(this.getConfig());
        } catch (IllegalArgumentException exception) {
            this.getLogger().severe("Ungültige IsleWars-Konfiguration: " + exception.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        this.apiExecutor = Executors.newFixedThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "islewars-api");
            thread.setDaemon(true);
            return thread;
        });

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(apiConfiguration.connectTimeout())
                .executor(this.apiExecutor)
                .build();
        IsleWarsApiPort apiPort = new JavaNetIsleWarsApiAdapter(httpClient, apiConfiguration);
        BukkitTaskScheduler scheduler = new BukkitTaskScheduler(this);

        GameStateService gameStateService = new GameStateService(
                List.of(new PreGameStateHandler()), GameState.PRE_GAME);
        ServerRuntimeService runtimeService = new ServerRuntimeService();
        FactionSelectionService factionSelectionService = new FactionSelectionService();
        FactionSelectionMenu factionSelectionMenu = new FactionSelectionMenu(runtimeService, factionSelectionService);
        LobbyItemService itemService = LobbyItemService.withTeamSelectionAction(factionSelectionMenu::open);
        LobbyPlayerService playerService = new LobbyPlayerService(runtimeService, itemService);

        this.countdownService = new CountdownService(
                runtimeService,
                new BukkitLobbyAudience(),
                scheduler,
                new BukkitGameStartAction(runtimeService, factionSelectionService, factionSelectionMenu)
        );
        LobbyCoordinator lobbyCoordinator = new LobbyCoordinator(playerService, this.countdownService);

        this.registerCommand("start", new StartCommand(this.countdownService));
        this.registerCommand("setlobby", new SetLobbyCommand(
                apiPort, runtimeService, playerService, lobbyCoordinator, scheduler));
        Bukkit.getPluginManager().registerEvents(
                new PlayerLifecycleListener(runtimeService, playerService, this.countdownService,
                        factionSelectionService, factionSelectionMenu), this);
        Bukkit.getPluginManager().registerEvents(
                new LobbyProtectionListener(gameStateService, itemService, playerService), this);
        Bukkit.getPluginManager().registerEvents(new InventoryProtectionListener(gameStateService), this);

        this.bootstrapService = new BootstrapService(
                apiPort,
                runtimeService,
                scheduler,
                apiConfiguration.retrySeconds() * 20L,
                playerService::isSpawnAvailable,
                lobbyCoordinator::activateReadyLobby,
                this.getLogger()
        );
        this.bootstrapService.start();
        this.getLogger().info("IsleWars wurde im PRE_GAME-State gestartet.");
    }

    @Override
    public void onDisable() {
        if (this.bootstrapService != null) {
            this.bootstrapService.stop();
        }
        if (this.countdownService != null) {
            this.countdownService.shutdown();
        }
        Bukkit.getOnlinePlayers().forEach(player -> player.setCollidable(true));
        if (this.apiExecutor != null) {
            this.apiExecutor.shutdownNow();
        }
    }

    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor) {
        PluginCommand command = Objects.requireNonNull(this.getCommand(name),
                () -> "Command /" + name + " fehlt in plugin.yml");
        command.setExecutor(executor);
    }
}
