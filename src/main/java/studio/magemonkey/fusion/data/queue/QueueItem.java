package studio.magemonkey.fusion.data.queue;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.fusion.cfg.Cfg;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.data.professions.pattern.Category;
import studio.magemonkey.fusion.data.recipes.Recipe;

@Getter
@AllArgsConstructor
public class QueueItem {

    private          long      id;
    private          String    profession;
    private          Category  category;
    private @NonNull Recipe    recipe;
    private          ItemStack icon;
    private          long      timestamp;
    private          boolean   done;
    private          int       savedSeconds;

    private boolean       isRunning = false;
    private CraftingQueue craftingQueue;

    @Getter
    private int visualRemainingItemTime;

    public QueueItem(int id,
                     String profession,
                     Category category,
                     @NotNull Recipe recipe,
                     long timestamp,
                     int savedSeconds) {
        this.id = id;
        this.profession = profession;
        this.category = category;
        this.recipe = recipe;
        this.timestamp = timestamp;
        this.savedSeconds = savedSeconds;
        this.visualRemainingItemTime = (recipe.getCraftingTime() - savedSeconds);
        // If the queue item shall not be working when player is offline, just instantly override the timestamp
        if (Cfg.updateQueueOffline) {
            int diff = (int) ((System.currentTimeMillis() - timestamp) / 1000);
            if (diff + savedSeconds > recipe.getCraftingTime()) {
                this.savedSeconds = recipe.getCraftingTime();
                this.done = true;
            } else {
                this.savedSeconds += diff;
            }
        }
        this.timestamp = System.currentTimeMillis();
    }

    public void setCraftinQueue(CraftingQueue craftingQueue) {
        this.craftingQueue = craftingQueue;
    }

    public void update() {
        if (isDone()) return;
        if (this.craftingQueue != null) {
            this.visualRemainingItemTime = craftingQueue.getVisualRemainingTotalTime();
            int reconstructedCooldown = this.visualRemainingItemTime + savedSeconds;

            if (visualRemainingItemTime == recipe.getCraftingTime() + 1) return;
            if (reconstructedCooldown <= recipe.getCraftingTime()) {
                if (!isRunning) {
                    isRunning = true;
                    return;
                }
                this.savedSeconds++;
                this.done = savedSeconds >= recipe.getCraftingTime();
                this.icon = ProfessionsCfg.getQueueItem(profession, this);
                if (this.savedSeconds > 0)
                    this.visualRemainingItemTime--;
            }
        } else {
            this.icon = ProfessionsCfg.getQueueItem(profession, this);
        }
    }

    public void updateIcon() {
        this.icon = ProfessionsCfg.getQueueItem(profession, this);
    }

    public String getRecipePath() {
        return recipe.getRecipePath();
    }
}
