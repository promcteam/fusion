package studio.magemonkey.fusion.gui.editors.pattern;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.editors.EditorRegistry;
import studio.magemonkey.fusion.data.professions.pattern.InventoryPattern;
import studio.magemonkey.fusion.data.recipes.CraftingTable;
import studio.magemonkey.fusion.gui.editors.Editor;
import studio.magemonkey.fusion.gui.editors.browse.BrowseEditor;

import java.util.HashMap;
import java.util.Map;

public class PatternEditor extends Editor implements Listener {

    // Globally used variabled
    private final Player           player;
    private       InventoryPattern pattern;

    // Profession only
    private final CraftingTable table;
    private final boolean       isCategoryPattern;

    // Browse only
    private final BrowseEditor browseEditor;

    private final Map<Integer, Character> slots = new HashMap<>();

    public PatternEditor(Editor parentEditor, Player player, CraftingTable table, boolean isCategoryPattern) {
        super(parentEditor,
                EditorRegistry.getPatternEditorCfg().getTitle(isCategoryPattern ? "Category" : "Master"),
                (isCategoryPattern && table.getCatPattern() != null) ? table.getCatPattern().getInventorySize()
                        : table.getRecipePattern().getInventorySize());
        this.player = player;
        this.table = table;
        this.isCategoryPattern = isCategoryPattern;
        this.browseEditor = null;

        setIcons(EditorRegistry.getPatternEditorCfg().getIcons(table));
        this.pattern = isCategoryPattern ? table.getCatPattern() : table.getRecipePattern();
        if (isCategoryPattern && this.pattern == null) {
            table.setCatPattern(InventoryPattern.copy(table.getRecipePattern()));
            this.pattern = table.getCatPattern();
            CodexEngine.get().getMessageUtil().sendMessage("editor.defaultCategoryPattern", player);
        }

        initialize();
        Fusion.registerListener(this);
    }

    public PatternEditor(BrowseEditor browseEditor, Player player) {
        super(browseEditor,
                EditorRegistry.getPatternEditorCfg().getTitle("Browse"),
                browseEditor.getBrowsePattern().getInventorySize());
        this.player = player;
        this.browseEditor = browseEditor;
        this.table = null;
        this.isCategoryPattern = false;

        setIcons(EditorRegistry.getPatternEditorCfg().getIcons(browseEditor));
        this.pattern = browseEditor.getBrowsePattern();
        initialize();
        Fusion.registerListener(this);
    }

    public void initialize() {
        slots.clear();
        int i = 0;
        for (String key : pattern.getPattern()) {
            for (char c : key.toCharArray()) {
                slots.put(i, c);
                if (c == 'o') {
                    if (table != null)
                        setItem(i, getIcons().get("recipeSlot"));
                    else if (browseEditor != null)
                        setItem(i, getIcons().get("browseSlot"));
                    i++;
                    continue;
                } else if (pattern.getItems().get(c) != null && !pattern.getItems().get(c).getType().isAir()) {
                    setItem(i, EditorRegistry.getPatternEditorCfg().getPatternItem(c, pattern.getItems().get(c)));
                }
                i++;
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() != getInventory()) return;
        event.setCancelled(true);
        boolean hasChanges = false;
        if (event.getClickedInventory() == getInventory()) {
            event.setCancelled(true);
            int slot = event.getSlot();
            if (slots.containsKey(slot)) {
                if (event.isLeftClick()) {
                    char cycled = pattern.getCycledCharacter(pattern.getSlot(slot), true);
                    pattern.replaceSlot(event.getSlot(), cycled);
                    hasChanges = true;
                } else if (event.isRightClick()) {
                    char cycled = pattern.getCycledCharacter(pattern.getSlot(slot), false);
                    pattern.replaceSlot(event.getSlot(), cycled);
                    hasChanges = true;
                }
            }
        }
        if (hasChanges) {
            if (table != null)
                setIcons(EditorRegistry.getPatternEditorCfg().getIcons(table));
            else
                setIcons(EditorRegistry.getPatternEditorCfg().getIcons(browseEditor));
            initialize();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory() != getInventory()) return;
        Bukkit.getScheduler().runTaskLater(Fusion.getInstance(), () -> openParent(player), 1);
    }
}
