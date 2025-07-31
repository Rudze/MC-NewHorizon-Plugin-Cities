package fr.rudy.cities.manager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import fr.rudy.cities.CityRank;
import fr.rudy.cities.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.lang.reflect.Type;
import java.sql.*;
import java.util.*;

public class CityManager {

    private final Connection connection;
    private final Gson gson = new Gson();
    private final Type mapType = new TypeToken<Map<String, String>>() {}.getType();

    public CityManager() {
        this.connection = Main.get().getDatabase();
    }

    public String getCityName(UUID playerUUID) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT city_name, members FROM cities")) {
            ResultSet result = statement.executeQuery();
            while (result.next()) {
                String json = result.getString("members");
                Map<String, String> members = gson.fromJson(json, mapType);
                if (members != null && members.containsKey(playerUUID.toString())) {
                    return result.getString("city_name");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public int getCityId(UUID playerUUID) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT id, members FROM cities")) {
            ResultSet result = statement.executeQuery();
            while (result.next()) {
                String json = result.getString("members");
                Map<String, String> members = gson.fromJson(json, mapType);
                if (members != null && members.containsKey(playerUUID.toString())) {
                    return result.getInt("id");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public CityRank getCityRank(UUID playerUUID) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT members FROM cities")) {
            ResultSet result = statement.executeQuery();
            while (result.next()) {
                String json = result.getString("members");
                Map<String, String> members = gson.fromJson(json, mapType);
                if (members != null && members.containsKey(playerUUID.toString())) {
                    return CityRank.valueOf(members.get(playerUUID.toString()));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean setMember(String cityName, UUID uuid, CityRank rank) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT members FROM cities WHERE city_name = ?")) {
            statement.setString(1, cityName);
            ResultSet result = statement.executeQuery();
            if (result.next()) {
                String json = result.getString("members");
                Map<String, String> members = json == null || json.isEmpty() ? new HashMap<>() : gson.fromJson(json, mapType);
                members.put(uuid.toString(), rank.name());

                try (PreparedStatement update = connection.prepareStatement("UPDATE cities SET members = ? WHERE city_name = ?")) {
                    update.setString(1, gson.toJson(members));
                    update.setString(2, cityName);
                    return update.executeUpdate() > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean removeMember(UUID uuid) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT city_name, members FROM cities")) {
            ResultSet result = statement.executeQuery();
            while (result.next()) {
                String json = result.getString("members");
                Map<String, String> members = gson.fromJson(json, mapType);

                if (members.containsKey(uuid.toString())) {
                    members.remove(uuid.toString());
                    try (PreparedStatement update = connection.prepareStatement("UPDATE cities SET members = ? WHERE city_name = ?")) {
                        update.setString(1, gson.toJson(members));
                        update.setString(2, result.getString("city_name"));
                        return update.executeUpdate() > 0;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean createCity(UUID ownerUUID, String cityName, Location location) {
        Map<String, String> members = new HashMap<>();
        members.put(ownerUUID.toString(), CityRank.LEADER.name());

        // bloqué en dehors du monde autorisé
        if (!location.getWorld().getName().equalsIgnoreCase("world_newhorizon")) {
            return false;
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO cities (owner_uuid, city_name, world, x, y, z, yaw, pitch, members, bank_balance) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
        )) {
            statement.setString(1, ownerUUID.toString());
            statement.setString(2, cityName);
            statement.setString(3, location.getWorld().getName());
            statement.setDouble(4, location.getX());
            statement.setDouble(5, location.getY());
            statement.setDouble(6, location.getZ());
            statement.setFloat(7, location.getYaw());
            statement.setFloat(8, location.getPitch());
            statement.setString(9, gson.toJson(members));
            statement.setDouble(10, 0.0);

            if (statement.executeUpdate() > 0) {
                int cityId = -1;
                try (PreparedStatement getId = connection.prepareStatement("SELECT id FROM cities WHERE city_name = ?")) {
                    getId.setString(1, cityName);
                    ResultSet rs = getId.executeQuery();
                    if (rs.next()) {
                        cityId = rs.getInt("id");
                    }
                }

                if (cityId != -1) {
                    Main.get().getClaimManager().claimChunk(cityId, location.getChunk());
                }

                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean updateCitySpawn(String cityName, Location location) {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE cities SET world = ?, x = ?, y = ?, z = ?, yaw = ?, pitch = ? WHERE LOWER(city_name) = LOWER(?)"
        )) {
            statement.setString(1, location.getWorld().getName());
            statement.setDouble(2, location.getX());
            statement.setDouble(3, location.getY());
            statement.setDouble(4, location.getZ());
            statement.setFloat(5, location.getYaw());
            statement.setFloat(6, location.getPitch());
            statement.setString(7, cityName);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean removeCity(UUID ownerUUID) {
        try (PreparedStatement getCity = connection.prepareStatement("SELECT id FROM cities WHERE owner_uuid = ?")) {
            getCity.setString(1, ownerUUID.toString());
            ResultSet result = getCity.executeQuery();

            if (result.next()) {
                int cityId = result.getInt("id");

                try (PreparedStatement deleteClaims = connection.prepareStatement("DELETE FROM city_claims WHERE city_id = ?")) {
                    deleteClaims.setInt(1, cityId);
                    deleteClaims.executeUpdate();
                }

                try (PreparedStatement deleteCity = connection.prepareStatement("DELETE FROM cities WHERE id = ?")) {
                    deleteCity.setInt(1, cityId);
                    return deleteCity.executeUpdate() > 0;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public Location getCityLocation(String cityName) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM cities WHERE LOWER(city_name) = LOWER(?)")) {
            statement.setString(1, cityName);
            ResultSet result = statement.executeQuery();
            if (result.next()) {
                World world = Bukkit.getWorld(result.getString("world"));
                if (world == null) return null;

                return new Location(
                        world,
                        result.getDouble("x"),
                        result.getDouble("y"),
                        result.getDouble("z"),
                        result.getFloat("yaw"),
                        result.getFloat("pitch")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean hasCity(UUID ownerUUID) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM cities WHERE owner_uuid = ?")) {
            statement.setString(1, ownerUUID.toString());
            ResultSet result = statement.executeQuery();
            return result.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean hasLikedCity(UUID likerUUID, String cityName) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT liked_by FROM cities WHERE LOWER(city_name) = LOWER(?)")) {
            statement.setString(1, cityName);
            ResultSet result = statement.executeQuery();
            if (result.next()) {
                String likedBy = result.getString("liked_by");
                return Arrays.asList(likedBy.split(",")).contains(likerUUID.toString());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean likeCity(UUID likerUUID, String cityName) {
        if (hasLikedCity(likerUUID, cityName)) return false;

        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE cities SET likes = likes + 1, liked_by = liked_by || ? || ',' WHERE LOWER(city_name) = LOWER(?)"
        )) {
            statement.setString(1, likerUUID.toString());
            statement.setString(2, cityName);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public int getLikes(UUID ownerUUID) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT likes FROM cities WHERE owner_uuid = ?")) {
            statement.setString(1, ownerUUID.toString());
            ResultSet result = statement.executeQuery();
            if (result.next()) {
                return result.getInt("likes");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // ✅ NOUVELLE MÉTHODE — membres triés par rôle
    public List<UUID> getSortedMembersByRank(String cityName) {
        List<UUID> sorted = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT owner_uuid, members FROM cities WHERE city_name = ?")) {
            statement.setString(1, cityName);
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                String ownerUUID = rs.getString("owner_uuid");
                sorted.add(UUID.fromString(ownerUUID));

                String json = rs.getString("members");
                Map<String, String> members = gson.fromJson(json, mapType);

                if (members != null) {
                    members.entrySet().stream()
                            .filter(e -> !e.getKey().equals(ownerUUID))
                            .sorted(Comparator.comparingInt(e -> rankOrder(e.getValue())))
                            .map(e -> UUID.fromString(e.getKey()))
                            .forEach(sorted::add);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return sorted;
    }

    // 🧠 Méthode utilitaire pour classer les rangs
    private int rankOrder(String rankName) {
        try {
            return switch (CityRank.valueOf(rankName)) {
                case COLEADER -> 1;
                case MEMBER -> 2;
                default -> 3;
            };
        } catch (Exception e) {
            return 999;
        }
    }

    public String getCityNameById(int cityId) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT city_name FROM cities WHERE id = ?")) {
            statement.setInt(1, cityId);
            ResultSet result = statement.executeQuery();
            if (result.next()) {
                return result.getString("city_name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Inconnue";
    }


}