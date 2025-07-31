package fr.rudy.cities.listener;

import fr.rudy.cities.Main;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pillager;
import org.bukkit.entity.Raider;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RaidTriggerListener implements Listener {

    private final Map<UUID, Long> entryTime = new HashMap<>();
    private final Map<UUID, Integer> entryCity = new HashMap<>();

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Chunk to = event.getTo().getChunk();
        Chunk from = event.getFrom().getChunk();
        if (to.equals(from)) return;

        Integer cityId = Main.get().getClaimManager().getChunkOwnerId(event.getTo());
        if (cityId == null) {
            entryTime.remove(player.getUniqueId());
            entryCity.remove(player.getUniqueId());
            return;
        }

        int claimCount = Main.get().getClaimManager().getClaimCount(cityId);
        if (claimCount < 4) {
            entryTime.remove(player.getUniqueId());
            entryCity.remove(player.getUniqueId());
            return;
        }

        UUID uuid = player.getUniqueId();
        entryTime.put(uuid, System.currentTimeMillis());
        entryCity.put(uuid, cityId);

        // Schedule check in 60s
        new BukkitRunnable() {
            @Override
            public void run() {
                Long start = entryTime.get(uuid);
                Integer cid = entryCity.get(uuid);
                if (start == null || cid == null || cid != cityId) return;
                if (!player.isOnline()) {
                    entryTime.remove(uuid);
                    entryCity.remove(uuid);
                    return;
                }

                // Ensure still in same chunk and still owned by same city
                Chunk current = player.getLocation().getChunk();
                Integer nowCity = Main.get().getClaimManager().getChunkOwnerId(player.getLocation());
                if (current.equals(to) && nowCity != null && nowCity.equals(cityId)) {
                    // Trigger raid
                    String cityName = Main.get().getCityManager().getCityNameById(cityId);
                    Bukkit.broadcastMessage("§c🛡️ Un raid de Pillagers a été déclenché sur la ville §6" + cityName + "§c !");
                    spawnRaidGroup(player.getLocation());
                }

                entryTime.remove(uuid);
                entryCity.remove(uuid);
            }
        }.runTaskLater(Main.get(), 20L * 60);
    }

    private void spawnRaidGroup(Location loc) {
        for (int i = 0; i < 5; i++) {
            Location spawnLoc = loc.clone().add((Math.random() -0.5)*16, 1, (Math.random()-0.5)*16);
            Pillager pillager = (Pillager) loc.getWorld().spawn(spawnLoc, Pillager.class);
            pillager.setTarget(null);
        }
    }
}
