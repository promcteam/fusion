package studio.magemonkey.fusion.gui.editors.pattern;

import lombok.Getter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import studio.magemonkey.codex.api.DelayedCommand;
import studio.magemonkey.codex.legacy.item.ItemBuilder;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.editors.EditorCriteria;
import studio.magemonkey.fusion.cfg.editors.EditorRegistry;
import studio.magemonkey.fusion.commands.FusionEditorCommand;
import studio.magemonkey.fusion.data.professions.pattern.InventoryPattern;
import studio.magemonkey.fusion.data.recipes.CraftingTable;
import studio.magemonkey.fusion.gui.editors.Editor;
import studio.magemonkey.fusion.gui.editors.browse.BrowseEditor;
import studio.magemonkey.fusion.util.InventoryUtils;

import java.util.ArrayList;

public class PatternItemEditor extends Editor implements Listener {

    // Globally used variabled
    private final Player           player;
    private final InventoryPattern pattern;
    private final char             c;

    // Profession only
    private final CraftingTable table;
    private final boolean       isCategoryPattern;

    // Browse only
    private final BrowseEditor browseEditor;

    @Getter
    private ItemBuilder builder;

    public PatternItemEditor(Editor parentEditor,
                             Player player,
                             CraftingTable table,
                             char c,
                             boolean isCategoryPattern) {
        super(parentEditor, EditorRegistry.getPatternItemEditorCfg().getSubTitle(c), 45);
        this.player = player;
        this.table = table;
        this.c = c;
        this.isCategoryPattern = isCategoryPattern;
        this.pattern = isCategoryPattern ? table.getCatPattern() : table.getPattern();
        this.browseEditor = null;

        ItemStack item = pattern.getItems().get(c);
        builder = ItemBuilder.newItem(item);
        setIcons(EditorRegistry.getPatternItemEditorCfg().getSubIcons(c, builder, pattern.getCommands(c)));
        initialize();
        Fusion.registerListener(this);
    }

    public PatternItemEditor(Editor editor, BrowseEditor browseEditor, Player player, char c) {
        super(editor, EditorRegistry.getPatternItemEditorCfg().getSubTitle(c), 45);
        this.player = player;
        this.browseEditor = browseEditor;
        this.c = c;
        this.table = null;
        this.isCategoryPattern = false;

        this.pattern = browseEditor.getBrowsePattern();
        ItemStack item = pattern.getItems().get(c);
        builder = ItemBuilder.newItem(item);
        setIcons(EditorRegistry.getPatternItemEditorCfg().getSubIcons(c, builder, pattern.getCommands(c)));
        initialize();
        Fusion.registerListener(this);
    }

    private void initialize() {
        InventoryUtils.fillInventory(getInventory(), getIcons().get("fill"));
        setItem(10, getIcons().get("name"));
        setItem(11, getIcons().get("lore"));
        setItem(13, getIcons().get("patternItem"));
        setItem(15, getIcons().get("glowing"));
        setItem(16, getIcons().get("commands"));
        setItem(29, getIcons().get("durability"));
        setItem(30, getIcons().get("unbreakable"));
        setItem(32, getIcons().get("enchants"));
        setItem(33, getIcons().get("flags"));
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
                    EditorCriteria.Pattern_Edit_Name,
                    "/fusion-editor " + builder.getName());
            case 11 -> {
                if (event.isLeftClick()) {
                    FusionEditorCommand.suggestUsage(player, EditorCriteria.Pattern_Edit_Lore, "/fusion-editor <lore>");
                } else {
                    if (builder.getLore().isEmpty()) {
                        return;
                    }
                    builder.getLore().remove(builder.getLore().size() - 1);
                    hasChanges = true;
                }
            }
            case 13 -> FusionEditorCommand.suggestUsage(player,
                    EditorCriteria.Pattern_Edit_Pattern,
                    "/fusion-editor " + builder.getMaterial().getNamespacedID() + " " + builder.getAmount());
            case 15 -> {
                if (!builder.getEnchants().isEmpty()) {
                    builder.clearEnchants();
                    builder.clearFlags();
                } else {
                    builder.enchant(Enchantment.UNBREAKING, 1);
                    builder.flag(ItemFlag.HIDE_ENCHANTS);
                }
                hasChanges = true;
            }
            case 16 -> {
                if (event.isLeftClick()) {
                    FusionEditorCommand.suggestUsage(player,
                            EditorCriteria.Pattern_Add_Commands,
                            "/fusion-editor <caster> <delay> <command without />");
                } else {
                    if (pattern.getCommands(c).isEmpty()) {
                        return;
                    }
                    DelayedCommand command =
                            new ArrayList<>(pattern.getCommands(c)).get(pattern.getCommands(c).size() - 1);
                    pattern.getCommands(c).remove(command);
                    hasChanges = true;
                }
            }
            case 26 -> {
                reload(false);
                ((PatternItemsEditor) getParentEditor()).reload(true);
                return;
            }
            case 29 -> {
                int amount = event.isShiftClick() ? 25 : 1;
                if (event.isLeftClick()) {
                    builder.durability(builder.getDurability() + amount);
                    hasChanges = true;
                } else if (event.isRightClick()) {
                    if (builder.getDurability() == 0) return;
                    builder.durability(Math.max(builder.getDurability() - amount, 0));
                    hasChanges = true;
                }
                hasChanges = true;
            }
            case 30 -> {
                builder.unbreakable(!builder.isUnbreakable());
                hasChanges = true;
            }
            case 32 -> {
                if (event.isLeftClick()) {
                    FusionEditorCommand.suggestUsage(player,
                            EditorCriteria.Pattern_Add_Enchants,
                            "/fusion-editor <enchantment> [level]");
                } else if (event.isRightClick()) {
                    if (builder.getEnchants().isEmpty()) {
                        return;
                    }
                    // Get last entry and remove it
                    Enchantment lastEnchantment =
                            new ArrayList<>(builder.getEnchants().keySet()).get(builder.getEnchants().size() - 1);
                    builder.unEnchant(lastEnchantment);
                    hasChanges = true;
                }
            }
            case 33 -> {
                if (event.isLeftClick()) {
                    FusionEditorCommand.suggestUsage(player, EditorCriteria.Pattern_Add_Flags, "/fusion-editor <flag>");
                } else if (event.isRightClick()) {
                    if (builder.getFlags().isEmpty()) {
                        return;
                    }
                    builder.getFlags().remove(builder.getFlags().get(builder.getFlags().size() - 1));
                    hasChanges = true;
                }
            }
            case 44 -> {
                getParentEditor().open(player);
                return;
            }
        }

        if (hasChanges) {
            reload(true);
        }
    }

    public void reload(boolean open) {
        if (table != null) {
            if (isCategoryPattern) {
                table.getCatPattern().getItems().put(c, builder.build());
            } else {
                table.getPattern().getItems().put(c, builder.build());
            }
        } else {
            browseEditor.getBrowsePattern().getItems().put(c, builder.build());
        }
        setIcons(EditorRegistry.getPatternItemEditorCfg().getSubIcons(c, builder, pattern.getCommands(c)));
        initialize();
        if (open)
            open(player);
    }

    public void addCommand(DelayedCommand command) {
        pattern.getCommands(c).add(command);
    }
}
