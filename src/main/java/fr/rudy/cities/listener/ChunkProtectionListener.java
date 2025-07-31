package fr.rudy.cities.listener;

import fr.rudy.cities.Main;
import fr.rudy.cities.manager.ClaimManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.UUID;

public class ChunkProtectionListener implements Listener {

    private final ClaimManager claimManager = new ClaimManager();
    private final Main plugin = Main.get();

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();
        Location loc = event.getBlock().getLocation();
        String worldName = loc.getWorld().getName();

        // Monde spécial : aucune casse si pas de claim de la ville du joueur
        if (worldName.equalsIgnoreCase("world_newhorizon")) {
            Integer cityId = claimManager.getChunkOwnerId(loc);
            if (cityId == null || cityId != Main.get().getCityManager().getCityId(playerUUID)) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', "&cCe monde est protégé. Vous devez revendiquer ce chunk avec votre ville pour construire ici."));
                return;
            }
        }

        // Autres mondes : règle normale
        if (!checkAccess(playerUUID, loc)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', "&cCe chunk est revendiqué. Vous ne pouvez pas casser ici."));
        }
    }


    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;

        if (!checkAccess(event.getPlayer().getUniqueId(), event.getClickedBlock().getLocation())) {
            event.setCancelled(true);
            //MessageUtil.sendMessage(event.getPlayer(), plugin.getPrefixError(), "&cCe chunk est revendiqué. Vous ne pouvez pas interagir ici.");
        }
    }

    private boolean checkAccess(UUID player, Location loc) {
        Integer cityId = claimManager.getChunkOwnerId(loc);
        if (cityId == null) return true; // Pas de claim ici

        String playerCity = Main.get().getCityManager().getCityName(player);
        if (playerCity == null) return false;

        int playerCityId = Main.get().getCityManager().getCityId(player);
        return playerCityId == cityId;
    }
}