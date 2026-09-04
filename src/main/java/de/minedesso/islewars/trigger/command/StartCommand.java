package de.minedesso.islewars.trigger.command;

import de.minedesso.islewars.application.service.CountdownService;
import org.bukkit.ChatColor;
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
            sender.sendMessage(ChatColor.RED + "Dieser Befehl kann nur von einem Spieler ausgeführt werden.");
            return true;
        }
        if (!argsAreEmpty(args)) {
            player.sendMessage(ChatColor.RED + "Verwendung: /start");
            return true;
        }

        CountdownService.ForceStartResult result = this.countdownService.forceStart(player.getName());
        switch (result) {
            case NOT_READY -> player.sendMessage(ChatColor.RED + "Die IsleWars-Lobby ist noch nicht bereit.");
            case NOT_ENOUGH_PLAYERS -> player.sendMessage(ChatColor.RED
                    + "Zum Starten werden mindestens zwei Spieler benötigt – dir fehlt noch ein Gegner.");
            case ALREADY_RUNNING -> player.sendMessage(ChatColor.YELLOW
                    + "Der verkürzte Countdown läuft bereits.");
            case ALREADY_STARTED -> player.sendMessage(ChatColor.YELLOW
                    + "Der Spielstart wurde bereits simuliert.");
            case STARTED -> {
            }
        }
        return true;
    }

    private static boolean argsAreEmpty(String[] args) {
        return args == null || args.length == 0;
    }
}
