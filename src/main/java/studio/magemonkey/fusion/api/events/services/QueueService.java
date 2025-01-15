package studio.magemonkey.fusion.api.events.services;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.codex.api.DelayedCommand;
import studio.magemonkey.codex.util.messages.MessageData;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.api.FusionAPI;
import studio.magemonkey.fusion.api.events.QueueItemAddedEvent;
import studio.magemonkey.fusion.api.events.QueueItemFinishedEvent;
import studio.magemonkey.fusion.api.events.QueueItemRemovedEvent;
import studio.magemonkey.fusion.cfg.sql.SQLManager;
import studio.magemonkey.fusion.data.queue.CraftingQueue;
import studio.magemonkey.fusion.data.queue.QueueItem;
import studio.magemonkey.fusion.data.recipes.CraftingTable;
import studio.magemonkey.fusion.util.PlayerUtil;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class QueueService {

    /**
     * Call the QueueItemAddedEvent.
     * @param player The player that adds the item to the queue.
     * @param table The crafting table (profession) the player is using.
     * @param queue The crafting queue the player is using.
     * @param item The queue item that is added to the queue.
     */
    public void addQueueItem(Player player, CraftingTable table, CraftingQueue queue, QueueItem item) {
        QueueItemAddedEvent event = new QueueItemAddedEvent(table.getName(), player, queue, item);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            item.setCraftinQueue(event.getQueue());
            event.getQueue().getQueue().add(item);
        }
    }

    /**
     * Call the QueueItemCanceledEvent.
     * @param player The player that cancels the item in the queue.
     * @param table The crafting table (profession) the player is using.
     * @param queue The crafting queue the player is using.
     * @param item The queue item that is canceled.
     * @param finished If the item is finished.
     * @param refunded If the item ingredients will be refunded.
     * @param refundItems The items that will be refunded in case `refunded=true`.
     */
    public void cancelQueueItem(Player player,
                                CraftingTable table,
                                CraftingQueue queue,
                                QueueItem item,
                                boolean finished,
                                boolean refunded,
                                List<ItemStack> refundItems) {
        QueueItemRemovedEvent event =
                new QueueItemRemovedEvent(table.getName(), player, queue, item, finished, refunded, refundItems);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            if (event.isRefunded()) {
                CodexEngine.get().getVault().give(event.getPlayer(), item.getRecipe().getConditions().getMoneyCost());

                Collection<ItemStack> refunds = event.getRefundedItems();
                for (ItemStack refundItem : refunds) {
                    // If those are not stacked natively, we need to give them one by one
                    if (refundItem.getMaxStackSize() < refundItem.getAmount()) {
                        for (int i = 0; i < refundItem.getAmount(); i++) {
                            ItemStack singleItem = refundItem.clone();
                            singleItem.setAmount(1);
                            Collection<ItemStack> notAdded = player.getInventory().addItem(singleItem).values();
                            if (!notAdded.isEmpty()) {
                                for (ItemStack _item : notAdded) {
                                    Objects.requireNonNull(player.getLocation().getWorld())
                                            .dropItemNaturally(player.getLocation(), _item);
                                }
                            }
                        }
                    } else {
                        Collection<ItemStack> notAdded = player.getInventory().addItem(refundItem).values();
                        if (!notAdded.isEmpty()) {
                            for (ItemStack _item : notAdded) {
                                Objects.requireNonNull(player.getLocation().getWorld())
                                        .dropItemNaturally(player.getLocation(), _item);
                            }
                        }
                    }
                }
            }
            event.getQueue().getQueue().remove(item);
            for (Map.Entry<Integer, QueueItem> entry : event.getQueue().getQueuedItems().entrySet()) {
                if (entry.getValue().equals(item)) {
                    event.getQueue().getQueuedItems().remove(entry.getKey());
                    break;
                }
            }
            if (!SQLManager.queues().removeQueueItem(item)) {
                Fusion.getInstance().getLogger().warning("Failed to remove queue item from SQL");
            }
        }
    }

    /**
     * Call the QueueItemFinishedEvent.
     * @param player The player that finishes the item in the queue.
     * @param table The crafting table (profession) the player is using.
     * @param queue The crafting queue the player is using.
     * @param item The queue item that is finished.
     * @param resultItem The result item of the queue item.
     * @param resultAmount The amount of the result item.
     */
    public void finishQueueItem(Player player,
                                CraftingTable table,
                                CraftingQueue queue,
                                QueueItem item,
                                ItemStack resultItem,
                                int resultAmount) {
        QueueItemFinishedEvent event =
                new QueueItemFinishedEvent(table.getName(), player, queue, item, resultItem, resultAmount);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            if (event.getFusionPlayer().hasRecipeLimitReached(event.getQueueItem().getRecipe())) {
                cancelQueueItem(player,
                        table,
                        queue,
                        item,
                        false,
                        true,
                        event.getQueueItem().getRecipe().getItemsToTake());
                event.setCancelled(true);
                CodexEngine.get().getMessageUtil().sendMessage("fusion.error.recipeLimitReached", player,
                        new MessageData("recipe", event.getQueueItem().getRecipe().getName()),
                        new MessageData("amount",
                                event.getFusionPlayer().getRecipeLimit(event.getQueueItem().getRecipe()).getLimit()),
                        new MessageData("recipe.limit", event.getQueueItem().getRecipe().getCraftingLimit()),
                        new MessageData("limit", event.getQueueItem().getRecipe().getCraftingLimit()));
                return;
            }
            // Items if no commands exist
            if(item.getRecipe().getResults().getCommands().isEmpty()) {
                ItemStack result = event.getQueueItem().getRecipe().getDivinityRecipeMeta() == null ? event.getResultItem()
                        : event.getQueueItem().getRecipe().getDivinityRecipeMeta().generateItem();
                result.setAmount(event.getResultAmount());
                // If there is no space in the inventory, drop the items
                Collection<ItemStack> notAdded = player.getInventory().addItem(result).values();
                if (!notAdded.isEmpty()) {
                    for (ItemStack _item : notAdded) {
                        Objects.requireNonNull(player.getLocation().getWorld())
                                .dropItemNaturally(player.getLocation(), _item);
                    }
                }
            } else {
                // If there are commands, we need to delay the item giving
                DelayedCommand.invoke(Fusion.getInstance(), player, item.getRecipe().getResults().getCommands());
            }

            //Experience
            long professionExp = item.getRecipe().getResults().getProfessionExp() + (long) (item.getRecipe().getResults().getProfessionExp() * PlayerUtil.getProfessionExpBonusThroughPermissions(player, table.getName()));
            if (professionExp > 0) {
                FusionAPI.getEventServices()
                        .getProfessionService()
                        .giveProfessionExp(player,
                                event.getCraftingTable(),
                                professionExp);
            }
            if (item.getRecipe().getResults().getVanillaExp() > 0) {
                player.giveExp(event.getQueueItem().getRecipe().getResults().getVanillaExp());
            }

            // Increment the limit if existent
            if (event.getQueueItem().getRecipe().getCraftingLimit() > 0)
                event.getFusionPlayer().incrementLimit(event.getQueueItem().getRecipe());

            // Remove the item from the queue
            event.getQueue().removeRecipe(event.getQueueItem(), false);
        }
    }
}
