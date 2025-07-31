package fr.rudy.cities.manager;

import fr.rudy.cities.Main;
import org.bukkit.Chunk;
import org.bukkit.Location;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ClaimManager {

    private final Connection db = Main.get().getDatabase();

    /**
     * Revendique un chunk pour une ville.
     */
    public boolean claimChunk(int cityId, Chunk chunk) {
        try (PreparedStatement ps = db.prepareStatement(
                "INSERT INTO city_claims (chunk_x, chunk_z, world, city_id) VALUES (?, ?, ?, ?)")) {
            ps.setInt(1, chunk.getX());
            ps.setInt(2, chunk.getZ());
            ps.setString(3, chunk.getWorld().getName());
            ps.setInt(4, cityId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Libère un chunk s'il appartient à la ville spécifiée.
     */
    public boolean unclaimChunk(int cityId, Chunk chunk) {
        try (PreparedStatement ps = db.prepareStatement(
                "DELETE FROM city_claims WHERE chunk_x = ? AND chunk_z = ? AND world = ? AND city_id = ?")) {
            ps.setInt(1, chunk.getX());
            ps.setInt(2, chunk.getZ());
            ps.setString(3, chunk.getWorld().getName());
            ps.setInt(4, cityId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Vérifie si un chunk est revendiqué par n'importe quelle ville.
     */
    public boolean isChunkClaimed(Chunk chunk) {
        try (PreparedStatement ps = db.prepareStatement(
                "SELECT 1 FROM city_claims WHERE chunk_x = ? AND chunk_z = ? AND world = ?")) {
            ps.setInt(1, chunk.getX());
            ps.setInt(2, chunk.getZ());
            ps.setString(3, chunk.getWorld().getName());
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Récupère l'ID de la ville qui possède le chunk donné.
     */
    public Integer getChunkOwnerId(Location loc) {
        try (PreparedStatement ps = db.prepareStatement(
                "SELECT city_id FROM city_claims WHERE chunk_x = ? AND chunk_z = ? AND world = ?")) {
            ps.setInt(1, loc.getChunk().getX());
            ps.setInt(2, loc.getChunk().getZ());
            ps.setString(3, loc.getWorld().getName());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("city_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Retourne le nombre total de claims actifs pour une ville.
     */
    public int getClaimCount(int cityId) {
        try (PreparedStatement ps = db.prepareStatement(
                "SELECT COUNT(*) FROM city_claims WHERE city_id = ?")) {
            ps.setInt(1, cityId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}