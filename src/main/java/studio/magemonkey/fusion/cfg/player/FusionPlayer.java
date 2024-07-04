package studio.magemonkey.fusion.cfg.player;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import studio.magemonkey.fusion.Category;
import studio.magemonkey.fusion.CraftingTable;
import studio.magemonkey.fusion.Profession;
import studio.magemonkey.fusion.cfg.sql.SQLManager;
import studio.magemonkey.fusion.queue.CraftingQueue;
import studio.magemonkey.fusion.queue.QueueItem;

import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@SuppressWarnings("unused")
public class FusionPlayer {

    private final UUID uuid;

    private final Map<String, Profession> professions = new TreeMap<>();
    private final Map<String, CraftingQueue> cachedQeues = new TreeMap<>();

    public FusionPlayer(UUID uuid) {
        this.uuid = uuid;
        for (Profession profession : SQLManager.professions().getProfessions(uuid)) {
            professions.put(profession.getName(), profession);
        }
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public CraftingQueue getQueue(String profession, Category category) {
        if (!cachedQeues.containsKey(profession)) {
            cachedQeues.put(profession, new CraftingQueue(getPlayer(), profession, category));
        }
        return cachedQeues.get(profession);
    }


    /*
     * Returns the amount of items in the queue for the given profession and category
     * @param profession The profession to check
     * @param category The category to check
     * @return The amount of items in the queue for the given profession and category

     * sizes[0] = amount of items in the queue for the given profession and category
     * sizes[1] = amount of items in the queue for the given profession
     * sizes[2] = amount of items in the queue
     */

    public int getExperience(String profession) {
        int experience = 0;
        if (professions.containsKey(profession)) {
            experience = (int) professions.get(profession).getExp();
        }
        return experience;
    }

    public int getExperience(Profession profession) {
        return getExperience(profession.getName());
    }

    public int getExperience(CraftingTable table) {
        return getExperience(table.getName());
    }

    public Profession getProfession(String profession) {
        return professions.get(profession);
    }

    public Profession getProfession(CraftingTable table) {
        return getProfession(table.getName());
    }

    public void removeProfession(String profession) {
        professions.remove(profession);
    }

    public void removeProfession(Profession profession) {
        removeProfession(profession.getName());
    }

    public void removeProfession(CraftingTable table) {
        removeProfession(table.getName());
    }

    public void addProfession(Profession profession) {
        professions.put(profession.getName(), profession);
    }

    public boolean hasMastered(String profession) {
        return professions.containsKey(profession) && professions.get(profession).isMastered();
    }

    public boolean hasMastered(Profession profession) {
        return hasMastered(profession.getName());
    }

    public boolean hasMastered(CraftingTable table) {
        return hasMastered(table.getName());
    }

    public boolean hasJoined(String profession) {
        return professions.containsKey(profession) && professions.get(profession).isJoined();
    }

    public boolean hasJoined(Profession profession) {
        return hasJoined(profession.getName());
    }

    public boolean hasJoined(CraftingTable table) {
        return hasJoined(table.getName());
    }

    public void setMastered(String profession, boolean mastered) {
        if (professions.containsKey(profession)) {
            professions.get(profession).setMastered(mastered);
        }
    }

    public void setMastered(Profession profession, boolean mastered) {
        setMastered(profession.getName(), mastered);
    }

    public void setMastered(CraftingTable table, boolean mastered) {
        setMastered(table.getName(), mastered);
    }

    public void setJoined(String profession, boolean joined) {
        if (professions.containsKey(profession)) {
            professions.get(profession).setJoined(joined);
        }
    }

    public void setJoined(Profession profession, boolean joined) {
        setJoined(profession.getName(), joined);
    }

    public void setJoined(CraftingTable table, boolean joined) {
        setJoined(table.getName(), joined);
    }

    public void addExperience(String profession, int experience) {
        if (professions.containsKey(profession)) {
            professions.get(profession).addExp(experience);
        }
    }

    public void addExp(Profession profession, int experience) {
        addExperience(profession.getName(), experience);
    }

    public void addExp(CraftingTable table, int experience) {
        addExperience(table.getName(), experience);
    }

    public void removeExperience(String profession, int experience) {
        if (professions.containsKey(profession)) {
            professions.get(profession).removeExp(experience);
        }
    }

    public void removeExp(Profession profession, int experience) {
        removeExperience(profession.getName(), experience);
    }

    public void removeExp(CraftingTable table, int experience) {
        removeExperience(table.getName(), experience);
    }

    public void resetExperience(String profession) {
        if (professions.containsKey(profession)) {
            professions.get(profession).resetExp();
        }
    }

    public void resetExp(Profession profession) {
        resetExperience(profession.getName());
    }

    public void resetExp(CraftingTable table) {
        resetExperience(table.getName());
    }

    public void resetMastered(String profession) {
        if (professions.containsKey(profession)) {
            professions.get(profession).resetMastered();
        }
    }

    public void resetMastered(Profession profession) {
        resetMastered(profession.getName());
    }

    public void resetMastered(CraftingTable table) {
        resetMastered(table.getName());
    }

    public void resetJoined(String profession) {
        if (professions.containsKey(profession)) {
            professions.get(profession).resetJoined();
        }
    }

    public void resetJoined(Profession profession) {
        resetJoined(profession.getName());
    }

    public void resetJoined(CraftingTable table) {
        resetJoined(table.getName());
    }

    public void reset(String profession) {
        if (professions.containsKey(profession)) {
            professions.get(profession).reset();
        }
    }

    public void reset(Profession profession) {
        reset(profession.getName());
    }

    public void reset(CraftingTable table) {
        reset(table.getName());
    }

    public int[] getQueueSizes(String profession, Category category) {
        int[] limits = new int[]{0, 0, 0};
        limits[0] = cachedQeues.get(profession).getQueue().size();
        for (CraftingQueue queue : cachedQeues.values()) {
            if (queue.getProfession().equals(profession)) {
                limits[1] += queue.getQueue().size();
            }
            limits[2] += queue.getQueue().size();
        }
        return limits;
    }

    public int getFinishedSize() {
        int size = 0;
        for (CraftingQueue queue : cachedQeues.values()) {
            for (QueueItem item : queue.getQueue()) {
                if (item.isDone()) {
                    size++;
                }
            }
        }
        return size;
    }

    public void save() {
        for (Profession profession : professions.values()) {
            SQLManager.professions().setProfession(uuid, profession);
        }
        for (CraftingQueue queue : cachedQeues.values()) {
            SQLManager.queues().saveCraftingQueue(queue);
        }
    }
}
