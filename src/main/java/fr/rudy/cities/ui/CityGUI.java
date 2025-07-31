package fr.rudy.cities.ui;

import fr.rudy.cities.Main;
import fr.rudy.cities.manager.CityBankManager;
import fr.rudy.cities.manager.CityManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.io.BukkitObjectInputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.*;

public class CityGUI {

    private static final int GUI_SIZE = 54;
    private final Connection database;
    private final CityManager cityManager;
    private final CityBankManager bankManager;


    public CityGUI() {
        this.database = Main.get().getDatabase();
        this.cityManager = Main.get().getCityManager();
        this.bankManager = Main.get().getCityBankManager(); // <- ajoute ça
    }

    public void openCityList(Player player) {
        Inventory gui = Bukkit.createInventory(null, GUI_SIZE, ":offset_-48::phone_menu::offset_-251::citylist_menu:");

        UUID uuid = player.getUniqueId();
        boolean inCity = cityManager.getCityName(uuid) != null;

        // Slot 1 : Retour (/menu)
        gui.setItem(0, createCustomPaperItem("§7Retour"));

        // Slot 3/5/6/13/14/15 : Ma ville ou Créer une ville
        String label = inCity ? "§bMa ville" : "§bCréer une ville";
        for (int slot : new int[]{3, 4, 5, 12, 13, 14}) {
            gui.setItem(slot, createCustomPaperItem(label));
        }

        // Slot 9 : Wiki
        gui.setItem(8, createCustomPaperItem("§bWiki & Guide"));

        // Slots de classement
        int[] slots = {19,20,21,22,23,24,25,28,29,30,31,32,33,34,37,38,39,40,41,42,43};
        List<CityEntry> cities = getCitiesSortedByLikes();
        for (int i = 0; i < Math.min(cities.size(), slots.length); i++) {
            CityEntry entry = cities.get(i);
            ItemStack banner = getCityBanner(entry.cityName);

            BannerMeta meta = (BannerMeta) banner.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§f" + entry.cityName); // Nom simple en blanc

                OfflinePlayer owner = Bukkit.getOfflinePlayer(entry.ownerUUID);
                double bank = bankManager.getBalance(cityManager.getCityId(entry.ownerUUID));

                List<String> lore = new ArrayList<>();
                lore.add("§f 👑" + translateHex("e6fff3") + " Chef " + translateHex("8c8c8c") + ": " + translateHex("ffacd5") + (owner.getName() != null ? owner.getName() : "Inconnu"));
                lore.add("§f ⭐" + translateHex("e6fff3") + " Likes " + translateHex("8c8c8c") + ": " + translateHex("ffacd5") + entry.likes);
                lore.add("§a $" + translateHex("e6fff3") + "  Banque" + translateHex("8c8c8c") + " : " + translateHex("ffacd5") + String.format("%.2f", bank));

                meta.setLore(lore);
                banner.setItemMeta(meta);
            }

            gui.setItem(slots[i], banner);
        }

        player.openInventory(gui);
    }

    private ItemStack createCustomPaperItem(String name, String... lore) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        meta.setCustomModelData(10077);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack getCityBanner(String cityName) {
        try (PreparedStatement ps = database.prepareStatement(
                "SELECT banner FROM cities WHERE city_name = ?")) {
            ps.setString(1, cityName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String encoded = rs.getString("banner");
                if (encoded != null && !encoded.isEmpty()) {
                    return deserializeBanner(encoded);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new ItemStack(Material.WHITE_BANNER);
    }

    private ItemStack deserializeBanner(String base64) {
        try {
            byte[] data = Base64.getDecoder().decode(base64);
            BukkitObjectInputStream in = new BukkitObjectInputStream(new ByteArrayInputStream(data));
            Object obj = in.readObject();
            if (obj instanceof ItemStack stack) return stack;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return new ItemStack(Material.WHITE_BANNER);
    }

    private List<CityEntry> getCitiesSortedByLikes() {
        List<CityEntry> cities = new ArrayList<>();
        try (PreparedStatement ps = database.prepareStatement(
                "SELECT city_name, owner_uuid, likes FROM cities ORDER BY likes DESC LIMIT 54");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                cities.add(new CityEntry(
                        rs.getString("city_name"),
                        UUID.fromString(rs.getString("owner_uuid")),
                        rs.getInt("likes")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return cities;
    }

    private static class CityEntry {
        String cityName;
        UUID ownerUUID;
        int likes;

        CityEntry(String cityName, UUID ownerUUID, int likes) {
            this.cityName = cityName;
            this.ownerUUID = ownerUUID;
            this.likes = likes;
        }
    }

    private String translateHex(String hex) {
        return "§x§" + hex.charAt(0) +
                "§" + hex.charAt(1) +
                "§" + hex.charAt(2) +
                "§" + hex.charAt(3) +
                "§" + hex.charAt(4) +
                "§" + hex.charAt(5);
    }

}