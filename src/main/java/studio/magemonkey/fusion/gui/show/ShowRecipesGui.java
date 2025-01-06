package studio.magemonkey.fusion.gui.show;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.codex.api.DelayedCommand;
import studio.magemonkey.codex.api.Replacer;
import studio.magemonkey.codex.util.ItemUtils;
import studio.magemonkey.codex.util.messages.MessageData;
import studio.magemonkey.codex.util.messages.MessageUtil;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.cfg.ShowRecipesCfg;
import studio.magemonkey.fusion.data.recipes.CraftingTable;
import studio.magemonkey.fusion.data.recipes.Recipe;
import studio.magemonkey.fusion.data.recipes.RecipeItem;
import studio.magemonkey.fusion.gui.ProfessionGuiRegistry;
import studio.magemonkey.fusion.gui.slot.Slot;
import studio.magemonkey.fusion.util.ChatUT;
import studio.magemonkey.fusion.util.Utils;

import java.util.*;

public class ShowRecipesGui implements Listener {

    private final Player player;
    private final Map<Recipe, RecipeItem> recipes;

    private final Map<Integer, Recipe> recipeSlots = new HashMap<>();

    @Getter
    @Setter
    private Inventory inventory;
    private Slot[] slots;
    private final ArrayList<Integer> resultSlots = new ArrayList<>(20);
    private final ArrayList<Integer> blockedSlots = new ArrayList<>(20);

    private int page = 0;
    private int nextPage;
    private int prevPage;


    public ShowRecipesGui(Player player, Map<Recipe, RecipeItem> recipes) {
        this.player = player;
        this.recipes = recipes;
        initialize();
        Bukkit.getServer().getPluginManager().registerEvents(this, Fusion.getInstance());
    }

    public void initialize() {
        this.resultSlots.clear();
        this.inventory = Bukkit.createInventory(null,
                ShowRecipesCfg.getPattern().getInventorySize(),
                ShowRecipesCfg.getInventoryName(recipes.values().stream().toList().get(0)));
        mapSlots();
        reloadRecipes();
    }

    public void reloadRecipes() {
        if (!player.isOnline()) return;
        try {
            /* Default setup */
            ItemStack fill = ShowRecipesCfg.getFillItem();
            int pageSize = resultSlots.size();
            int allRecipesCount = recipes.size();
            int i = 0;
            int page = this.page;

            int fullPages = allRecipesCount / pageSize;
            int rest = allRecipesCount % pageSize;
            int pages = (rest == 0) ? fullPages : (fullPages + 1);
            if (player.isOnline() && page >= pages) {
                if (page > 0) {
                    this.page = pages - 1;
                }

                // Add a check to prevent infinite recursion
                if (this.page != page) {  // Only reload if page has changed
                    this.reloadRecipes();
                }
                return;
            }
            Recipe[] allRecipesArray = recipes.keySet().toArray(new Recipe[allRecipesCount]);

            Integer[] slots = resultSlots.toArray(new Integer[0]);
            for (int slot : slots) {
                this.inventory.setItem(slot, null);
            }

            updateBlockedSlots(new MessageData[]{
                    new MessageData("gui", ShowRecipesCfg.getInventoryName(recipes.values().stream().toList().get(0))),
                    new MessageData("player", player.getName()),
                    new MessageData("bal",
                            CodexEngine.get().getVault() == null ? 0
                                    : CodexEngine.get().getVault().getBalance(player))
            });

            for (int k = (page * pageSize), e = Math.min(slots.length, allRecipesArray.length);
                 (k < allRecipesArray.length) && (i < e);
                 k++, i++) {
                Recipe recipe = allRecipesArray[k];
                int slot = slots[i];
                this.recipeSlots.put(slot, recipe);
                this.inventory.setItem(slot, ShowRecipesCfg.getRecipeIcon(recipe, recipes.get(recipe)));
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

    public void reloadRecipesTask() {
        Bukkit.getScheduler().runTaskLater(Fusion.getInstance(), this::reloadRecipes, 1L);
    }

    public void updateBlockedSlots(MessageData[] data) {
        int totalItems = recipes.size();
        int fullPages = totalItems / resultSlots.size();
        int rest = totalItems % resultSlots.size();
        int pages = (rest == 0) ? fullPages : (fullPages + 1);

        boolean includeBack = false;

        int k = -1;
        HashMap<Character, ItemStack> items = ShowRecipesCfg.getPattern().getItems();

        ArrayList<Integer> leaveBlank = new ArrayList<>();
        ArrayList<Integer> fill = new ArrayList<>();
        for (String row : ShowRecipesCfg.getPattern().getPattern()) {
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
            inventory.setItem(index, ShowRecipesCfg.getFillItem());
        }
    }

    private void mapSlots() {
        this.resultSlots.clear();
        this.slots = new Slot[ShowRecipesCfg.getPattern().getPattern().length * 9];
        int k = -1;
        int prevPage = -1, nextPage = -1;
        for (String row : ShowRecipesCfg.getPattern().getPattern()) {
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
            this.reloadRecipesTask();
            return false;
        }
        int pageSize = resultSlots.size();
        int fullCount = recipes.size();
        int page = this.page;

        int fullPages = fullCount / pageSize;
        int rest = fullCount % pageSize;
        int pages = (rest == 0) ? fullPages : (fullPages + 1);
        if (page >= pages) {
            this.page = pages;
            this.reloadRecipesTask();
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
            this.reloadRecipesTask();
        }
    }

    private void nextPage() {
        this.page++;
        if (this.validatePageCount()) {
            Bukkit.getConsoleSender().sendMessage("[+] Validated: " + this.page);
            this.reloadRecipesTask();
        }
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    public void executeCommands(Character c, HumanEntity player) {
        Collection<DelayedCommand> patternCommands = ShowRecipesCfg.getPattern().getCommands(c);
        if (patternCommands != null && !patternCommands.isEmpty()) {
            DelayedCommand.invoke(Fusion.getInstance(), player, patternCommands);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory() != getInventory()) return;
        event.setCancelled(true);
        event.setResult(Event.Result.DENY);

        Player player = (Player) event.getWhoClicked();
        int    slot   = event.getSlot();
        if (slot < 0) return;

        Character c = ShowRecipesCfg.getPattern().getSlot(slot);
        executeCommands(c, event.getWhoClicked());

        if (ShowRecipesCfg.getPattern().getCloseOnClickSlots().contains(c)) {
            Bukkit.getScheduler().runTask(Fusion.getInstance(), () -> event.getWhoClicked().closeInventory());
        }

        if ((nextPage != -1) && (event.getSlot() == nextPage)) {
            this.nextPage();
            return;
        } else if (prevPage != -1 && event.getSlot() == prevPage) {
            this.prevPage();
            return;
        }

        if (recipeSlots.containsKey(slot)) {
            Recipe recipe = recipeSlots.get(slot);
            CraftingTable table = recipe.getTable();
            ProfessionsCfg.getGuiMap().get(table.getName()).open(player, table.getCategory(recipe.getCategory()));
        }
    }
}
