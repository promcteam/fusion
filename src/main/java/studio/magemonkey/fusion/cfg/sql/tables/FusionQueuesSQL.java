package studio.magemonkey.fusion.cfg.sql.tables;

import org.bukkit.entity.Player;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.cfg.sql.SQLManager;
import studio.magemonkey.fusion.data.professions.pattern.Category;
import studio.magemonkey.fusion.data.queue.CraftingQueue;
import studio.magemonkey.fusion.data.queue.QueueItem;
import studio.magemonkey.fusion.data.recipes.CraftingTable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class FusionQueuesSQL {

    private final String Table = "fusion_queues";

    public FusionQueuesSQL() {
        try (PreparedStatement create = SQLManager.connection()
                .prepareStatement("CREATE TABLE IF NOT EXISTS " + Table + "("
                        + "Id long,"
                        + "UUID varchar(36), "
                        + "RecipePath varchar(100),"
                        + "CraftingTime numeric,"
                        + "SavedSeconds numeric,"
                        + "Timestamp BIGINT)")) {
            create.execute();
        } catch (SQLException e) {
            Fusion.getInstance()
                    .getLogger()
                    .warning("[SQL:FusionQueuesSQL:FusionQueuesSQL] Something went wrong with the sql-connection: "
                            + e.getMessage());
        }
    }

    public long getNextId() {
        try (PreparedStatement select = SQLManager.connection().prepareStatement("SELECT Count(Id) FROM " + Table)) {
            ResultSet result = select.executeQuery();
            if (result.next()) {
                return result.getLong(1);
            }
        } catch (SQLException e) {
            Fusion.getInstance()
                    .getLogger()
                    .warning("[SQL:FusionQueuesSQL:getNextId] Something went wrong with the sql-connection: "
                            + e.getMessage());
        }
        return 0;
    }

    public boolean setQueueItem(UUID uuid, QueueItem item) {
        if (item == null) return false;
        if (item.getId() == -1) {
            try (PreparedStatement insert = SQLManager.connection()
                    .prepareStatement("INSERT INTO " + Table
                            + "(Id, UUID, RecipePath, Timestamp, CraftingTime, SavedSeconds) VALUES(?,?,?,?,?,?)")) {
                insert.setLong(1, getNextId());
                insert.setString(2, uuid.toString());
                insert.setString(3, item.getRecipePath());
                insert.setLong(4, item.getTimestamp());
                insert.setLong(5, item.getRecipe().getCraftingTime());
                insert.setLong(6, item.getSavedSeconds());
                insert.execute();
                return true;
            } catch (SQLException e) {
                Fusion.getInstance()
                        .getLogger()
                        .warning("[SQL:FusionQueuesSQL:setQueueItem] Something went wrong with the sql-connection: "
                                + e.getMessage());
            }
        } else {
            try (PreparedStatement update = SQLManager.connection()
                    .prepareStatement("UPDATE " + Table + " SET SavedSeconds=? WHERE Id=?")) {
                update.setLong(1, item.getSavedSeconds());
                update.setLong(2, item.getId());
                update.execute();
                return true;
            } catch (SQLException e) {
                Fusion.getInstance()
                        .getLogger()
                        .warning("[SQL:FusionQueuesSQL:setQueueItem] Something went wrong with the sql-connection: "
                                + e.getMessage());
            }
        }
        return false;
    }

    public boolean removeQueueItem(QueueItem item) {
        try (PreparedStatement delete = SQLManager.connection()
                .prepareStatement("DELETE FROM " + Table + " WHERE Id=?")) {
            delete.setLong(1, item.getId());
            delete.execute();
            return true;
        } catch (SQLException e) {
            Fusion.getInstance()
                    .getLogger()
                    .warning("[SQL:FusionQueuesSQL:removeQueueItem] Something went wrong with the sql-connection: "
                            + e.getMessage());
        }
        return false;
    }

    public List<QueueItem> getQueueItems(UUID uuid, String profession, Category category) {
        List<QueueItem> entries = new ArrayList<>();
        String          sql     = "SELECT * FROM " + Table + " WHERE UUID=? AND RecipePath LIKE ?";

        try (PreparedStatement select = SQLManager.connection().prepareStatement(sql)) {
            select.setString(1, uuid.toString());
            select.setString(2, "%" + profession + "." + category.getName() + "%");
            try (ResultSet result = select.executeQuery()) {
                while (result.next()) {
                    entries.add(new QueueItem(
                            result.getInt("Id"),
                            profession,
                            category,
                            category.getRecipe(result.getString("RecipePath").split("\\.")[2]),
                            result.getLong("Timestamp"),
                            result.getInt("SavedSeconds")
                    ));
                }
            }
        } catch (SQLException e) {
            Fusion.getInstance()
                    .getLogger()
                    .warning("[SQL:FusionQueuesSQL:getQueueItems] Something went wrong with the sql-connection: "
                            + e.getMessage());
        }
        return entries;
    }

    public Map<String, CraftingQueue> getCraftingQueues(Player player) {
        Map<String, CraftingQueue> entries = new HashMap<>();
        for (Map.Entry<String, CraftingTable> entry : ProfessionsCfg.getMap().entrySet()) {
            String profession = entry.getKey();
            for (Category category : entry.getValue().getCategories().values()) {
                String path = profession + "." + category.getName();
                if (entries.containsKey(path)) continue;
                entries.putIfAbsent(path, new CraftingQueue(player, profession, category));
            }
        }
        return entries;
    }

    public void saveCraftingQueue(CraftingQueue queue) {
        queue.cancelTask();
        for (QueueItem item : queue.getQueue()) {
            if (!setQueueItem(queue.getPlayer().getUniqueId(), item)) {
                Fusion.getInstance()
                        .getLogger()
                        .warning("An instance of " + item.getRecipePath() + " could not be saved to the database: "
                                + queue.getPlayer().getUniqueId());
            }
        }
    }
}
