package de.minedesso.islewars.application.lobby;

import de.minedesso.islewars.application.service.ServerRuntimeService;
import de.minedesso.islewars.domain.model.LobbySpawn;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

public final class LobbyPlayerService {
    private final ServerRuntimeService runtimeService;
    private final LobbyItemService lobbyItemService;

    public LobbyPlayerService(ServerRuntimeService runtimeService, LobbyItemService lobbyItemService) {
        this.runtimeService = runtimeService;
        this.lobbyItemService = lobbyItemService;
    }

    public void prepareLobbyPlayer(Player player) {
        this.resetPlayer(player);
        this.teleportToLobby(player);
        this.lobbyItemService.giveItems(player);
    }

    public void prepareSetupPlayer(Player player) {
        this.resetPlayer(player);
        player.sendMessage("§eDie IsleWars-Lobby ist noch nicht bereit. Setze den Spawn mit /setlobby.");
    }

    public boolean teleportToLobby(Player player) {
        if (!this.runtimeService.isReady()) {
            return false;
        }
        LobbySpawn spawn = this.runtimeService.requireConfiguration().lobbySpawn();
        World world = Bukkit.getWorld(spawn.worldName());
        if (world == null) {
            return false;
        }
        player.setFallDistance(0.0F);
        return player.teleport(new Location(world, spawn.x(), spawn.y(), spawn.z(), spawn.yaw(), spawn.pitch()));
    }

    public boolean isSpawnAvailable(LobbySpawn spawn) {
        return Bukkit.getWorld(spawn.worldName()) != null;
    }

    public Location requireLobbyLocation() {
        LobbySpawn spawn = this.runtimeService.requireConfiguration().lobbySpawn();
        World world = Bukkit.getWorld(spawn.worldName());
        if (world == null) {
            throw new IllegalStateException("Lobby-Welt ist nicht geladen: " + spawn.worldName());
        }
        return new Location(world, spawn.x(), spawn.y(), spawn.z(), spawn.yaw(), spawn.pitch());
    }

    private void resetPlayer(Player player) {
        player.closeInventory();
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        player.setGameMode(GameMode.ADVENTURE);
        AttributeInstance maxHealthAttribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        double maxHealth = maxHealthAttribute == null ? 20.0D : maxHealthAttribute.getValue();
        player.setHealth(Math.min(20.0D, maxHealth));
        player.setFoodLevel(20);
        player.setSaturation(20.0F);
        player.setExhaustion(0.0F);
        player.setFireTicks(0);
        player.setFallDistance(0.0F);
        player.setLevel(0);
        player.setExp(0.0F);
        player.setFlying(false);
        player.setAllowFlight(false);
        player.setCollidable(false);
        player.updateInventory();
    }
}
