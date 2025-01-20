package studio.magemonkey.fusion.gui.editors.browse;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.editors.EditorCriteria;
import studio.magemonkey.fusion.cfg.editors.EditorRegistry;
import studio.magemonkey.fusion.commands.FusionEditorCommand;
import studio.magemonkey.fusion.data.professions.ProfessionConditions;
import studio.magemonkey.fusion.gui.editors.Editor;
import studio.magemonkey.fusion.util.InventoryUtils;

public class BrowseProfessionEditor extends Editor implements Listener {

    private final Player player;

    @Getter
    private ProfessionConditions conditions;

    public BrowseProfessionEditor(Editor editor, Player player, ProfessionConditions conditions) {
        super(editor, EditorRegistry.getBrowseProfessionCfg().getSubTitle(conditions.getProfession()), 36);
        this.player = player;
        this.conditions = conditions;
        setIcons(EditorRegistry.getBrowseProfessionCfg().getSubIcons(conditions));
        initialize();
        Fusion.registerListener(this);
    }

    private void initialize() {
        InventoryUtils.fillInventory(getInventory(), getIcons().get("fill"));
        setItem(11, getIcons().get("moneyCost"));
        setItem(13, getIcons().get("expCost"));
        setItem(15, getIcons().get("rank"));
        setItem(21, getIcons().get("ingredients"));
        setItem(23, getIcons().get("conditions"));
        setItem(35, getIcons().get("back"));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() != getInventory()) return;
        event.setCancelled(true);
        Player  player     = (Player) event.getWhoClicked();
        boolean hasChanges = false;

        switch (event.getSlot()) {
            case 11 -> {
                int amount = event.isShiftClick() ? 10 : 1;
                if (event.isLeftClick()) {
                    conditions.setMoneyCost(conditions.getMoneyCost() + amount);
                    hasChanges = true;
                } else if (event.isRightClick()) {
                    if (conditions.getMoneyCost() == 0) return;
                    conditions.setMoneyCost(Math.max(conditions.getMoneyCost() - amount, 0));
                    hasChanges = true;
                }
            }
            case 13 -> {
                int amount = event.isShiftClick() ? 10 : 1;
                if (event.isLeftClick()) {
                    conditions.setExpCost(conditions.getExpCost() + amount);
                    hasChanges = true;
                } else if (event.isRightClick()) {
                    if (conditions.getExpCost() == 0) return;
                    conditions.setExpCost(Math.max(conditions.getExpCost() - amount, 0));
                    hasChanges = true;
                }
            }
            case 15 -> {
                if (event.isLeftClick()) {
                    FusionEditorCommand.suggestUsage(player,
                            EditorCriteria.Browse_Profession_Edit_Rank,
                            "/fusion-editor <rank>");
                } else if (event.isRightClick()) {
                    if (conditions.getPermission() == null)
                        return;
                    conditions.setPermission(null);
                    hasChanges = true;
                }
            }
            case 21 -> FusionEditorCommand.suggestUsage(player,
                    EditorCriteria.Browse_Profession_Add_Ingredients,
                    "/fusion-editor <ingredient> <amount>");
            case 23 -> FusionEditorCommand.suggestUsage(player,
                    EditorCriteria.Browse_Profession_Add_Conditions,
                    "/fusion-editor <conditionKey> <conditionValue> <level>");
            case 35 -> {
                reload(false);
                ((BrowseProfessionsEditor) getParentEditor()).reload(true);
            }
        }

        if (hasChanges) {
            reload(true);
        }
    }

    public void reload(boolean open) {
        setIcons(EditorRegistry.getBrowseProfessionCfg().getSubIcons(conditions));
        initialize();
        if (open)
            open(player);
    }
}
