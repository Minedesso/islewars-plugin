package de.minedesso.islewars.trigger.command;

import de.minedesso.islewars.application.service.CountdownService;
import de.minedesso.islewars.util.Message;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class StartCommand implements CommandExecutor {
    private final CountdownService countdownService;

    public StartCommand(CountdownService countdownService) {
        this.countdownService = countdownService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Message.ERROR.with(
                    "Dieser Befehl kann nur von einem Spieler ausgeführt werden."));
            return true;
        }
        if (!argsAreEmpty(args)) {
            player.sendMessage(Message.ERROR.with("Verwendung: /start"));
            return true;
        }

        CountdownService.ForceStartResult result = this.countdownService.forceStart(player.getName());
        switch (result) {
            case NOT_READY -> player.sendMessage(Message.ERROR.with(
                    "Die IsleWars-Lobby ist noch nicht bereit."));
            case NOT_ENOUGH_PLAYERS -> player.sendMessage(Message.ERROR.with(
                    "Zum Starten werden mindestens zwei Spieler benötigt – dir fehlt noch ein Gegner."));
            case ALREADY_RUNNING -> player.sendMessage(Message.WARNING.with(
                    "Der verkürzte Countdown läuft bereits."));
            case ALREADY_STARTED -> player.sendMessage(Message.WARNING.with(
                    "Der Spielstart wurde bereits simuliert."));
            case STARTED -> {
            }
        }
        return true;
    }

    private static boolean argsAreEmpty(String[] args) {
        return args == null || args.length == 0;
    }
}
