package fr.rudy.cities.commands;

import fr.rudy.cities.Main;
import fr.rudy.cities.manager.CityManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CityAdminCommand implements CommandExecutor {

    private final Connection db = Main.get().getDatabase();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 0) {
            sender.sendMessage("§e/City Admin Commands:");
            sender.sendMessage("§f• §a/city admin interest §7- Ajoute 7% d'intérêt à toutes les banques de villes");
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "interest":
                if (!sender.isOp() && !sender.getName().equalsIgnoreCase("CONSOLE")) {
                    sender.sendMessage("§cCommande réservée aux administrateurs.");
                    return true;
                }

                int updated = 0;

                try (PreparedStatement select = db.prepareStatement("SELECT id, bank_balance FROM cities");
                     ResultSet rs = select.executeQuery()) {

                    while (rs.next()) {
                        int id = rs.getInt("id");
                        double balance = rs.getDouble("bank_balance");
                        double interest = Math.round(balance * 0.07 * 100.0) / 100.0;
                        double newBalance = balance + interest;

                        try (PreparedStatement update = db.prepareStatement("UPDATE cities SET bank_balance = ? WHERE id = ?")) {
                            update.setDouble(1, newBalance);
                            update.setInt(2, id);
                            update.executeUpdate();
                            updated++;
                        }
                    }

                } catch (SQLException e) {
                    e.printStackTrace();
                    sender.sendMessage("§cErreur lors de l'application des intérêts.");
                    return true;
                }

                sender.sendMessage("§a✔ Intérêts appliqués à §e" + updated + " §avilles !");
                break;

            default:
                sender.sendMessage("§cCommande invalide. Utilisez /city admin interest");
                break;
        }

        return true;
    }
}