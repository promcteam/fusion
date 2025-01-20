package studio.magemonkey.fusion.gui.editors.professions;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.codex.util.messages.MessageData;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.cfg.editors.EditorCriteria;
import studio.magemonkey.fusion.cfg.editors.EditorRegistry;
import studio.magemonkey.fusion.commands.FusionEditorCommand;
import studio.magemonkey.fusion.data.recipes.CraftingTable;
import studio.magemonkey.fusion.gui.editors.Editor;
import studio.magemonkey.fusion.gui.editors.pattern.PatternEditor;
import studio.magemonkey.fusion.gui.editors.pattern.PatternItemsEditor;
import studio.magemonkey.fusion.util.InventoryUtils;

@Getter
public class ProfessionEditor extends Editor implements Listener {

    private final Player        player;
    private final String        profession;
    private final CraftingTable table;

    private PatternItemsEditor patternItemsEditor;
    private PatternEditor      patternEditor;
    private PatternItemsEditor categoryPatternItemEditor;
    private PatternEditor      categoryPatternEditor;
    private CategoryEditor     categoryEditor;
    private RecipeEditor       recipeEditor;

    public ProfessionEditor(Player player, String profession) {
        super(null, EditorRegistry.getProfessionEditorCfg().getTitle(profession), 45);
        this.player = player;
        this.profession = profession;
        // Copy the table to prevent changes to the original table while people do crafting
        this.table = CraftingTable.copy(ProfessionsCfg.getTable(profession));
        table.cleanUpRecipesForEditor();
        setIcons(EditorRegistry.getProfessionEditorCfg().getIcons(table));

        initialize();
        Fusion.registerListener(this);
    }

    private void initialize() {
        InventoryUtils.fillInventory(getInventory(), getIcons().get("fill"));
        setItem(10, getIcons().get("name"));
        setItem(11, getIcons().get("icon"));
        setItem(12, getIcons().get("masteryUnlock"));
        setItem(13, getIcons().get("masteryCost"));
        setItem(14, getIcons().get("maxLevel"));
        setItem(16, getIcons().get("recipes"));
        setItem(28, getIcons().get("useCategories"));
        setItem(29, getIcons().get("patternItems"));
        setItem(30, getIcons().get("pattern"));

        setItem(32, getIcons().get("categories"));
        setItem(33, getIcons().get("categoryPatternItems"));
        setItem(34, getIcons().get("categoryPattern"));
        setItem(36, getIcons().get("save"));
        setItem(44, getIcons().get("back"));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() != getInventory()) return;
        event.setCancelled(true);
        Player  player     = (Player) event.getWhoClicked();
        boolean hasChanges = false;

        switch (event.getSlot()) {
            case 10 -> FusionEditorCommand.suggestUsage(player,
                    EditorCriteria.Profession_Edit_Name,
                    "/fusion-editor " + table.getName());
            case 11 -> FusionEditorCommand.suggestUsage(player,
                    EditorCriteria.Profession_Edit_Icon,
                    "/fusion-editor DIVINITY_" + table.getIconItem().getID());
            case 12 -> {
                int amount = event.isShiftClick() ? 10 : 1;
                if (event.isLeftClick()) {
                    table.setMasteryUnlock(table.getMasteryUnlock() + amount);
                    hasChanges = true;
                } else if (event.isRightClick()) {
                    if (table.getMasteryUnlock() == 0) return;
                    table.setMasteryUnlock(Math.max(table.getMasteryUnlock() - amount, 0));
                    hasChanges = true;
                }
            }
            case 13 -> {
                int amount = event.isShiftClick() ? 100 : 1;
                if (event.isLeftClick()) {
                    table.setMasteryFee(table.getMasteryFee() + amount);
                    hasChanges = true;
                } else if (event.isRightClick()) {
                    if (table.getMasteryFee() == 0) return;
                    table.setMasteryFee(Math.max(table.getMasteryFee() - amount, 0));
                    hasChanges = true;
                }
            }
            case 14 -> {
                int amount = event.isShiftClick() ? 10 : 1;
                if (event.isLeftClick()) {
                    table.setMaxLevel(table.getMaxLevel() + amount);
                    hasChanges = true;
                } else if (event.isRightClick()) {
                    if (table.getMaxLevel() == 0) return;
                    table.setMaxLevel(Math.max(table.getMaxLevel() - amount, 0));
                    hasChanges = true;
                }
            }
            case 16 -> {
                if (recipeEditor == null)
                    recipeEditor = new RecipeEditor(this, player, table);
                recipeEditor.open(player);
            }
            case 28 -> {
                table.setUseCategories(!table.getUseCategories());
                hasChanges = true;
            }
            case 29 -> {
                if (patternItemsEditor == null)
                    patternItemsEditor = new PatternItemsEditor(this, player, table, false);
                patternItemsEditor.open(player);
            }
            case 30 -> {
                if (patternEditor == null)
                    patternEditor = new PatternEditor(this, player, table, false);
                patternEditor.open(player);
            }
            case 32 -> {
                if (categoryEditor == null)
                    categoryEditor = new CategoryEditor(this, player, table);
                categoryEditor.open(player);
            }
            case 33 -> {
                if (categoryPatternItemEditor == null)
                    categoryPatternItemEditor = new PatternItemsEditor(this, player, table, true);
                categoryPatternItemEditor.open(player);
            }
            case 34 -> {
                if (event.isLeftClick()) {
                    if (categoryPatternEditor == null)
                        categoryPatternEditor = new PatternEditor(this, player, table, true);
                    categoryPatternEditor.open(player);
                } else if (event.isRightClick()) {
                    if (table.getCatPattern() != null) {
                        table.getCatPattern().clear();
                    }
                }
            }
            case 36 -> table.save(() -> {
                player.closeInventory();
                CodexEngine.get()
                        .getMessageUtil()
                        .sendMessage("editor.changesSaved",
                                player,
                                new MessageData("file", ProfessionsCfg.getFiles().get(profession).getName()));
                EditorRegistry.removeCurrentEditor(player);
                FusionEditorCommand.removeEditorCriteria(player.getUniqueId());
                ProfessionsCfg.init();
            });
            case 44 -> {
                player.closeInventory();
                hasChanges = true;
            }
        }

        if (hasChanges) {
            reload(true);
        }
    }

    public void reload(boolean open) {
        setIcons(EditorRegistry.getProfessionEditorCfg().getIcons(table));
        initialize();
        if (open)
            open(player);
    }
}
