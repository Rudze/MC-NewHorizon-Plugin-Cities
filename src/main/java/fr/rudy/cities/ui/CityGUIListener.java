package fr.rudy.cities.ui;

import fr.rudy.cities.Main;
import fr.rudy.cities.manager.CityManager;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
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
        String displayName = meta.getDisplayName();

        // Gestion du menu de gestion de ville via InventoryHolder
        if (event.getInventory().getHolder() instanceof CityManageInventoryHolder) {
            event.setCancelled(true);
            player.playSound(player.getLocation(), "newhorizon:newhorizon.select", 1f, 1f);

            switch (displayName) {
                case "§7Retour" -> {
                    player.closeInventory();
                    Bukkit.getScheduler().runTaskLater(Main.get(), () -> {
                        new CityGUI().openCityList(player);
                    }, 1L);
                }
                case "§4Supprimer la ville" -> player.performCommand("city remove");
                case "§4Quitter la ville" -> player.performCommand("city leave");
                case "§bWiki & Guide" -> player.performCommand("wiki");
                case "§7Modifier la bannière" -> player.performCommand("city setbanner");
                case "§aAjouter un membre" -> player.performCommand("city invite");
                case "§7Placer le spawn" -> player.performCommand("city setspawn");
                case "§7Protéger" -> {
                    if (event.getClick() == ClickType.LEFT) player.performCommand("city claim");
                    else if (event.getClick() == ClickType.RIGHT) player.performCommand("city unclaim");
                }
            }
            return;
        }

        // Gestion du menu city list
        String title = event.getView().getTitle();
        String cityListTitle = "%nexo_shift_-48%<glyph:citylist>";
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            cityListTitle = PlaceholderAPI.setPlaceholders(player, cityListTitle);
        }

        if (title.equals(cityListTitle)) {
            event.setCancelled(true);
            player.playSound(player.getLocation(), "newhorizon:newhorizon.select", 1f, 1f);

            switch (displayName) {
                case "§7Retour" -> {
                    player.closeInventory();
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dmenu open main_menu " + player.getName());
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
    }
}
