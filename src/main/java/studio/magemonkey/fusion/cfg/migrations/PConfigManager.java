package studio.magemonkey.fusion.cfg.migrations;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.cfg.sql.SQLManager;
import studio.magemonkey.fusion.data.professions.Profession;
import studio.magemonkey.fusion.data.queue.QueueItem;

import java.io.File;
import java.util.*;

public class PConfigManager {

    public static void backup() {
        File parentFolder = new File(Fusion.getInstance().getDataFolder() + File.separator + "players");
        if (!parentFolder.exists()) return;

        File backupFolder =
                new File(Fusion.getInstance().getDataFolder() + File.separator + "backup" + File.separator + "players");
        if (!backupFolder.exists()) backupFolder.mkdirs();

        for (File file : parentFolder.listFiles()) {
            if (file.getName().endsWith(".yml")) {
                File backupFile = new File(backupFolder, file.getName());
                try {
                    file.renameTo(backupFile);
                } catch (Exception e) {
                    Fusion.getInstance().getLogger().warning("Can't backup player data file: " + file);
                    e.printStackTrace();
                }
            }
        }
        Fusion.getInstance().getLogger().info("Backup of player data is done.");
    }

    public static void migrateIntoSQL() {
        File parentFolder = new File(Fusion.getInstance().getDataFolder() + File.separator + "players");
        if (!parentFolder.exists()) return;
        for (File file : parentFolder.listFiles()) {
            if (file.getName().endsWith(".yml")) {
                FileConfiguration cfg = new YamlConfiguration();
                try {
                    cfg.load(file);
                    UUID uuid = UUID.fromString(file.getName().replace(".yml", ""));

                    // Logic for players table
                    boolean autoCraft = cfg.getBoolean("autoCraft", false);
                    SQLManager.players().setAutoCrafting(uuid, autoCraft);
                    // Logic for professions table
                    Map<String, Boolean> professionData = new HashMap<>();
                    if (cfg.contains("professions")) {
                        for (String profession : cfg.getStringList("professions")) {
                            professionData.put(profession, false);
                        }
                    }
                    if (cfg.contains("guis")) {
                        for (String profession : cfg.getConfigurationSection("guis").getKeys(false)) {
                            professionData.put(profession, cfg.getBoolean("guis." + profession));
                        }
                    }

                    List<Profession> professions       = SQLManager.professions().getProfessions(uuid);
                    List<String>     joinedProfessions = cfg.getStringList("professions");
                    // Existing data of old ExperienceManage will be updated
                    for (Profession profession : professions) {
                        if (professionData.containsKey(profession.getName())) {
                            profession.setMastered(professionData.get(profession.getName()));
                            profession.setJoined(joinedProfessions.contains(profession.getName()));
                            SQLManager.professions().setProfession(uuid, profession);
                            professionData.remove(profession.getName());
                        }
                    }
                    // Remaining data of old PlayerConfig will be inserted
                    professionData.forEach((name, mastered) -> {
                        Profession profession =
                                new Profession(-1, uuid, name, 0, mastered, joinedProfessions.contains(name));
                        SQLManager.professions().setProfession(uuid, profession);
                    });

                    // Logic for queues table
                    if (cfg.contains("queues")) {
                        List<QueueItem> entries = new ArrayList<>();
                        for (String profession : cfg.getConfigurationSection("queues").getKeys(false)) {
                            for (String category : cfg.getConfigurationSection("queues." + profession).getKeys(false)) {
                                for (String recipe : cfg.getConfigurationSection(
                                        "queues." + profession + "." + category).getKeys(false)) {
                                    List<Long> timestamps = cfg.getLongList(
                                            "queues." + profession + "." + category + "." + recipe + ".timestamps");
                                    for (long timestamp : timestamps)
                                        entries.add(new QueueItem(-1,
                                                profession,
                                                ProfessionsCfg.getTable(profession).getCategory(category),
                                                ProfessionsCfg.getTable(profession)
                                                        .getCategory(category)
                                                        .getRecipe(recipe),
                                                timestamp,
                                                -1));
                                }
                            }
                        }
                        entries.forEach(e -> SQLManager.queues().setQueueItem(uuid, e));
                    }
                    Fusion.getInstance().getLogger().info("Migrated player data for " + uuid + " into SQL.");
                } catch (Exception e) {
                    Fusion.getInstance().getLogger().warning("Can't load player data file: " + file);
                    e.printStackTrace();
                    return;
                }
            }
        }
        backup();
        if (parentFolder.listFiles().length == 0) {
            parentFolder.delete();
            Fusion.getInstance().getLogger().info("Players folder is not required anymore and will be deleted.");
        }
    }
}
