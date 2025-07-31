package fr.rudy.cities.listener;

import fr.rudy.cities.CityRank;
import fr.rudy.cities.Main;
import fr.rudy.cities.manager.CityBankManager;
import fr.rudy.cities.manager.CityManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.*;

public class CityChatListener implements Listener {

    public static final Map<UUID, Location> waitingForCityName = new HashMap<>();
    public static final Set<UUID> cityCreationMode = new HashSet<>();
    public static final Set<UUID> awaitingDeposit = new HashSet<>();
    public static final Set<UUID> awaitingWithdraw = new HashSet<>();

    private final CityManager cityManager = Main.get().getCityManager();
    private final CityBankManager cityBankManager = Main.get().getCityBankManager();
    private final Economy vaultEconomy;

    public CityChatListener() {
        RegisteredServiceProvider<Economy> provider = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (provider == null) {
            Bukkit.getLogger().severe("❌ Vault non trouvé ou non prêt : l'économie de ville est désactivée.");
            vaultEconomy = null;
        } else {
            vaultEconomy = provider.getProvider();
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String msg = ChatColor.stripColor(event.getMessage()).trim();

        if (msg.equalsIgnoreCase("quitter")) {
            if (waitingForCityName.containsKey(uuid)) {
                waitingForCityName.remove(uuid);
                cityCreationMode.remove(uuid);
                player.sendMessage(ChatColor.RED + "Création annulée.");
            }
            if (awaitingDeposit.remove(uuid)) {
                player.sendMessage(ChatColor.RED + "Dépôt annulé.");
            }
            if (awaitingWithdraw.remove(uuid)) {
                player.sendMessage(ChatColor.RED + "Retrait annulé.");
            }
            event.setCancelled(true);
            return;
        }

        if (vaultEconomy == null) return;

        // 💰 Dépôt
        if (awaitingDeposit.contains(uuid)) {
            event.setCancelled(true);
            awaitingDeposit.remove(uuid);

            try {
                double amount = Double.parseDouble(msg);
                if (amount <= 0) {
                    player.sendMessage(ChatColor.RED + "Montant invalide.");
                    return;
                }

                if (!vaultEconomy.has(player, amount)) {
                    player.sendMessage(ChatColor.RED + "Fonds insuffisants.");
                    return;
                }

                int cityId = cityManager.getCityId(uuid);
                if (cityBankManager.deposit(cityId, amount)) {
                    vaultEconomy.withdrawPlayer(player, amount);
                    player.sendMessage(ChatColor.AQUA + "Déposé: " + ChatColor.LIGHT_PURPLE + amount + " pièces");
                } else {
                    player.sendMessage(ChatColor.RED + "Erreur lors du dépôt.");
                }

            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Montant invalide.");
            }
            return;
        }

        // 🏧 Retrait
        if (awaitingWithdraw.contains(uuid)) {
            event.setCancelled(true);
            awaitingWithdraw.remove(uuid);

            try {
                double amount = Double.parseDouble(msg);
                if (amount <= 0) {
                    player.sendMessage(ChatColor.RED + "Montant invalide.");
                    return;
                }

                int cityId = cityManager.getCityId(uuid);
                if (cityBankManager.withdraw(cityId, amount)) {
                    vaultEconomy.depositPlayer(player, amount);
                    player.sendMessage(ChatColor.AQUA + "Retiré: " + ChatColor.LIGHT_PURPLE + amount + " pièces");
                } else {
                    player.sendMessage(ChatColor.RED + "Fonds insuffisants dans la banque de la ville.");
                }

            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Montant invalide.");
            }
            return;
        }

        // 📛 Création ou modification du nom de la ville
        if (!waitingForCityName.containsKey(uuid)) return;

        event.setCancelled(true);
        String cityName = msg;
        Location location = waitingForCityName.remove(uuid);

        if (cityCreationMode.remove(uuid)) {
            if (cityManager.getCityLocation(cityName) != null) {
                player.sendMessage(ChatColor.RED + "Une ville avec ce nom existe déjà.");
                return;
            }

            if (!location.getWorld().getName().equalsIgnoreCase("world_newhorizon")) {
                player.sendMessage(ChatColor.RED + "Créez votre ville dans la zone Survie (via Capitaine Jack).");
                return;
            }

            if (cityManager.createCity(uuid, cityName, location)) {
                player.sendMessage(ChatColor.AQUA + "Ville créée: " + ChatColor.LIGHT_PURPLE + cityName);
            } else {
                player.sendMessage(ChatColor.RED + "Erreur lors de la création.");
            }

        } else {
            String currentCity = cityManager.getCityName(uuid);
            CityRank rank = cityManager.getCityRank(uuid);

            if (currentCity == null || rank == null) {
                player.sendMessage(ChatColor.RED + "Vous n'appartenez à aucune ville.");
                return;
            }

            if (!(rank == CityRank.LEADER || rank == CityRank.COLEADER)) {
                player.sendMessage(ChatColor.RED + "Seul le chef ou le sous-chef peut modifier le spawn.");
                return;
            }

            if (cityManager.updateCitySpawn(currentCity, location)) {
                player.sendMessage(ChatColor.AQUA + "Spawn de la ville mis à jour !");
            } else {
                player.sendMessage(ChatColor.RED + "Erreur lors de la mise à jour du spawn.");
            }
        }
    }
}
