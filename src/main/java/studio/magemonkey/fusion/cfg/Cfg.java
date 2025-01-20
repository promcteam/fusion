package studio.magemonkey.fusion.cfg;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.sql.DatabaseType;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public final class Cfg {
    public static String  recursive                = "floor(n+300^(n/7)^2)";
    public static String  finalMod                 = "floor(x)/4";
    public static long    dataSaveInterval         = 12000;
    public static double  forgetPenalty            = 0.2d;
    public static boolean craftingQueue            = true;
    public static int     finishedMessageInterval  = 300;
    public static boolean updateQueueOffline       = true;
    public static boolean showRequirementsOnBrowse = true;

    public static boolean hideRecipesNoPermission = false;
    public static boolean hideRecipesNoRank       = false;
    public static boolean hideRecipesLimitReached = false;

    public static boolean useCustomFormula = true;

    public static String finishMessage = "&aYou have crafting items ready for pickup! ($<amount>)";


    // No usage inside of Cfg, just used for default values. The actual values are stored in SQLManager.class
    private static final DatabaseType storageType     = DatabaseType.LOCAL;
    private static final String       storageHost     = "localhost";
    private static final int          storagePort     = 3306;
    private static final String       storageDatabase = "database";
    private static final String       storageUser     = "root";
    private static final String       storagePassword = "password";

    public static FileConfiguration getConfig() {
        File              file = new File(Fusion.getInstance().getDataFolder(), "config.yml");
        FileConfiguration cfg  = new YamlConfiguration();

        if (!file.exists()) {
            addDefs(cfg);
            file.getAbsoluteFile().getParentFile().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                Fusion.getInstance().getLogger().warning("Can't create config file: " + file + ":" + e.getMessage());
            }
            cfg.options().copyDefaults(true);
            try {
                cfg.save(file);
            } catch (IOException e) {
                Fusion.getInstance().getLogger().warning("Can't save config file: " + file + ":" + e.getMessage());
            }
        } else {
            try {
                reload(cfg, file);
            } catch (Exception e) {
                Fusion.getInstance().getLogger().warning("Can't load config file: " + file + ":" + e.getMessage());
                return null;
            }
        }

        return cfg;
    }

    private static void addDefs(FileConfiguration cfg) {
        // addDefault did not add it to config, so here is a work around
        if (!cfg.isSet("recursive_level_formula")) cfg.set("recursive_level_formula", recursive);
        if (!cfg.isSet("final_level_mod")) cfg.set("final_level_mod", dataSaveInterval);
        if (!cfg.isSet("data_save_interval")) cfg.set("data_save_interval", dataSaveInterval);
        if (!cfg.isSet("forget.penalty")) cfg.set("forget.penalty", forgetPenalty);
        if (!cfg.isSet("crafting_queue")) cfg.set("crafting_queue", craftingQueue);
        if (!cfg.isSet("update_queue_offline")) cfg.set("update_queue_offline", updateQueueOffline);
        if (!cfg.isSet("finished_message")) cfg.set("finished_message", finishMessage);
        if (!cfg.isSet("finished_message_interval")) cfg.set("finished_message_interval", finishedMessageInterval);
        if (!cfg.isSet("showRequirementsOnBrowse")) cfg.set("showRequirementsOnBrowse", showRequirementsOnBrowse);
        if (!cfg.isSet("hideRecipesDefault.noPermission"))
            cfg.set("hideRecipesDefault.noPermission", hideRecipesNoPermission);
        if (!cfg.isSet("hideRecipesDefault.noRank")) cfg.set("hideRecipesDefault.noRank", hideRecipesNoRank);
        if (!cfg.isSet("hideRecipesDefault.recipeLimitReached"))
            cfg.set("hideRecipesDefault.recipeLimitReached", hideRecipesLimitReached);

        if (!cfg.isSet("storage.type")) cfg.set("storage.type", storageType.name());
        if (!cfg.isSet("storage.host")) cfg.set("storage.host", storageHost);
        if (!cfg.isSet("storage.port")) cfg.set("storage.port", storagePort);
        if (!cfg.isSet("storage.database")) cfg.set("storage.database", storageDatabase);
        if (!cfg.isSet("storage.user")) cfg.set("storage.user", storageUser);
        if (!cfg.isSet("storage.password")) cfg.set("storage.password", storagePassword);

        if (!cfg.isSet("useCustomFormula")) cfg.set("useCustomFormula", useCustomFormula);
    }

    public static void init() {
        FileConfiguration cfg = getConfig();
        if (cfg == null) {
            Fusion.getInstance().getLogger().warning("Can't load config file!");
            return;
        }

        recursive = cfg.getString("recursive_level_formula");
        finalMod = cfg.getString("final_level_mod");
        dataSaveInterval = cfg.getLong("data_save_interval");
        forgetPenalty = cfg.getDouble("forget.penalty");
        craftingQueue = cfg.getBoolean("crafting_queue");
        updateQueueOffline = cfg.getBoolean("update_queue_offline");
        finishedMessageInterval = cfg.getInt("finished_message_interval");
        finishMessage = cfg.getString("finished_message");
        showRequirementsOnBrowse = cfg.getBoolean("showRequirementsOnBrowse");
        hideRecipesNoPermission = cfg.getBoolean("hideRecipesDefault.noPermission");
        hideRecipesNoRank = cfg.getBoolean("hideRecipesDefault.noRank");
        hideRecipesLimitReached = cfg.getBoolean("hideRecipesDefault.recipeLimitReached");

        useCustomFormula = cfg.getBoolean("useCustomFormula");

        migrateOldTypes(cfg);
    }

    private static void reload(FileConfiguration cfg, File file) {
        try {
            // Update the config from file
            cfg.load(file);
            // Add and save the defaults
            addDefs(cfg);
            cfg.save(file);
            // Load the config again
            cfg.load(file);
        } catch (Exception e) {
            Fusion.getInstance().getLogger().warning("Can't load config file: " + file + ":" + e.getMessage());
        }
    }

    public static void notifyForQueue(Player player, int amount) {
        player.sendMessage(finishMessage.replace("$<amount>", String.valueOf(amount)).replace("&", "§"));
    }

    public static void migrateOldTypes(FileConfiguration cfg) {
        List<Map<?, ?>> typesSection = cfg.getMapList("types");
        if (typesSection.isEmpty()) return;
        Fusion.getInstance().getLogger().warning("Found old types section in config.yml. Migrating...");
        typesSection.removeIf(typeData -> ProfessionsCfg.loadFrom((String) typeData.get("name"),
                (Map<String, Object>) typeData));
        cfg.set("types", typesSection);
        if (cfg.getMapList("types").isEmpty()) {
            cfg.set("types", null);
        }
        try {
            cfg.save(new File(Fusion.getInstance().getDataFolder(), "config.yml"));
        } catch (IOException e) {
            Fusion.getInstance().getLogger().warning("Can't save config file: " + e.getMessage());
        }
    }

    public static boolean setDatabaseType(DatabaseType type) {
        FileConfiguration cfg = getConfig();
        if (cfg == null) {
            Fusion.getInstance().getLogger().warning("Can't load config file!");
            return false;
        }
        cfg.set("storage.type", type.name());
        try {
            cfg.save(new File(Fusion.getInstance().getDataFolder(), "config.yml"));
            return true;
        } catch (IOException e) {
            Fusion.getInstance().getLogger().warning("Can't save config file: " + e.getMessage());
            return false;
        }
    }
}
