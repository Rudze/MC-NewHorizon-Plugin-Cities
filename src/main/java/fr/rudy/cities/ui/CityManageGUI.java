package fr.rudy.cities.ui;

import fr.rudy.cities.Main;
import fr.rudy.cities.manager.CityManager;
import fr.rudy.cities.CityRank;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class CityManageGUI {

    private final CityManager cityManager = Main.get().getCityManager();

    public void open(Player player) {
        UUID uuid = player.getUniqueId();
        String city = cityManager.getCityName(uuid);
        CityRank rank = cityManager.getCityRank(uuid);

        Inventory gui = Bukkit.createInventory(null, 54, ":offset_-48::phone_menu::offset_-251::mycity_menu:");

        // Boutons principaux
        gui.setItem(0, createItem("§7Retour"));
        gui.setItem(8, createItem("§bWiki & Guide"));

        gui.setItem(7, createItem(
                rank == CityRank.LEADER ? "§4Supprimer la ville" : "§4Quitter la ville"
        ));

        // Protéger / Libérer
        ItemStack protect = createItem("§7Protéger",
                "§f Protéger une zone",
                "§f Libérer une zone"
        );
        gui.setItem(37, protect);
        gui.setItem(38, protect);
        gui.setItem(39, protect);

        // Spawn
        ItemStack spawn = createItem("§7Placer le spawn", "§f Définir le point d’apparition");
        gui.setItem(19, spawn);
        gui.setItem(20, spawn);
        gui.setItem(21, spawn);

        // Modifier bannière
        ItemStack modif = createItem("§7Modifier la bannière");
        gui.setItem(28, modif);
        gui.setItem(29, modif);
        gui.setItem(30, modif);

        // Membres avec têtes
        int[] memberSlots = {23, 24, 25, 32, 33, 34, 41, 42};
        List<UUID> members = cityManager.getSortedMembersByRank(city);
        for (int i = 0; i < Math.min(memberSlots.length, members.size()); i++) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(members.get(i));
            CityRank memberRank = cityManager.getCityRank(members.get(i));
            gui.setItem(memberSlots[i], createPlayerHead(target, "§7" + memberRank.getDisplayName() + " §f" + target.getName()));
        }

        gui.setItem(43, createItem("§aAjouter un membre"));

        player.openInventory(gui);
    }

    private ItemStack createItem(String name, String... lore) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore.length > 0) meta.setLore(Arrays.asList(lore));
        meta.setCustomModelData(10077);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPlayerHead(OfflinePlayer player, String displayName) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        meta.setOwningPlayer(player);
        meta.setDisplayName(displayName);
        skull.setItemMeta(meta);
        return skull;
    }
}