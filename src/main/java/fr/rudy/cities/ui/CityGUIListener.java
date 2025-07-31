package fr.rudy.cities.ui;

import fr.rudy.cities.Main;
import fr.rudy.cities.manager.CityManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CityGUIListener implements Listener {

    private final CityManager cityManager = Main.get().getCityManager();

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null || event.getCurrentItem() == null) return;

        ItemStack clicked = event.getCurrentItem();
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        String title = event.getView().getTitle();
        String displayName = meta.getDisplayName();

        // ✅ On ne gère que les menus custom
        if (title.contains(":citylist_menu:") || title.contains(":mycity_menu:")) {
            event.setCancelled(true); // ← Empêche le déplacement seulement dans les GUIs custom
            player.playSound(player.getLocation(), "newhorizon:newhorizon.select", 1f, 1f);

            // 📘 Menu des villes
            if (title.contains(":citylist_menu:")) {
                switch (displayName) {
                    case "§7Retour" -> {
                        player.closeInventory();
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dmenu open phone_menu " + player.getName());
                    }
                    case "§bCréer une ville" -> {
                        player.closeInventory();
                        player.performCommand("city create");
                    }
                    case "§bMa ville" -> {
                        player.closeInventory();
                        new CityManageGUI().open(player);
                    }
                    case "§bWiki & Guide" -> {
                        player.closeInventory();
                        player.performCommand("wiki");
                    }
                    default -> {
                        if (displayName.startsWith("§f")) {
                            String cityName = displayName.substring("§f".length()).trim();
                            player.closeInventory();
                            player.performCommand("city tp " + cityName);
                        }
                    }
                }
            }

            // ⚙️ Menu de gestion de ville
            else if (title.contains(":mycity_menu:")) {
                switch (displayName) {
                    case "§7Retour" -> {
                        player.closeInventory();
                        new CityGUI().openCityList(player);
                    }
                    case "§4Supprimer la ville" -> {
                        player.closeInventory();
                        player.performCommand("city remove");
                    }
                    case "§4Quitter la ville" -> {
                        player.closeInventory();
                        player.performCommand("city leave");
                    }
                    case "§bWiki & Guide" -> {
                        player.closeInventory();
                        player.performCommand("wiki");
                    }
                    case "§7Modifier la bannière" -> {
                        player.closeInventory();
                        player.performCommand("city setbanner");
                    }
                    case "§aAjouter un membre" -> {
                        player.closeInventory();
                        player.performCommand("city invite");
                    }
                    case "§7Placer le spawn" -> {
                        player.closeInventory();
                        player.performCommand("city setspawn");
                    }
                    case "§7Protéger" -> {
                        player.closeInventory();
                        if (event.getClick() == ClickType.LEFT) {
                            player.performCommand("city claim");
                        } else if (event.getClick() == ClickType.RIGHT) {
                            player.performCommand("city unclaim");
                        }
                    }
                }
            }
        }
    }
}