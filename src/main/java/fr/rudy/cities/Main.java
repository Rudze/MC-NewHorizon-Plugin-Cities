package fr.rudy.cities;

import fr.rudy.cities.api.CitiesAPI;
import fr.rudy.cities.commands.CityAdminCommand;
import fr.rudy.cities.commands.CityCommand;
import fr.rudy.cities.listener.*;
import fr.rudy.cities.manager.CityBankManager;
import fr.rudy.cities.manager.CityManager;
import fr.rudy.cities.manager.ClaimManager;
import fr.rudy.cities.ui.CityGUIListener;
import fr.rudy.databaseapi.DatabaseAPI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Main extends JavaPlugin implements CitiesAPI {

    private static Main instance;
    private CityManager cityManager;
    private CityBankManager cityBankManager;
    private ClaimManager claimManager;
    private final Map<UUID, String> pendingInvites = new HashMap<>();

    public static Main get() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        if (Bukkit.getPluginManager().getPlugin("DatabaseAPI") == null) {
            getLogger().severe("❌ DatabaseAPI est requis mais non trouvé !");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        setupManagers();
        setupTables();
        registerCommands();
        registerListeners();

        // ✅ Enregistrement de l'API CitiesAPI via le ServiceManager
        Bukkit.getServicesManager().register(CitiesAPI.class, this, this, ServicePriority.Normal);

        getLogger().info("✅ CityPlugin activé !");
    }

    @Override
    public void onDisable() {
        Bukkit.getServicesManager().unregister(CitiesAPI.class, this);
        getLogger().info("🛑 CityPlugin désactivé proprement.");
    }

    private void setupManagers() {
        cityManager = new CityManager();
        cityBankManager = new CityBankManager();
        claimManager = new ClaimManager();
    }

    private void registerCommands() {
        getCommand("city").setExecutor(new CityCommand());
        getCommand("cityadmin").setExecutor(new CityAdminCommand());
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new CityChatListener(), this);
        Bukkit.getPluginManager().registerEvents(new CityInviteListener(), this);
        Bukkit.getPluginManager().registerEvents(new ChunkProtectionListener(), this);
        Bukkit.getPluginManager().registerEvents(new CityEnterListener(), this);
        Bukkit.getPluginManager().registerEvents(new CityGUIListener(), this);
        Bukkit.getPluginManager().registerEvents(new RaidTriggerListener(), this);
    }

    private void setupTables() {
        try (Statement statement = getDatabase().createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS cities (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "owner_uuid TEXT NOT NULL, " +
                    "city_name TEXT UNIQUE NOT NULL, " +
                    "world TEXT NOT NULL, x DOUBLE NOT NULL, y DOUBLE NOT NULL, z DOUBLE NOT NULL, " +
                    "yaw FLOAT, pitch FLOAT, likes INTEGER DEFAULT 0, " +
                    "liked_by TEXT DEFAULT '', members TEXT DEFAULT '', " +
                    "banner TEXT, bank_balance DOUBLE DEFAULT 0.0)");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS city_claims (" +
                    "chunk_x INTEGER NOT NULL, chunk_z INTEGER NOT NULL, " +
                    "world TEXT NOT NULL, city_id INTEGER NOT NULL, " +
                    "PRIMARY KEY (chunk_x, chunk_z, world))");

            getLogger().info("✅ Tables SQLite créées avec succès !");
        } catch (Exception e) {
            e.printStackTrace();
            getLogger().severe("❌ Erreur lors de la création des tables SQLite !");
        }
    }

    public Connection getDatabase() {
        return DatabaseAPI.get().getDatabaseManager().getConnection();
    }

    @Override
    public CityManager getCityManager() {
        return cityManager;
    }

    @Override
    public ClaimManager getClaimManager() {
        return claimManager;
    }

    public CityBankManager getCityBankManager() {
        return cityBankManager;
    }

    public Map<UUID, String> getPendingInvites() {
        return pendingInvites;
    }
}
