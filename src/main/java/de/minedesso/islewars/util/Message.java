package de.minedesso.islewars.util;

public enum Message {
    PREFIX("§8[§3§lMinedesso§8] §7» "),
    INFO(PREFIX.message + "§7"),
    SUCCESS(PREFIX.message + "§a"),
    WARNING(PREFIX.message + "§e"),
    ERROR(PREFIX.message + "§c");

    public final String message;

    Message(String message) {
        this.message = message;
    }

    public String with(String content) {
        return this.message + content;
    }
}
