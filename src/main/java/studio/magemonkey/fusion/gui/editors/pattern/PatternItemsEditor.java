package studio.magemonkey.fusion.gui.editors.pattern;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.editors.EditorRegistry;
import studio.magemonkey.fusion.data.professions.pattern.InventoryPattern;
import studio.magemonkey.fusion.data.recipes.CraftingTable;
import studio.magemonkey.fusion.gui.editors.Editor;
import studio.magemonkey.fusion.gui.editors.browse.BrowseEditor;
import studio.magemonkey.fusion.util.InventoryUtils;

import java.util.*;

public class PatternItemsEditor extends Editor implements Listener {

    // Globally used variabled
    private final Player           player;
    private       InventoryPattern pattern;

    // Profession only
    private final CraftingTable table;
    private final boolean       isCategoryPattern;

    // Browse only
    private final BrowseEditor browseEditor;


    @Getter
    private PatternItemEditor patternItemEditor;

    private final Map<Integer, Character>   slots              = new HashMap<>();
    private final Map<Character, ItemStack> visualPatternItems = new HashMap<>();

    List<Map.Entry<Boolean, Map.Entry<Character, ItemStack>>> clipboardUndo = new ArrayList<>();
    List<Map.Entry<Boolean, Map.Entry<Character, ItemStack>>> clipboardRedo = new ArrayList<>();

    public PatternItemsEditor(Editor parentEditor, Player player, CraftingTable table, boolean isCategoryPattern) {
        super(parentEditor, EditorRegistry.getPatternItemEditorCfg().getTitle(), 54);
        this.player = player;
        this.table = table;
        this.isCategoryPattern = isCategoryPattern;
        this.browseEditor = null;

        setIcons(EditorRegistry.getPatternItemEditorCfg().getIcons(table));
        this.pattern = isCategoryPattern ? table.getCatPattern() : table.getRecipePattern();
        if (this.pattern == null) {
            // TODO Translation
            table.setCatPattern(InventoryPattern.copy(table.getRecipePattern()));
            this.pattern = table.getCatPattern();
            player.sendMessage("§cNo category pattern found. Using default pattern as reference.");
        }

        initialize();
        Fusion.registerListener(this);
    }

    public PatternItemsEditor(BrowseEditor browseEditor, Player player) {
        super(browseEditor, EditorRegistry.getPatternItemEditorCfg().getTitle(), 54);
        this.player = player;
        this.browseEditor = browseEditor;
        this.table = null;
        this.isCategoryPattern = false;

        setIcons(EditorRegistry.getPatternItemEditorCfg().getIcons(browseEditor));
        this.pattern = browseEditor.getBrowsePattern();

        initialize();
        Fusion.registerListener(this);
    }

    private void initialize() {
        slots.clear();
        visualPatternItems.clear();

        InventoryUtils.fillInventory(getInventory(), getIcons().get("fill"));

        // Create a list to hold the visual pattern items for sorting
        List<Map.Entry<Character, ItemStack>> visualPatternItemsList = new ArrayList<>();

        for (Map.Entry<Character, ItemStack> entry : pattern.getItems().entrySet()) {
            if (entry.getValue() == null || entry.getValue().getType().isAir()) continue;
            visualPatternItems.put(entry.getKey(),
                    EditorRegistry.getPatternItemEditorCfg().getPatternItemIcon(entry.getKey(), entry.getValue()));
            visualPatternItemsList.add(Map.entry(entry.getKey(), visualPatternItems.get(entry.getKey())));
        }

        // Sort the list by Material
        visualPatternItemsList.sort(Comparator.comparing(e -> e.getValue().getType()));

        int slot = 9;
        for (Map.Entry<Character, ItemStack> entry : visualPatternItemsList) {
            slots.put(slot, entry.getKey());
            slot++;
        }

        setItem(3, getIcons().get("undo"));
        setItem(4, getIcons().get("add"));
        setItem(5, getIcons().get("redo"));
        setItem(53, getIcons().get("back"));

        for (int i = 9; i < 45; i++) {
            if (slots.containsKey(i)) {
                setItem(i, visualPatternItems.get(slots.get(i)));
            } else {
                setItem(i, new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE));
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() != getInventory()) return;
        boolean hasChanges = false;
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();

        if (event.getClickedInventory() == getInventory()) {
            event.setCancelled(true);
            switch (event.getSlot()) {
                case 3 -> {
                    if (clipboardUndo.isEmpty()) return;
                    Map.Entry<Boolean, Map.Entry<Character, ItemStack>> entry =
                            clipboardUndo.get(clipboardUndo.size() - 1);
                    if (entry.getKey()) {
                        pattern.getItems().put(entry.getValue().getKey(), entry.getValue().getValue());
                    } else {
                        pattern.getItems().remove(entry.getValue().getKey());
                    }
                    clipboardRedo.add(entry);
                    clipboardUndo.remove(clipboardUndo.size() - 1);
                    hasChanges = true;
                }
                case 5 -> {
                    if (clipboardRedo.isEmpty()) return;
                    Map.Entry<Boolean, Map.Entry<Character, ItemStack>> entry = clipboardRedo.get(0);
                    if (!entry.getKey()) {
                        pattern.getItems().put(entry.getValue().getKey(), entry.getValue().getValue());
                    } else {
                        pattern.getItems().remove(entry.getValue().getKey());
                    }
                    clipboardUndo.add(entry);
                    clipboardRedo.remove(0);
                    hasChanges = true;
                }
                case 53 -> openParent(player);
                default -> {
                    if (slots.containsKey(event.getSlot())) {
                        if (event.isLeftClick()) {
                            if (patternItemEditor == null) {
                                if (table != null)
                                    patternItemEditor = new PatternItemEditor(this,
                                            player,
                                            table,
                                            slots.get(event.getSlot()),
                                            isCategoryPattern);
                                else if (browseEditor != null)
                                    patternItemEditor = new PatternItemEditor(this,
                                            browseEditor,
                                            player,
                                            slots.get(event.getSlot()));
                            }
                            patternItemEditor.open(player);
                        } else {
                            char c = slots.get(event.getSlot());
                            switch (c) {
                                case 'o', '-', '<', '>', '{', '}', 'f' -> {
                                    CodexEngine.get()
                                            .getMessageUtil()
                                            .sendMessage("editor.patternItemPredefined", player);
                                    return;
                                }
                                default -> {
                                    if (pattern.isUsed(c)) {
                                        CodexEngine.get()
                                                .getMessageUtil()
                                                .sendMessage("editor.patternItemUsed", player);
                                        return;
                                    }
                                    clipboardUndo.add(Map.entry(true, Map.entry(c, pattern.getItems().get(c))));
                                    pattern.getItems().remove(c);
                                    hasChanges = true;
                                }
                            }
                        }
                    }
                }
            }
        } else if (event.getClickedInventory() == player.getInventory()) {
            ItemStack item = player.getInventory().getItem(event.getSlot());
            if (item == null || item.getType() == Material.AIR) return;
            char c = getFreeChar();
            if (c == ' ') {
                // TODO Translation
                player.sendMessage("§cNo more free slots available");
                return;
            }
            pattern.getItems().put(c, item);
            clipboardUndo.add(Map.entry(false, Map.entry(c, item)));
            hasChanges = true;
        }

        if (hasChanges) {
            reload(false);
        }
    }

    public void reload(boolean open) {
        if (table != null)
            setIcons(EditorRegistry.getPatternItemEditorCfg().getIcons(table));
        else
            setIcons(EditorRegistry.getPatternItemEditorCfg().getIcons(browseEditor));

        initialize();
        if (open)
            open(player);
    }

    private char getFreeChar() {
        String chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        for (int i = 0; i < chars.length(); i++) {
            if (pattern.getItems().size() == 37) return ' ';
            char c = chars.charAt(i);
            if (!pattern.getItems().containsKey(c)) {
                return c;
            }
        }
        return ' ';
    }
}
