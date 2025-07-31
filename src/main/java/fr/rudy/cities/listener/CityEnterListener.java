package fr.rudy.cities.listener;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import fr.rudy.cities.Main;
import fr.rudy.cities.manager.CityManager;
import fr.rudy.cities.manager.ClaimManager;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class CityEnterListener
        implements Listener {
    private final ClaimManager claimManager = new ClaimManager();
    private final CityManager cityManager = Main.get().getCityManager();
    private final Map<Player, String> lastTerritoryMap = new HashMap<Player, String>();

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        this.handleChange(event.getPlayer(), event.getTo());
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        this.handleChange(event.getPlayer(), event.getTo());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        this.handleChange(event.getPlayer(), event.getPlayer().getLocation());
    }

    private void handleChange(Player player, Location to) {
        Object currentTerritory;
        if (to == null) {
            return;
        }
        String worldName = to.getWorld().getName();
        if (worldName.equalsIgnoreCase("world_resource")) {
            String currentTerritory2 = "resource";
            if (!"resource".equals(this.lastTerritoryMap.get(player))) {
                this.lastTerritoryMap.put(player, "resource");
                player.sendTitle("\u00a7f\u00cele des ressources", null, 10, 50, 10);
            }
            return;
        }
        if (worldName.equalsIgnoreCase("world_resource_nether")) {
            String currentTerritory3 = "nether";
            if (!"nether".equals(this.lastTerritoryMap.get(player))) {
                this.lastTerritoryMap.put(player, "nether");
                player.sendTitle("\u00a74Nether", null, 10, 50, 10);
            }
            return;
        }
        if (worldName.equalsIgnoreCase("world_resource_the_end")) {
            String currentTerritory4 = "end";
            if (!"end".equals(this.lastTerritoryMap.get(player))) {
                this.lastTerritoryMap.put(player, "end");
                player.sendTitle("\u00a75The End", null, 10, 50, 10);
            }
            return;
        }
        if (!worldName.equalsIgnoreCase("world_newhorizon")) {
            return;
        }
        if (this.isInSpawnRegion(to)) {
            String currentTerritory5 = "spawn";
            if (!"spawn".equals(this.lastTerritoryMap.get(player))) {
                this.lastTerritoryMap.put(player, "spawn");
                player.sendTitle("\u00a7f\u00cele de NewHorizon", null, 10, 50, 10);
            }
            return;
        }
        Integer cityId = this.claimManager.getChunkOwnerId(to);
        Object object = currentTerritory = cityId == null ? "nature" : "city:" + cityId;
        if (((String)currentTerritory).equals(this.lastTerritoryMap.get(player))) {
            return;
        }
        this.lastTerritoryMap.put(player, (String)currentTerritory);
        if (cityId == null) {
            player.sendTitle("\u00a7fNature", "\u00a77Terre non revendiqu\u00e9e", 10, 50, 10);
        } else {
            String cityName = this.cityManager.getCityNameById(cityId);
            player.sendTitle("\u00a7f" + cityName, "\u00a77Terre revendiqu\u00e9e", 10, 50, 10);
        }
    }

    private boolean isInSpawnRegion(Location loc) {
        RegionManager manager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt((World)loc.getWorld()));
        if (manager == null) {
            return false;
        }
        ApplicableRegionSet set = manager.getApplicableRegions(BukkitAdapter.asBlockVector((Location)loc));
        for (ProtectedRegion region : set) {
            if (!region.getId().equalsIgnoreCase("spawn")) continue;
            return true;
        }
        return false;
    }
}