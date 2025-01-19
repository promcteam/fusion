package studio.magemonkey.fusion.data.recipes;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.api.items.ItemType;
import studio.magemonkey.codex.legacy.item.ItemBuilder;
import studio.magemonkey.fusion.cfg.hooks.divinity.DivinityRecipeMeta;

import java.util.LinkedHashMap;
import java.util.Map;

@ToString
@EqualsAndHashCode
public class RecipeCustomItem implements RecipeItem {
    // Only ONE of these 3 should be set
    private final ItemType           item;
    private final ItemBuilder        builder;
    private final DivinityRecipeMeta meta;

    @Getter
    private final int     amount;
    private final boolean simple;

    public RecipeCustomItem(@NotNull ItemType item, int amount, boolean simple) {
        this.item = item;
        this.builder = null;
        this.meta = null;
        this.amount = amount;
        this.simple = simple;
    }

    public RecipeCustomItem(@NotNull ItemBuilder item, int amount, boolean simple) {
        this.item = null;
        this.builder = item;
        this.meta = null;
        this.amount = amount;
        this.simple = simple;
    }

    public RecipeCustomItem(@NotNull DivinityRecipeMeta meta) {
        this.item = null;
        this.builder = null;
        this.meta = meta;
        this.amount = meta.getAmount();
        this.simple = false;
    }

    @Override
    public ItemStack getItemStack() {
        ItemStack clone = null;
        if (this.item != null) {
            clone = this.item.create();
        } else if (this.builder != null) {
            clone = this.builder.build();
        } else {
            clone = this.meta.generateItem();
        }

        clone.setAmount(clone.getAmount() * this.amount);
        return clone;
    }

    @Override
    public Object toConfig() {
        ItemStack it = null;
        if (this.item != null) {
            it = this.item.create();
        } else if (this.builder != null) {
            it = this.builder.build();
        } else {
            it = this.meta.generateItem();
        }

        if (this.simple) {
            return CUSTOM_PREFIX + it.getType().name() + ":" + it.getDurability()
                    + (this.amount != 0 ? (":" + this.amount) : "");
        }

        ItemBuilder builder = ItemBuilder.newItem(it);
        if (it.getType() == Material.ENCHANTED_BOOK) {
            // Reapply enchants to the item, if it's an enchanted book
            if (it.getItemMeta() instanceof EnchantmentStorageMeta storage) {
                builder.clearEnchants();
                Map<Enchantment, Integer> enchants = new LinkedHashMap<>(storage.getStoredEnchants());
                builder.enchant(enchants);
            }
        }

        return builder.serialize();
    }
}
