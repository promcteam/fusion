package studio.magemonkey.fusion.gui;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.codex.api.DelayedCommand;
import studio.magemonkey.codex.api.Replacer;
import studio.magemonkey.codex.util.ItemUtils;
import studio.magemonkey.codex.util.messages.MessageData;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.data.professions.pattern.Category;
import studio.magemonkey.fusion.data.recipes.CraftingTable;
import studio.magemonkey.fusion.gui.slot.Slot;
import studio.magemonkey.fusion.util.ChatUT;
import studio.magemonkey.fusion.util.LevelFunction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Getter
public class CategoryGui implements Listener {

    private final Player        player;
    private final CraftingTable table;

    private       Inventory               inventory;
    private final Map<Integer, RecipeGui> categories = new HashMap<>();
    private       int                     page       = 0;
    private       int                     nextPage   = -1;
    private       int                     prevPage   = -1;

    private       Slot[]             slots;
    private final ArrayList<Integer> resultSlots  = new ArrayList<>(20);
    private final ArrayList<Integer> blockedSlots = new ArrayList<>(20);

    public CategoryGui(Player player, CraftingTable table) {
        this.player = player;
        this.table = table;
        initialize();
        Fusion.registerListener(this);
    }

    public void initialize() {
        this.categories.clear();
        this.resultSlots.clear();
        this.inventory = Bukkit.createInventory(null,
                table.getCatPattern().getInventorySize(),
                ChatUT.hexString(table.getInventoryName()));
        mapSlots();
        reloadCategories();
    }

    public void reloadCategories() {
        if (!player.isOnline()) return;
        try {
            /* Default setup */
            ItemStack            fill             = table.getFillItem();
            Collection<Category> allCategories    = new ArrayList<>(table.getCategories().values());
            int                  pageSize         = resultSlots.size();
            int                  allCategoryCount = allCategories.size();
            int                  i                = 0;
            int                  page             = this.page;

            int fullPages = allCategoryCount / pageSize;
            int rest      = allCategoryCount % pageSize;
            int pages     = (rest == 0) ? fullPages : (fullPages + 1);
            if (player.isOnline() && page >= pages) {
                if (page > 0) {
                    this.page = pages - 1;
                }

                // Add a check to prevent infinite recursion
                if (this.page != page) {  // Only reload if page has changed
                    this.reloadCategories();
                }
                return;
            }
            Category[] allCategoryArray = allCategories.toArray(new Category[allCategoryCount]);

            Integer[] slots = resultSlots.toArray(new Integer[0]);
            for (int slot : slots) {
                this.inventory.setItem(slot, null);
            }

            updateBlockedSlots(new MessageData[]{
                    new MessageData("level", table.getLevelFunction().getLevel(player)),
                    new MessageData("gui", table.getName()),
                    new MessageData("player", player.getName()),
                    new MessageData("bal",
                            CodexEngine.get().getVault() == null ? 0
                                    : CodexEngine.get().getVault().getBalance(player))
            });

            for (int k = (page * pageSize), e = Math.min(slots.length, allCategoryArray.length);
                 (k < allCategoryArray.length) && (i < e);
                 k++, i++) {
                Category category = allCategoryArray[k];
                int      slot     = slots[i];
                this.categories.put(slot, new RecipeGui(player, table, category));
                this.inventory.setItem(slot, category.getDisplayIcon());
            }

            for (int k = 0; k < inventory.getSize(); k++) {
                if (inventory.getItem(k) != null && inventory.getItem(k).getType() != Material.AIR)
                    continue;

                inventory.setItem(k, fill);
            }
        } catch (
                Exception e) {
            this.inventory.clear();
            this.player.closeInventory();
            throw new RuntimeException("Exception was thrown when reloading categories for: " + this.player.getName(),
                    e);
        }
    }

    public void reloadCategoriesTask() {
        Bukkit.getScheduler().runTaskLater(Fusion.getInstance(), this::reloadCategories, 1L);
    }

    public void updateBlockedSlots(MessageData[] data) {
        int totalItems = table.getCategories().size();
        int fullPages  = totalItems / resultSlots.size();
        int rest       = totalItems % resultSlots.size();
        int pages      = (rest == 0) ? fullPages : (fullPages + 1);

        boolean includeBack = false;

        int                           k     = -1;
        HashMap<Character, ItemStack> items = table.getCatPattern().getItems();

        ArrayList<Integer> leaveBlank = new ArrayList<>();
        ArrayList<Integer> fill       = new ArrayList<>();
        for (String row : table.getCatPattern().getPattern()) {
            for (char c : row.toCharArray()) {
                k++;
                ItemStack item = ItemUtils.replaceText(items.get(c), data);
                if (!includeBack && c == '<' && page <= 0) {
                    leaveBlank.add(k);
                    continue;
                }
                if (c == '>' && page + 1 >= pages) {
                    leaveBlank.add(k);
                    continue;
                }
                if (c == '{' || c == '}' || c == '-') {
                    fill.add(k);
                    continue;
                }
                if (item != null) {
                    inventory.setItem(k, item.clone());
                }
            }
        }

        for (Integer index : leaveBlank) {
            if (inventory.getSize() > index + 1)
                inventory.setItem(index, inventory.getItem(index + 1));
            else
                inventory.setItem(index, inventory.getItem(index - 1));
        }
        for (Integer index : fill) {
            inventory.setItem(index, ProfessionsCfg.getFillItem(table.getName()));
        }
    }

    private void mapSlots() {
        this.resultSlots.clear();
        this.slots = new Slot[table.getCatPattern().getPattern().length * 9];
        int k        = -1;
        int prevPage = -1, nextPage = -1;
        for (String row : table.getCatPattern().getPattern()) {
            for (char c : row.toCharArray()) {
                k++;
                switch (c) {
                    case '=', 'o' -> {
                        this.slots[k] = Slot.BASE_RESULT_SLOT;
                        this.resultSlots.add(k);
                    }
                    case '>' -> {
                        this.slots[k] = Slot.BLOCKED_SLOT;
                        nextPage = k;
                    }
                    case '<' -> {
                        this.slots[k] = Slot.BLOCKED_SLOT;
                        prevPage = k;
                    }
                    case '{', '}', '-' -> {
                    }
                    default -> {
                        this.slots[k] = Slot.BLOCKED_SLOT;
                        this.blockedSlots.add(k);
                    }
                }
            }
        }
        this.nextPage = nextPage;
        this.prevPage = prevPage;
    }

    private boolean validatePageCount() {
        if (this.page <= 0) {
            this.reloadCategoriesTask();
            return false;
        }
        Collection<Category> allCategories = table.getCategories().values();
        int                  pageSize      = resultSlots.size();
        int                  fullCount     = allCategories.size();
        int                  page          = this.page;

        int fullPages = fullCount / pageSize;
        int rest      = fullCount % pageSize;
        int pages     = (rest == 0) ? fullPages : (fullPages + 1);
        if (page >= pages) {
            this.page = pages;
            this.reloadCategoriesTask();
            return false;
        }
        return true;
    }

    private void prevPage() {
        if (this.page <= 0) {
            player.closeInventory();
            return;
        }
        this.page--;
        if (this.validatePageCount()) {
            Bukkit.getConsoleSender().sendMessage("[-] Validated: " + this.page);
            this.reloadCategoriesTask();
        }
    }

    private void nextPage() {
        this.page++;
        if (this.validatePageCount()) {
            Bukkit.getConsoleSender().sendMessage("[+] Validated: " + this.page);
            this.reloadCategoriesTask();
        }
    }

    public void open(Player player, Category category) {
        if(category == null) open(player);
        else {
            for(RecipeGui gui : categories.values()) {
                if(gui.getCategory().equals(category)) {
                    gui.open(player);
                    return;
                }
            }
        }
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    public void executeCommands(Character c, HumanEntity player) {
        Collection<DelayedCommand> patternCommands = table.getCatPattern().getCommands(c);
        if (patternCommands != null && !patternCommands.isEmpty()) {
            DelayedCommand.invoke(Fusion.getInstance(), player, patternCommands,
                    Replacer.replacer("{crafting}", table.getName()),
                    Replacer.replacer("{inventoryName}", table.getInventoryName()));
        }
    }

    public void click(InventoryClickEvent event) {
        event.setCancelled(true);
        if ((event.getRawSlot() >= slots.length)) {
            if (event.getCursor().getType() == Material.BARRIER)
                event.setCancelled(true);
            return;
        }
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            event.setCancelled(true);
            event.setResult(Event.Result.DENY);
            return;
        }

        Character c = table.getCatPattern().getSlot(event.getRawSlot());
        executeCommands(c, event.getWhoClicked());

        //Close on click
        if (table.getCatPattern().getCloseOnClickSlots().contains(c)) {
            Bukkit.getScheduler().runTask(Fusion.getInstance(), () -> event.getWhoClicked().closeInventory());
        }

        if (slots[event.getRawSlot()].equals(Slot.BLOCKED_SLOT)) {
            event.setCancelled(true);
            event.setResult(Event.Result.DENY);
            if ((nextPage != -1) && (event.getSlot() == nextPage)) {
                this.nextPage();
                return;
            }
            if (prevPage != -1 && event.getSlot() == prevPage) {
                this.prevPage();
                return;
            }
            return;
        }
        if (slots[event.getRawSlot()].equals(Slot.BASE_RESULT_SLOT)) {
            event.setCancelled(true);
            event.setResult(Event.Result.DENY);
            Fusion.getInstance().runSync(() -> categories.get(event.getRawSlot()).open((Player) event.getWhoClicked()));
            return;
        }
        this.reloadCategoriesTask();
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory() != getInventory()) return;
        event.setCancelled(true);
        event.setResult(Event.Result.DENY);

        Player player = (Player) event.getWhoClicked();
        int    slot   = event.getSlot();
        if (slot < 0) return;

        Character c = table.getCatPattern().getSlot(slot);
        executeCommands(c, event.getWhoClicked());

        if (table.getCatPattern().getCloseOnClickSlots().contains(c)) {
            Bukkit.getScheduler().runTask(Fusion.getInstance(), () -> event.getWhoClicked().closeInventory());
        }

        if ((nextPage != -1) && (event.getSlot() == nextPage)) {
            this.nextPage();
            return;
        } else if (prevPage != -1 && event.getSlot() == prevPage) {
            this.prevPage();
            return;
        }

        if (categories.containsKey(slot)) {
            categories.get(slot).open(player);
        }
    }
}
