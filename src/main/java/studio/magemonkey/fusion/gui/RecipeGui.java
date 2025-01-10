package studio.magemonkey.fusion.gui;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.codex.api.DelayedCommand;
import studio.magemonkey.codex.api.Replacer;
import studio.magemonkey.codex.util.ItemUtils;
import studio.magemonkey.codex.util.messages.MessageData;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.api.FusionAPI;
import studio.magemonkey.fusion.cfg.Cfg;
import studio.magemonkey.fusion.cfg.CraftingRequirementsCfg;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.data.player.PlayerLoader;
import studio.magemonkey.fusion.data.professions.pattern.Category;
import studio.magemonkey.fusion.data.professions.pattern.InventoryPattern;
import studio.magemonkey.fusion.data.queue.CraftingQueue;
import studio.magemonkey.fusion.data.queue.QueueItem;
import studio.magemonkey.fusion.data.recipes.CalculatedRecipe;
import studio.magemonkey.fusion.data.recipes.CraftingTable;
import studio.magemonkey.fusion.data.recipes.Recipe;
import studio.magemonkey.fusion.data.recipes.RecipeItem;
import studio.magemonkey.fusion.gui.slot.Slot;
import studio.magemonkey.fusion.util.*;

import java.util.*;

@Getter
public class RecipeGui implements Listener {

    private final Player player;
    private final CraftingTable table;
    @Getter
    protected final String name;
    @Getter
    private final String inventoryName;
    private final Category category;
    private InventoryPattern pattern;
    private final HashMap<Integer, CalculatedRecipe> recipes;

    private int page = 0;
    private int nextPage;
    private int prevPage;

    /* Queue Crafting Mode */
    private int queuePage = 0;
    private int prevQueuePage;
    private int nextQueuePage;
    private CraftingQueue queue;

    /* Manual Crafting Mode */
    private BukkitTask craftingTask;
    private BukkitTask barTask;
    private BossBar bar;
    private final Collection<ItemStack> refund = new ArrayList<>();
    private ItemStack previousCursor;
    private boolean craftingSuccess = true;
    private Recipe craftingRecipe = null;


    /* Inventory Slot Handling */
    @Getter
    @Setter
    private Inventory inventory;
    private Slot[] slots;
    private final ArrayList<Integer> resultSlots = new ArrayList<>(20);
    private final ArrayList<Integer> blockedSlots = new ArrayList<>(20);
    private final ArrayList<Integer> queuedSlots = new ArrayList<>(20);

    public RecipeGui(Player player, CraftingTable table, Category category) {
        this.player = player;
        this.table = table;
        this.name = table.getName();
        this.inventoryName = ChatUT.hexString(table.getInventoryName());
        this.recipes = new HashMap<>(20);
        this.category = category != null ? category : new Category("master", "PAPER", this.table.getRecipePattern(), 1);
        if (this.category.getPattern() == null)
            this.category.setPattern(table.getRecipePattern());

        if (this.category.getName().equals("master")) {
            this.category.getRecipes().addAll(this.table.getRecipes().values());
        }
        setPattern();
        if (Cfg.craftingQueue && pattern != null) {
            this.queue = PlayerLoader.getPlayer(player).getQueue(table.getName(), this.category);
        }
        Fusion.registerListener(this);
        initialize();
    }

    private void mapSlots() {
        this.resultSlots.clear();
        this.queuedSlots.clear();
        this.slots = new Slot[pattern.getPattern().length * 9];
        int k = -1;
        int prevPage = -1, nextPage = -1;
        int prevQueuePage = -1, nextQueuePage = -1;
        for (String row : this.pattern.getPattern()) {
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
                    case '-' -> {
                        this.slots[k] = Slot.QUEUED_SLOT;
                        this.queuedSlots.add(k);
                    }
                    case '}' -> {
                        this.slots[k] = Slot.BLOCKED_SLOT;
                        nextQueuePage = k;
                    }
                    case '{' -> {
                        this.slots[k] = Slot.BLOCKED_SLOT;
                        prevQueuePage = k;
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
        this.nextQueuePage = nextQueuePage;
        this.prevQueuePage = prevQueuePage;
    }

    public void updateBlockedSlots(MessageData[] data) {
        int totalItems = category.getRecipes().size();
        int queuedTotalItems = queue != null ? queue.getQueue().size() : 0;
        int fullPages = totalItems / resultSlots.size();
        int rest = totalItems % resultSlots.size();
        int pages = (rest == 0) ? fullPages : (fullPages + 1);

        boolean includeBack = category.hasPrevious();
        int queuedPages = -1;

        if (!queuedSlots.isEmpty()) {
            int queuedFullPages = queuedTotalItems / queuedSlots.size();
            int queuedRest = queuedTotalItems % queuedSlots.size();
            queuedPages = (queuedRest == 0) ? queuedFullPages : (queuedFullPages + 1);
        }

        int k = -1;
        HashMap<Character, ItemStack> items = pattern.getItems();

        ArrayList<Integer> leaveBlank = new ArrayList<>();
        ArrayList<Integer> fill = new ArrayList<>();
        for (String row : pattern.getPattern()) {
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
                if (c == '{' && (Cfg.craftingQueue && queuePage <= 0)) {
                    fill.add(k);
                    continue;
                }
                if (c == '}' && (Cfg.craftingQueue && ((queuedPages > -1) && (queuePage + 1 >= queuedPages)))) {
                    fill.add(k);
                    continue;
                }
                if (item != null && (c != '-')) {
                    inventory.setItem(k, item.clone());
                } else if (item != null) {
                    if (queue != null && queue.getQueuedItems().containsKey(k)) {
                        inventory.setItem(k, queue.getQueuedItems().get(k).getIcon());
                    } else {
                        inventory.setItem(k, ProfessionsCfg.getQueueSlot(name));
                    }
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
            inventory.setItem(index, ProfessionsCfg.getFillItem(name));
        }
    }

    public void initialize() {
        this.inventory = Bukkit.createInventory(null, pattern.getInventorySize(), inventoryName);
        mapSlots();
        reloadRecipes();
    }

    public void reloadRecipes() {
        if (!player.isOnline()) return;
        try {
            setPattern();

            /* Default setup */
            ItemStack fill = table.getFillItem();
            Collection<Recipe> allRecipes = new ArrayList<>(category.getRecipes());
            allRecipes.removeIf(r -> r.isHidden(player));
            int pageSize = resultSlots.size();
            int allRecipeCount = allRecipes.size();
            int i = 0;
            int page = this.page;

            int fullPages = allRecipeCount / pageSize;
            int rest = allRecipeCount % pageSize;
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

            Collection<ItemStack> playerItems = PlayerUtil.getPlayerItems(this.player);
            CalculatedRecipe[] calculatedRecipes =
                    new CalculatedRecipe[(page < pages) ? pageSize : ((rest == 0) ? pageSize : rest)];
            Recipe[] allRecipesArray = allRecipes.toArray(new Recipe[allRecipeCount]);

            Integer[] slots = resultSlots.toArray(new Integer[0]);
            for (Integer slot : slots) {
                if (slot != null) this.inventory.setItem(slot, null);
            }

            /* Additionally, when crafting_queue: true */
            if (Cfg.craftingQueue) {

                // Clear all queue slots
                Integer[] _queuedSlots = queuedSlots.toArray(new Integer[0]);
                for (int slot : _queuedSlots) {
                    this.inventory.setItem(slot, ProfessionsCfg.getQueueSlot(table.getName()));
                }

                this.queue.getQueuedItems().clear();
                Collection<QueueItem> allQueuedItems = queue.getQueue();
                int queueAllItemsCount = allQueuedItems.size();
                if (!allQueuedItems.isEmpty()) {
                    int queuePageSize = queuedSlots.size();
                    if (queuePageSize > 0) {
                        int j = 0;
                        int queuePage = this.queuePage;

                        int queueFullPages = queueAllItemsCount / queuePageSize;
                        int queueRest = queueAllItemsCount % queuePageSize;
                        int queuePages = (queueRest == 0) ? queueFullPages : (queueFullPages + 1);
                        if (queuePage >= queuePages) {
                            if (queuePage > 0)
                                this.queuePage = queuePages - 1;
                            this.reloadRecipes();
                            return;
                        }

                        QueueItem[] queuedItems = new QueueItem[queuePageSize];
                        QueueItem[] allQueueItemsArray = allQueuedItems.toArray(new QueueItem[queueAllItemsCount]);
                        Integer[] queuedSlots = this.queuedSlots.toArray(new Integer[0]);

                        for (int k = (queuePage * queuePageSize), e = queuedSlots.length;
                             (k < allQueueItemsArray.length) && (j < e);
                             k++, j++) {
                            QueueItem queueItem = allQueueItemsArray[k];
                            int slot = queuedSlots[j];
                            this.queue.getQueuedItems().put(slot, queuedItems[j] = queueItem);
                            this.queue.getQueuedItems().get(slot).updateIcon();

                            this.inventory.setItem(slot, queuedItems[j].getIcon().clone());
                        }
                    }
                }
            }
            updateBlockedSlots(new MessageData[]{
                    new MessageData("level", LevelFunction.getLevel(player, ProfessionsCfg.getTable(name))),
                    new MessageData("category", category),
                    new MessageData("gui", getName()),
                    new MessageData("player", player.getName()),
                    new MessageData("bal",
                            CodexEngine.get().getVault() == null ? 0
                                    : CodexEngine.get().getVault().getBalance(player))
            });

            for (int k = (page * pageSize), e = Math.min(slots.length, calculatedRecipes.length);
                 (k < allRecipesArray.length) && (i < e);
                 k++, i++) {
                Recipe recipe = allRecipesArray[k];
                int slot = slots[i];
                try {
                    CalculatedRecipe calculatedRecipe =
                            CalculatedRecipe.create(recipe, playerItems, this.player, table);
                    this.recipes.put(slot, calculatedRecipes[i] = calculatedRecipe);
                    this.inventory.setItem(slot, calculatedRecipe.getIcon().clone());
                } catch (InvalidPatternItemException ignored) {
                }
            }

            for (int k = 0; k < inventory.getSize(); k++) {
                if (inventory.getItem(k) != null && inventory.getItem(k).getType() != Material.AIR)
                    continue;
                inventory.setItem(k, fill);
            }
        } catch (
                Exception e) {
            this.inventory.clear();
            Bukkit.getScheduler().runTask(Fusion.getInstance(), this.player::closeInventory);
            throw new RuntimeException("Exception was thrown when reloading recipes for: " + this.player.getName(),
                    e);
        } finally {
            if (Cfg.craftingQueue && !queue.getQueuedItems().isEmpty()) {
                boolean requiresUpdate = false;
                for (Map.Entry<Integer, QueueItem> entry : queue.getQueuedItems().entrySet()) {
                    if (!entry.getValue().isDone()) {
                        requiresUpdate = true;
                        break;
                    }
                }
                if (requiresUpdate) {
                    Bukkit.getScheduler().runTaskLater(Fusion.getInstance(), this::reloadRecipes, 20L);
                }
            }
        }
    }

    public void reloadRecipesTask() {
        Bukkit.getScheduler().runTaskLater(Fusion.getInstance(), this::reloadRecipes, 1L);
    }

    private boolean validatePageCount() {
        if (this.page <= 0) {
            this.reloadRecipesTask();
            return false;
        }
        Collection<Recipe> allRecipes = table.getRecipes().values();
        int pageSize = resultSlots.size();
        int allRecipeCount = allRecipes.size();
        int page = this.page;

        int fullPages = allRecipeCount / pageSize;
        int rest = allRecipeCount % pageSize;
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
            cancel(true);
            ProfessionsCfg.getGUI(name).open(player);
            return;
        }
        this.page--;
        if (this.validatePageCount()) {
            this.reloadRecipesTask();
        }
    }

    private void nextPage() {
        this.page++;
        if (this.validatePageCount()) {
            this.reloadRecipesTask();
        }
    }

    private boolean validateQueuePageCount() {
        if (this.queuePage <= 0) {
            this.reloadRecipesTask();
            return false;
        }
        Collection<QueueItem> allQueuedItems = queue.getQueue();
        int pageSize = queuedSlots.size();
        int count = allQueuedItems.size();
        int page = this.queuePage;

        int fullPages = count / pageSize;
        int rest = count % pageSize;
        int pages = (rest == 0) ? fullPages : (fullPages + 1);
        if (page >= pages) {
            this.queuePage = pages;
            this.reloadRecipesTask();
            return false;
        }
        return true;
    }

    private void prevQueuePage() {
        if (this.queuePage <= 0)
            return;
        this.queuePage--;
        if (this.validateQueuePageCount()) {
            this.reloadRecipesTask();
        }
    }

    private void nextQueuePage() {
        this.queuePage++;
        if (this.validateQueuePageCount()) {
            this.reloadRecipesTask();
        }
    }

    public void setPattern() {
        this.pattern = category.getPattern();
        if (!pattern.getItems().containsKey('<') || pattern.getItems().containsKey('>') || pattern.getItems().containsKey('{') || pattern.getItems().containsKey('}')) {
            this.pattern.setItems(table.getRecipePattern().getItems());
        }
        mapSlots();
    }

    public void resetPattern() {
        this.pattern = category.getPattern();
        mapSlots();
    }

    public void setSlot(int i, Slot slot) {
        this.slots[i] = slot;
    }

    public Slot getSlot(int i) {
        return this.slots[i];
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    /* Manage Crafting Algorithm */
    private boolean canCraft(CalculatedRecipe calculatedRecipe, int slot) {
        Recipe recipe = calculatedRecipe.getRecipe();
        if (calculatedRecipe.getRecipe().getConditions().isMastery() && !PlayerLoader.getPlayer(player)
                .hasMastered(table.getName())) {
            CodexEngine.get().getMessageUtil().sendMessage("fusion.error.noMastery",
                    player,
                    new MessageData("craftingTable", ProfessionsCfg.getTable(table.getName())));
            return false;
        }
        if (!calculatedRecipe.isCanCraft()) {
            player.sendMessage(CraftingRequirementsCfg.getCanCraft(false));
            return false;
        }

        if (!Objects.equals(this.recipes.get(slot), calculatedRecipe)) {
            return false;
        }
        if (LevelFunction.getLevel(player, table) < recipe.getConditions().getProfessionLevel()) {
            CodexEngine.get()
                    .getMessageUtil()
                    .sendMessage("fusion.error.noLevel", player, new MessageData("recipe", recipe));
            return false;
        }
        if (ExperienceManager.getTotalExperience(this.player) < recipe.getConditions().getExpCost()) {
            CodexEngine.get()
                    .getMessageUtil()
                    .sendMessage("fusion.error.noXP", player, new MessageData("recipe", recipe));
            return false;
        }
        if (recipe.getConditions().getMoneyCost() != 0 && CodexEngine.get().getVault() != null
                && !CodexEngine.get().getVault().canPay(this.player, recipe.getConditions().getMoneyCost())) {
            CodexEngine.get()
                    .getMessageUtil()
                    .sendMessage("fusion.error.noFunds", player, new MessageData("recipe", recipe));
            return false;
        }
        return true;
    }

    private boolean craft(int slot, boolean addToCursor) {
        if (!recipes.containsKey(slot)) {
            return false;
        }
        CalculatedRecipe calculatedRecipe = this.recipes.get(slot);
        Recipe recipe = calculatedRecipe.getRecipe();
        if (craftingRecipe != null && craftingRecipe.equals(recipe)) {
            cancel(true);
            return false;
        }

        cancel(true);
        if (!canCraft(calculatedRecipe, slot)) return false;

        RecipeItem recipeResult = recipe.getResults().getResultItem();
        ItemStack resultItem = recipeResult.getItemStack();

        //Add "Crafted by"
        if (player.hasPermission("fusion.craftedby." + recipe.getName())) {
            ItemMeta meta = resultItem.getItemMeta();

            List<String> lore = (meta != null && meta.hasLore()) ? meta.getLore() : new ArrayList<>();
            lore.add(ChatColor.WHITE + " - " + ChatColor.YELLOW + "Crafted by: " + ChatColor.WHITE + player.getName());
            meta.setLore(lore);
            resultItem.setItemMeta(meta);
        }

        if (addToCursor) {
            ItemStack cursor = this.player.getItemOnCursor();
            if (resultItem.isSimilar(cursor)) {
                if ((resultItem.getAmount() + cursor.getAmount()) > resultItem.getMaxStackSize()) {
                    return false;
                }
            } else if (cursor.getType() != Material.AIR) {
                return false;
            }
        }

        Collection<ItemStack> itemsToTake = recipe.getItemsToTake();
        Collection<ItemStack> taken = new ArrayList<>(itemsToTake.size());
        PlayerInventory inventory = this.player.getInventory();

        for (Iterator<ItemStack> iterator = itemsToTake.iterator(); iterator.hasNext(); ) {
            ItemStack toTake = iterator.next();
            for (ItemStack entry : PlayerUtil.getPlayerItems(player)) {
                ItemStack item = entry.clone();
                entry = entry.clone();
                item = item.clone();
                entry.setAmount(toTake.getAmount());

                if (CalculatedRecipe.isSimilar(toTake, item)) {
                    toTake = entry;
                    break;
                }
            }

            HashMap<Integer, ItemStack> notRemoved = inventory.removeItem(toTake);
            if (notRemoved.isEmpty()) {
                taken.add(toTake);
                iterator.remove();
                continue;
            }
            for (ItemStack itemStack : taken) {
                HashMap<Integer, ItemStack> notAdded = inventory.addItem(itemStack);
                if (notAdded.isEmpty()) {
                    break;
                }
                for (ItemStack stack : notAdded.values()) {
                    this.player.getWorld().dropItemNaturally(this.player.getLocation(), stack);
                }
            }
            break;
        }

        refund.addAll(taken);

        if (!itemsToTake.isEmpty()) {
            CodexEngine.get()
                    .getMessageUtil()
                    .sendMessage("fusion.error.insufficientItems", player, new MessageData("recipe", recipe));
            cancel(true);
            return false;
        }
        if (!Cfg.craftingQueue) {
            double modifier = Fusion.getInstance().getPlayerCooldown(player);
            int cooldown = modifier == 0d
                    ? recipe.getCraftingTime()
                    : (int) Math.round(recipe.getCraftingTime() - (recipe.getCraftingTime() * modifier));
            showBossBar(this.player, recipe.getResults().getResultItem().getItemStack(), cooldown);

            if (cooldown != 0) {
                previousCursor = player.getOpenInventory().getCursor();
                player.getOpenInventory().setCursor(new ItemStack(Material.BARRIER));
            }

            craftingSuccess = false;
            craftingRecipe = recipe;
            craftingTask = Fusion.getInstance().runTaskLater(cooldown, () -> {
                craftingSuccess = true;
                if (recipe.getResults().getCommands().isEmpty()) {
                    if (addToCursor) {
                        ItemStack cursor = this.player.getItemOnCursor();
                        if (cursor.isSimilar(recipe.getResults().getResultItem().getItemStack())) {
                            if (cursor.getAmount() < cursor.getMaxStackSize()
                                    && cursor.getAmount() + recipe.getResults().getResultItem().getAmount()
                                    <= cursor.getMaxStackSize()) {
                                cursor.setAmount(cursor.getAmount() + recipe.getResults().getResultItem().getAmount());
                                this.player.setItemOnCursor(cursor);
                            } else {
                                craftingSuccess = false;
                            }
                        } else if (cursor.getType() == Material.AIR) {
                            this.player.setItemOnCursor(resultItem);
                        } else {
                            craftingSuccess = false;
                        }
                    } else {
                        boolean fits = calcWillFit(resultItem);
                        if (fits) {
                            HashMap<Integer, ItemStack> notAdded = inventory.addItem(resultItem);
                            if (!notAdded.isEmpty()) {
                                for (ItemStack stack : notAdded.values()) {
                                    this.player.getWorld().dropItemNaturally(this.player.getLocation(), stack);
                                }
                            }
                        } else {
                            craftingSuccess = false;
                        }
                    }
                }

                if (craftingSuccess) {
                    cancel(false);
                    CodexEngine.get().getVault().take(this.player, recipe.getConditions().getMoneyCost());
                    //Commands
                    DelayedCommand.invoke(Fusion.getInstance(), player, recipe.getResults().getCommands());

                    //Experience
                    if (recipe.getResults().getProfessionExp() > 0) {
                        FusionAPI.getEventServices()
                                .getProfessionService()
                                .giveProfessionExp(player, table, recipe.getResults().getProfessionExp());
                    }
                    if (recipe.getResults().getVanillaExp() > 0) {
                        player.giveExp(recipe.getResults().getVanillaExp());
                    }

                    //Restart the crafting sequence if auto-crafting is enabled
                    if (PlayerLoader.getPlayer(player).isAutoCrafting() && !this.recipes.isEmpty()) {
                        reloadRecipesTask();
                        boolean success = craft(slot, addToCursor); //Call this method again recursively
                        if (!success)
                            CodexEngine.get().getMessageUtil().sendMessage("fusion.autoCancelled", player);
                    }
                } else {
                    cancel(true);
                }
            });
        } else {
            if (recipe.getConditions().getMoneyCost() != 0 && CodexEngine.get().getVault() != null)
                CodexEngine.get().getVault().take(this.player, recipe.getConditions().getMoneyCost());
            this.queue.addRecipe(this.recipes.get(slot).getRecipe());
        }
        return true;
    }

    private boolean calcWillFit(ItemStack item) {
        Inventory inv = Bukkit.createInventory(null, InventoryType.PLAYER);
        inv.setContents(inventory.getContents());
        return inv.addItem(item).isEmpty();
    }

    public void executeCommands(Character c, HumanEntity player) {
        Collection<DelayedCommand> patternCommands = getPattern().getCommands(c);
        if (patternCommands != null && !patternCommands.isEmpty()) {
            DelayedCommand.invoke(Fusion.getInstance(), player, patternCommands,
                    Replacer.replacer("{crafting}", getName()),
                    Replacer.replacer("{inventoryName}", getInventoryName()));
        }
    }

    /* Manual Crafting Options */
    private void showBossBar(Player target, ItemStack item, double cooldown) {
        if (cooldown == 0)
            return;

        bar = Bukkit.createBossBar(CraftingRequirementsCfg.getBossBarTitle(item),
                BarColor.BLUE,
                BarStyle.SOLID,
                BarFlag.PLAY_BOSS_MUSIC);
        bar.setProgress(0d);
        bar.addPlayer(target);
        barTask = new BukkitRunnable() {
            int count = 0;

            public void run() {
                if (count >= cooldown * 20) {
                    this.cancel();
                    bar.removeAll();
                }

                bar.setProgress(Math.min(1d, count / (cooldown * 20)));

                count++;
            }
        }.runTaskTimer(Fusion.getInstance(), 1L, 1L);
    }

    private void cancel(boolean refund) {
        if (!Cfg.craftingQueue) {
            if (craftingTask == null) return;
            craftingRecipe = null;
            if (barTask != null) {
                barTask.cancel();
                barTask = null;
                bar.removeAll();
                bar = null;
            }

            if (!craftingSuccess && PlayerLoader.getPlayer(player).isAutoCrafting()) {
                CodexEngine.get().getMessageUtil().sendMessage("fusion.autoCancelled", player);
            }

            if (player.getOpenInventory().getCursor() != null
                    && player.getOpenInventory().getCursor().getType() == Material.BARRIER)
                if (previousCursor != null) {
                    player.getOpenInventory().setCursor(previousCursor);
                    previousCursor = null;
                } else
                    player.getOpenInventory().setCursor(new ItemStack(Material.AIR));

            if (craftingTask != null)
                craftingTask.cancel();
            craftingTask = null;

            if (!refund || craftingSuccess)
                return;


            PlayerInventory inventory = player.getInventory();
            Collection<ItemStack> notAdded = inventory.addItem(this.refund.toArray(new ItemStack[0])).values();
            if (!notAdded.isEmpty()) {
                for (ItemStack item : notAdded) {
                    player.getLocation().getWorld().dropItemNaturally(player.getLocation(), item);
                }
            }
            this.refund.clear();
        }
    }

    /* Event-Handling */
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

        Character c = pattern.getSlot(event.getRawSlot());
        executeCommands(c, event.getWhoClicked());

        //Close on click
        if (pattern.getCloseOnClickSlots().contains(c)) {
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
            if ((nextQueuePage != -1) && event.getSlot() == nextQueuePage) {
                // Open the next queue page
                this.nextQueuePage();
                return;
            }
            if (prevQueuePage != -1 && event.getSlot() == prevQueuePage) {
                // Open the previous queue page
                this.prevQueuePage();
            }
            return;
        }
        if (slots[event.getRawSlot()].equals(Slot.BASE_RESULT_SLOT)) {
            event.setCancelled(true);
            event.setResult(Event.Result.DENY);
            Fusion.getInstance().runSync(() ->
            {
                this.reloadRecipes();
                this.craft(event.getRawSlot(), false);
                this.reloadRecipesTask();
            });
            return;
        }
        if (slots[event.getRawSlot()].equals(Slot.QUEUED_SLOT)) {
            event.setCancelled(true);
            event.setResult(Event.Result.DENY);
            if (queuedSlots.contains(event.getSlot())) {
                // Interact with a queued item
                QueueItem item = queue.getQueuedItems().get(event.getSlot());
                if (item == null) return;
                if (item.isDone()) {
                    if (event.isLeftClick()) {
                        queue.finishRecipe(item);
                        this.reloadRecipes();
                    } else if (event.isRightClick()) {
                        int queueSize =
                                queue.getQueue().size(); // Estimated time that is required to finish all recipes
                        queue.finishAllRecipes();
                        Bukkit.getScheduler().runTaskLater(Fusion.getInstance(), this::reloadRecipes, queueSize + 1);
                    }
                } else {
                    queue.removeRecipe(item, true);
                }
            }
            return;
        }
        if (event.getCursor().getType() != Material.AIR) {

            if (Slot.SPECIAL_CRAFTING_SLOT.canHoldItem(event.getCursor()) == null) {
                event.setResult(Event.Result.DENY);
                return;
            }
        }
        this.reloadRecipesTask();
    }

    private void close(Player p, Inventory inv) {
        if (inv == null) {
            return;
        }
        Inventory pInventory = p.getInventory();
        if (inv.equals(this.inventory)) {
            for (int i = 0; i < this.slots.length; i++) {
                if (this.slots[i].equals(Slot.BLOCKED_SLOT) || this.slots[i].equals(Slot.BASE_RESULT_SLOT)
                        || this.slots[i].equals(Slot.QUEUED_SLOT)) {
                    continue;
                }
                ItemStack it = inv.getItem(i);
                if (it != null) {
                    pInventory.addItem(it)
                            .values()
                            .stream()
                            .filter(Objects::nonNull)
                            .forEach(itemStack -> p.getWorld().dropItem(p.getLocation(), itemStack));
                }
            }
            cancel(true);
            inv.clear();
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory() != getInventory()) return;
        if (event.getRawSlot() < 0) {
            return;
        }
        click(event);
    }

    /*
     Event to prevent the player from dragging items into the crafting slots
     while doing manual crafting
     */
    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) {
            return;
        }
        if (e.getInventory().equals(this.inventory) && !Cfg.craftingQueue) {
            if (e.getOldCursor().getType() == Material.BARRIER)
                e.setCancelled(true);
            if (e.getRawSlots()
                    .stream()
                    .anyMatch(i -> (i < this.slots.length) && (!Objects.equals(this.slots[i],
                            Slot.SPECIAL_CRAFTING_SLOT)))) {
                e.setResult(Event.Result.DENY);
                return;
            }

            if (e.getNewItems().values().stream().anyMatch(i -> Slot.SPECIAL_CRAFTING_SLOT.canHoldItem(i) == null)) {
                e.setResult(Event.Result.DENY);
            }
            reloadRecipesTask();
        }
    }

    /*
        Event to prevent the player from dropping items into the crafting slots
        while doing manual crafting
     */
    @EventHandler
    public void drop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (this.getInventory().getViewers().contains(player) && !Cfg.craftingQueue) {
            ItemStack stack = event.getItemDrop().getItemStack();
            if (stack.getType() == Material.BARRIER) {
                event.getItemDrop().remove();
                if (player.getOpenInventory().getCursor() == null
                        || player.getOpenInventory().getCursor().getType() == Material.AIR)
                    player.getOpenInventory().setCursor(stack);

                cancel(true);
            }
        }
    }

    // Events to close the players inventory
    @EventHandler(ignoreCancelled = true)
    public void onClose(InventoryCloseEvent e) {
        if (e.getPlayer() instanceof Player) {
            e.getInventory();
            close((Player) e.getPlayer(), e.getInventory());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onClose(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player)) {
            return;
        }
        reloadRecipesTask();
    }

    @EventHandler(ignoreCancelled = true)
    public void onExit(PlayerQuitEvent e) {
        close(e.getPlayer(), inventory);
    }
}
