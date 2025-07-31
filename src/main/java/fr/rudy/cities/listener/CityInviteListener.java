package fr.rudy.cities.listener;

import fr.rudy.cities.CityRank;
import fr.rudy.cities.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.*;

public class CityInviteListener implements Listener {

    public static final Set<UUID> awaitingInviteInput = new HashSet<>();
    public static final Set<UUID> awaitingPromoteInput = new HashSet<>();
    public static final Set<UUID> awaitingDemoteInput = new HashSet<>();
    public static final Map<UUID, UUID> awaitingConfirmPromote = new HashMap<>();

    private final Main plugin = Main.get();

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();
        UUID senderUUID = sender.getUniqueId();
        String input = ChatColor.stripColor(event.getMessage().trim());

        // 🔸 Invite
        if (awaitingInviteInput.remove(senderUUID)) {
            event.setCancelled(true);

            if (input.equalsIgnoreCase("quitter")) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cInvitation annulée."));
                return;
            }

            Player target = Bukkit.getPlayerExact(input);
            if (target == null || !target.isOnline()) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cJoueur introuvable ou hors ligne."));
                return;
            }

            UUID targetUUID = target.getUniqueId();
            String cityName = plugin.getCityManager().getCityName(senderUUID);
            CityRank senderRank = plugin.getCityManager().getCityRank(senderUUID);

            if (cityName == null || senderRank == null || !(senderRank == CityRank.LEADER || senderRank == CityRank.COLEADER)) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cVous n'avez pas la permission d'inviter."));
                return;
            }

            if (plugin.getCityManager().getCityName(targetUUID) != null) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cCe joueur est déjà dans une ville."));
                return;
            }

            plugin.getPendingInvites().put(targetUUID, cityName);
            target.sendMessage(ChatColor.translateAlternateColorCodes('&', "&bVous avez été invité à rejoindre la ville &d" + cityName + "&b !"));
            target.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7Faites &e/city accept &7pour accepter ou &c/city deny &7pour refuser."));
            target.sendMessage(ChatColor.translateAlternateColorCodes('&', "Invitation envoyée à &d" + target.getName() + "&b."));
            return;
        }

        // 🔸 Promote
        if (awaitingPromoteInput.remove(senderUUID)) {
            event.setCancelled(true);

            if (input.equalsIgnoreCase("quitter")) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cPromotion annulée."));
                return;
            }

            Player target = Bukkit.getPlayerExact(input);
            if (target == null || !target.isOnline()) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cJoueur introuvable ou hors ligne."));
                return;
            }

            UUID targetUUID = target.getUniqueId();
            String city = plugin.getCityManager().getCityName(senderUUID);
            CityRank senderRank = plugin.getCityManager().getCityRank(senderUUID);
            CityRank targetRank = plugin.getCityManager().getCityRank(targetUUID);

            if (city == null || senderRank != CityRank.LEADER) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cSeul le chef peut promouvoir un joueur."));
                return;
            }

            if (!city.equals(plugin.getCityManager().getCityName(targetUUID))) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cCe joueur n’est pas dans votre ville."));
                return;
            }

            if (targetRank == CityRank.COLEADER) {
                awaitingConfirmPromote.put(senderUUID, targetUUID);
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e⚠️ &bVous êtes sur le point de transférer votre rôle de chef à &d" + target.getName() + "&b."));
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7Tapez &a/city confirm &7pour confirmer ou ignorez pour annuler."));
            } else {
                boolean success = plugin.getCityManager().setMember(city, targetUUID, CityRank.COLEADER);
                if (success) {
                    target.sendMessage(ChatColor.translateAlternateColorCodes('&', "&d" + target.getName() + " &best maintenant Sous-chef !"));
                    target.sendMessage(ChatColor.translateAlternateColorCodes('&', "&bVous avez été promu Sous-chef par &d" + sender.getName() + "&b !"));
                } else {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cErreur lors de la promotion."));
                }
            }
            return;
        }

        // 🔸 Demote
        if (awaitingDemoteInput.remove(senderUUID)) {
            event.setCancelled(true);

            if (input.equalsIgnoreCase("quitter")) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cRétrogradation annulée."));
                return;
            }

            Player target = Bukkit.getPlayerExact(input);
            if (target == null || !target.isOnline()) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cJoueur introuvable ou hors ligne."));
                return;
            }

            UUID targetUUID = target.getUniqueId();
            String city = plugin.getCityManager().getCityName(senderUUID);
            CityRank senderRank = plugin.getCityManager().getCityRank(senderUUID);
            CityRank targetRank = plugin.getCityManager().getCityRank(targetUUID);

            if (city == null || senderRank != CityRank.LEADER) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cSeul le chef peut rétrograder un joueur."));
                return;
            }

            if (!city.equals(plugin.getCityManager().getCityName(targetUUID))) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cCe joueur n’est pas dans votre ville."));
                return;
            }

            if (targetUUID.equals(senderUUID)) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cVous ne pouvez pas vous rétrograder vous-même."));
                return;
            }

            if (targetRank == null || targetRank == CityRank.MEMBER) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cCe joueur ne peut pas être rétrogradé davantage."));
                return;
            }

            boolean success = plugin.getCityManager().setMember(city, targetUUID, CityRank.MEMBER);
            if (success) {
                target.sendMessage(ChatColor.translateAlternateColorCodes('&', "Le joueur &d" + target.getName() + " &ba été rétrogradé au rang &dMembre&b."));
                target.sendMessage(ChatColor.translateAlternateColorCodes('&',"&cVous avez été rétrogradé au rang &fMembre &cdans la ville &d" + city + "&c."));
            } else {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cErreur lors de la rétrogradation."));
            }
        }
    }
}