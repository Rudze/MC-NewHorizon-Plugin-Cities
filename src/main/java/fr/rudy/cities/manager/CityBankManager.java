package fr.rudy.cities.manager;

import fr.rudy.cities.Main;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CityBankManager {

    private final Connection db = Main.get().getDatabase();

    public double getBalance(int cityId) {
        try (PreparedStatement ps = db.prepareStatement(
                "SELECT bank_balance FROM cities WHERE id = ?")) {
            ps.setInt(1, cityId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getDouble("bank_balance");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    public boolean deposit(int cityId, double amount) {
        try (PreparedStatement ps = db.prepareStatement(
                "UPDATE cities SET bank_balance = bank_balance + ? WHERE id = ?")) {
            ps.setDouble(1, amount);
            ps.setInt(2, cityId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean withdraw(int cityId, double amount) {
        if (getBalance(cityId) < amount) return false;

        try (PreparedStatement ps = db.prepareStatement(
                "UPDATE cities SET bank_balance = bank_balance - ? WHERE id = ?")) {
            ps.setDouble(1, amount);
            ps.setInt(2, cityId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void applyInterest(double percentage) {
        try (PreparedStatement ps = db.prepareStatement(
                "UPDATE cities SET bank_balance = bank_balance + bank_balance * (? / 100.0)")) {
            ps.setDouble(1, percentage);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}