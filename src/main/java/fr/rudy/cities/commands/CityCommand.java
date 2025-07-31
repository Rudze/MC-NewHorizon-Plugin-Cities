package fr.rudy.cities.commands;

import fr.rudy.cities.Main;
import fr.rudy.cities.*;
import fr.rudy.cities.listener.CityChatListener;
import fr.rudy.cities.listener.CityInviteListener;
import fr.rudy.cities.manager.CityBankManager;
import fr.rudy.cities.manager.CityManager;
import fr.rudy.cities.ui.CityGUI;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Base64;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.util.io.BukkitObjectOutputStream;


import java.util.UUID;

public class CityCommand implements CommandExecutor {

    private final CityManager cityManager = Main.get().getCityManager();
    private final CityBankManager cityBankManager = Main.get().getCityBankManager();
    private final Economy economy;

    public CityCommand() {
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        this.economy = (rsp != null) ? rsp.getProvider() : null;
    }


    Main plugin = Main.get();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }

        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);

        if (args.length == 0) {
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create":
                if (cityManager.hasCity(uuid) || cityManager.getCityName(uuid) != null) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "Vous êtes déjà dans une ville !"));
                    return true;
                }

                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "Entrez le nom de votre ville dans le chat. Tapez 'quitter' pour annuler."));
                CityChatListener.waitingForCityName.put(uuid, player.getLocation());
                CityChatListener.cityCreationMode.add(uuid);
                break;

            case "setspawn":
                String cityOfPlayer = cityManager.getCityName(uuid);
                CityRank rankOfPlayer = cityManager.getCityRank(uuid);

                if (cityOfPlayer == null || rankOfPlayer == null) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "Vous n'appartenez à aucune ville."));
                    return true;
                }

                if (!(rankOfPlayer == CityRank.LEADER || rankOfPlayer == CityRank.COLEADER)) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "Seul le chef ou le sous-chef peut modifier le spawn."));
                    return true;
                }

                if (cityManager.updateCitySpawn(cityOfPlayer, player.getLocation())) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "Spawn de la ville §d" + cityOfPlayer + " §bmis à jour !"));
                } else {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "Erreur lors de la mise à jour du spawn."));
                }
                break;

            case "tp":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "Utilisation: /city tp <nom>"));
                    return true;
                }

                Location loc = cityManager.getCityLocation(args[1]);
                if (loc != null) {
                    player.teleport(loc);
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "Téléporté à la ville §d" + args[1]));
                } else {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "Cette ville est introuvable."));
                }
                break;

            case "remove":
                String playerCity = cityManager.getCityName(uuid);
                CityRank playerRank = cityManager.getCityRank(uuid);

                if (playerCity == null || playerRank != CityRank.LEADER) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "§cSeul le chef peut supprimer la ville."));
                    return true;
                }

                if (cityManager.removeCity(uuid)) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "§cVotre ville " + playerCity + " a été supprimée."));
                } else {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "Erreur lors de la suppression."));
                }
                break;

            case "leave":
                String cityLeave = cityManager.getCityName(uuid);
                CityRank rankLeave = cityManager.getCityRank(uuid);

                if (cityLeave == null || rankLeave == null) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "Vous n'êtes dans aucune ville."));
                    return true;
                }

                if (rankLeave == CityRank.LEADER) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "Vous devez transférer le rôle de chef ou supprimer la ville."));
                    return true;
                }

                if (cityManager.removeMember(uuid)) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "Vous avez quitté la ville §d" + cityLeave + "§b."));
                } else {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "Erreur lors de votre départ."));
                }
                break;

            case "like":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "Utilisation: /city like <ville>"));
                    return true;
                }

                if (cityManager.hasLikedCity(uuid, args[1])) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "Vous avez déjà liké cette ville."));
                    return true;
                }

                if (cityManager.likeCity(uuid, args[1])) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "Vous avez liké la ville §d" + args[1] + " §b!"));
                } else {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "Ville introuvable."));
                }
                break;

            case "info":
                String infoCity = cityManager.getCityName(uuid);
                CityRank infoRank = cityManager.getCityRank(uuid);

                if (infoCity == null || infoRank == null) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "Vous n'appartenez à aucune ville."));
                    return true;
                }

                Location spawn = cityManager.getCityLocation(infoCity);
                int likes = cityManager.getLikes(uuid);

                player.sendMessage("§b§m---------------------------");
                player.sendMessage("§b§l» §eInformations sur votre ville");
                player.sendMessage("§f➤ §dNom : §d" + infoCity);
                player.sendMessage("§f➤ §dGrade : §d" + infoRank.getDisplayName());
                player.sendMessage("§f➤ §dLikes : §d" + likes + " ⭐");
                player.sendMessage("§f➤ §dSpawn : §7" + (spawn != null ? formatLoc(spawn) : "§cNon défini"));
                player.sendMessage("§b§m---------------------------");
                break;

            case "accept":
                String accepted = Main.get().getPendingInvites().remove(uuid);
                if (accepted == null) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "Aucune invitation en attente."));
                    return true;
                }

                if (cityManager.getCityName(uuid) != null) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "Vous êtes déjà dans une ville."));
                    return true;
                }

                if (cityManager.setMember(accepted, uuid, CityRank.MEMBER)) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "Vous avez rejoint la ville §d" + accepted + "§b !"));
                } else {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "Erreur lors de l'ajout."));
                }
                break;

            case "deny":
                if (Main.get().getPendingInvites().remove(uuid) != null) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "Invitation refusée."));
                } else {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "Aucune invitation en attente."));
                }
                break;

            case "invite":
                if (!isLeaderOrCoLeader(uuid)) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "Seul le chef ou le sous-chef peut inviter."));
                    return true;
                }

                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "Entrez le pseudo à inviter dans le chat. Tapez 'quitter' pour annuler."));
                CityInviteListener.awaitingInviteInput.add(uuid);
                break;

            case "promote":
                if (!isLeader(uuid)) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "Seul le chef peut promouvoir un joueur."));
                    return true;
                }

                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "Entrez le pseudo à promouvoir. Tapez 'quitter' pour annuler."));
                CityInviteListener.awaitingPromoteInput.add(uuid);
                break;

            case "confirm":
                UUID target = CityInviteListener.awaitingConfirmPromote.remove(uuid);
                if (target == null) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "Aucune promotion en attente."));
                    return true;
                }

                String leaderCity = cityManager.getCityName(uuid);
                if (!leaderCity.equals(cityManager.getCityName(target))) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "Ce joueur n'est plus dans votre ville."));
                    return true;
                }

                if (cityManager.setMember(leaderCity, uuid, CityRank.COLEADER)
                        && cityManager.setMember(leaderCity, target, CityRank.LEADER)) {
                    Player targetPlayer = Bukkit.getPlayer(target);
                    player.sendMessage("§aVous avez promu §e" + (targetPlayer != null ? targetPlayer.getName() : "le joueur") + " §aChef !");
                    if (targetPlayer != null) targetPlayer.sendMessage("§aVous êtes désormais le Chef de la ville !");
                } else {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "Erreur lors de la promotion."));
                }
                break;

            case "demote":
                if (!isLeader(uuid)) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "Seul le chef peut rétrograder un joueur."));
                    return true;
                }

                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "Entrez le pseudo à rétrograder. Tapez 'quitter' pour annuler."));
                CityInviteListener.awaitingDemoteInput.add(uuid);
                break;

            case "claim":
                if (!isLeaderOrCoLeader(uuid)) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cSeuls le chef ou le sous-chef peuvent revendiquer."));
                    return true;
                }

                if (!player.getWorld().getName().equalsIgnoreCase("world_newhorizon")) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            "&cVous ne pouvez pas revendiquer ici. Rendez-vous dans la zone Survie en parlant au Capitaine Jack."));
                    return true;
                }

                int claimCityId = cityManager.getCityId(uuid);
                int currentClaimCount = Main.get().getClaimManager().getClaimCount(claimCityId);

                double baseCost = 50.0;
                double claimCost = baseCost * Math.pow(2, currentClaimCount);

                if (cityBankManager.getBalance(claimCityId) < claimCost) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            "&cLa banque doit contenir au moins " + String.format("%.2f", claimCost) + " pièces."));
                    return true;
                }

                Chunk chunk = player.getLocation().getChunk();
                if (Main.get().getClaimManager().isChunkClaimed(chunk)) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cCe chunk est déjà revendiqué."));
                    return true;
                }

                if (Main.get().getClaimManager().claimChunk(claimCityId, chunk)) {
                    cityBankManager.withdraw(claimCityId, claimCost);
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            "&bChunk revendiqué ! &7(-" + String.format("%.2f", claimCost) + " pièces)"));
                } else {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cErreur lors de la revendication."));
                }
                break;

            case "unclaim":
                if (!isLeaderOrCoLeader(uuid)) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "Seuls le chef ou le sous-chef peuvent libérer un chunk."));
                    return true;
                }

                int cityIdUnclaim = cityManager.getCityId(uuid);
                Chunk currentChunk = player.getLocation().getChunk();

                if (Main.get().getClaimManager().unclaimChunk(cityIdUnclaim, currentChunk)) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "Chunk libéré !"));
                } else {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "Ce chunk n'est pas revendiqué par votre ville."));
                }
                break;

            case "deposit":
                if (cityManager.getCityName(uuid) == null) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "Vous devez être dans une ville pour faire cela."));
                    return true;
                }

                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "Entrez le montant à déposer dans le chat. Tapez 'quitter' pour annuler."));
                CityChatListener.awaitingDeposit.add(uuid);
                break;

            case "withdraw":
                CityRank rankWithdraw = cityManager.getCityRank(uuid);
                if (rankWithdraw == null || (rankWithdraw != CityRank.LEADER && rankWithdraw != CityRank.COLEADER)) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "Seuls le chef ou le sous-chef peuvent retirer."));
                    return true;
                }

                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "Entrez le montant à retirer dans le chat. Tapez 'quitter' pour annuler."));
                CityChatListener.awaitingWithdraw.add(uuid);
                break;

            case "list":
                new CityGUI().openCityList(player);
                break;

            case "setbanner":
                String cityName = cityManager.getCityName(uuid);
                CityRank rank = cityManager.getCityRank(uuid);

                if (cityName == null || rank != CityRank.LEADER) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "Seul le chef de ville peut définir la bannière."));
                    return true;
                }

                ItemStack itemInHand = player.getInventory().getItemInMainHand();
                if (itemInHand == null || !(itemInHand.getItemMeta() instanceof BannerMeta)) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "Vous devez tenir une §ebannière personnalisée §cdans votre main."));
                    return true;
                }

                try {
                    // Sérialisation de l'ItemStack en Base64
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
                    dataOutput.writeObject(itemInHand);
                    dataOutput.close();
                    String encoded = Base64.getEncoder().encodeToString(outputStream.toByteArray());

                    try (PreparedStatement ps = Main.get().getDatabase().prepareStatement(
                            "UPDATE cities SET banner = ? WHERE city_name = ?")) {
                        ps.setString(1, encoded);
                        ps.setString(2, cityName);
                        ps.executeUpdate();
                    }

                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "La bannière de votre ville a été mise à jour avec succès !"));
                } catch (IOException | SQLException e) {
                    e.printStackTrace();
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "Une erreur est survenue lors de la sauvegarde de la bannière."));
                }
                break;



            default:
                //sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "Commande inconnue. Utilisation: /city <create|...>"));
                break;
        }

        return true;
    }

    private boolean isLeader(UUID uuid) {
        return cityManager.getCityRank(uuid) == CityRank.LEADER;
    }

    private boolean isLeaderOrCoLeader(UUID uuid) {
        CityRank rank = cityManager.getCityRank(uuid);
        return rank == CityRank.LEADER || rank == CityRank.COLEADER;
    }

    private String formatLoc(Location loc) {
        return "§f" + loc.getWorld().getName() + " §7(§a" +
                Math.round(loc.getX()) + "§7, §a" +
                Math.round(loc.getY()) + "§7, §a" +
                Math.round(loc.getZ()) + "§7)";
    }
}