package studio.magemonkey.fusion.gui;

import lombok.Getter;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.codex.util.messages.MessageData;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.data.professions.pattern.Category;
import studio.magemonkey.fusion.data.recipes.CraftingTable;

import java.util.*;

public class ProfessionGuiRegistry {

    @Getter
    private final String profession;

    private final Map<UUID, CategoryGui> categoryGuis = new TreeMap<>();
    private final Map<UUID, RecipeGui>   recipeGuis   = new TreeMap<>();

    public ProfessionGuiRegistry(String profession) {
        this.profession = profession;
    }

    public void open(Player player) {
        CraftingTable table = ProfessionsCfg.getTable(profession);
        if (table.getUseCategories() && !table.getCategories().isEmpty()) {
            categoryGuis.put(player.getUniqueId(), new CategoryGui(player, table));
            categoryGuis.get(player.getUniqueId()).open(player);
        } else {
            recipeGuis.put(player.getUniqueId(),
                    new RecipeGui(player, table, new Category("master", "PAPER", table.getRecipePattern(), 1)));
            recipeGuis.get(player.getUniqueId()).open(player);
        }
    }

    public void open(Player player, Category category) {
        CraftingTable table = ProfessionsCfg.getTable(profession);
        if (table.getUseCategories() && !table.getCategories().isEmpty()) {
            if (table.getCategories().containsKey(category.getName())) {
                categoryGuis.put(player.getUniqueId(), new CategoryGui(player, table));
                categoryGuis.get(player.getUniqueId()).open(player, category);
            } else {
                CodexEngine.get()
                        .getMessageUtil()
                        .sendMessage("fusion.error.categoryNotAvailable",
                                player,
                                new MessageData("sender", player.getPlayer()));
            }
        } else {
            CodexEngine.get()
                    .getMessageUtil()
                    .sendMessage("fusion.error.noCategories", player, new MessageData("sender", player.getPlayer()));
        }

    }

    public void closeAll() {
        List<HumanEntity> toClose = new ArrayList<>();

        for (CategoryGui gui : new ArrayList<>(categoryGuis.values())) {
            gui.getInventory().getViewers().forEach(HumanEntity::closeInventory);
            for (RecipeGui recipeGui : gui.getCategories().values()) {
                toClose.addAll(recipeGui.getInventory().getViewers());
            }
        }
        for (RecipeGui gui : new ArrayList<>(recipeGuis.values())) {
            toClose.addAll(gui.getInventory().getViewers());
        }

        toClose.forEach(HumanEntity::closeInventory);
    }
}
