package studio.magemonkey.fusion.data.recipes;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.codex.api.items.ItemType;
import studio.magemonkey.codex.api.items.exception.MissingItemException;
import studio.magemonkey.codex.api.items.exception.MissingProviderException;
import studio.magemonkey.codex.items.CodexItemManager;
import studio.magemonkey.codex.legacy.item.ItemBuilder;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.hooks.divinity.DivinityRecipeMeta;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface RecipeItem {
    String  CUSTOM_PREFIX   = "@ ";
    Pattern divinityPattern =
            Pattern.compile("DIVINITY_([\\w-]+:)?([\\w-]+)\\b((~level:(\\d+))|(~material:([\\w-]+)\\b)){0,2}");

    int getAmount();

    ItemStack getItemStack();

    Object toConfig();

    static RecipeItem fromConfig(Object obj) {
        if (obj instanceof Map) {
            //noinspection unchecked
            return getItemBuilder((Map<String, Object>) obj);
        }

        int        amount = 1;
        RecipeItem result = null;

        if (obj instanceof String itemString) {
            CodexItemManager items = CodexEngine.get().getItemManager();
            try {
                Matcher divMatcher = divinityPattern.matcher(itemString);
                String  itemKey;
                if (divMatcher.find()) {
                    itemKey = divMatcher.group(0);
                } else {
                    itemKey = itemString.split(":")[0];
                }
                ItemType type = items.getItemType(itemKey);
                if (type != null) {
                    // The amount *should* be the last part of the string, after the colon.
                    // We'll replace the Divinity format with a blank string to get rid of possible
                    // ~level:X conflicts so we can be sure that the amount is the last part.
                    itemString = itemString.replace(itemKey, "");
                    String[] amountSplit = StringUtils.split(itemString, ':');
                    if (amountSplit.length > 0) {
                        try {
                            amount = Integer.parseInt(amountSplit[amountSplit.length - 1]);
                        } catch (NumberFormatException e) {
                            Fusion.getInstance()
                                    .getLogger()
                                    .warning("Invalid amount found in configuration: "
                                            + amountSplit[amountSplit.length - 1] + " for item " + itemKey);
                        }
                    }

                    result = new RecipeCustomItem(type.create(), amount, false);
                }
            } catch (MissingProviderException | MissingItemException e) {
                throw new RuntimeException(e);
            }

            if (result == null) {
                result = buildInternalItem(obj);
            }
        }

        return result;
    }

    private static @Nullable RecipeItem buildInternalItem(Object obj) {
        RecipeItem result = null;
        String     str    = obj.toString();
        boolean    custom = str.startsWith(CUSTOM_PREFIX);
        if (custom) {
            str = str.substring(CUSTOM_PREFIX.length());
        }

        String[] srrs = StringUtils.split(str, ':');
        if (srrs.length == 0) {
            return null;
        }

        if (custom) {
            Material mat = Material.matchMaterial(srrs[0]);
            if (mat == null) {
                Fusion.getInstance().getLogger().warning("Invalid material found in configuration: " + srrs[0]);
                return null;
            }

            ItemBuilder itemBuilder = ItemBuilder.newItem(mat);

            if (srrs.length > 1) {
                try {
                    itemBuilder.durability(Integer.parseInt(srrs[1]));
                } catch (NumberFormatException e) {
                    Fusion.getInstance().error("Durability is invalid (" + srrs[1] + ") for material " + mat.name());
                }
            }

            if (srrs.length > 2) {
                try {
                    itemBuilder.amount(Integer.parseInt(srrs[2]));
                } catch (NumberFormatException e) {
                    Fusion.getInstance().error("Amount is invalid (" + srrs[2] + ") for material " + mat.name());
                }
            }

            result = new RecipeCustomItem(itemBuilder.build(), 1, true);
        } else if (srrs.length == 2) {
            try {
                RecipeEconomyItem recipeEconomyItem = new RecipeEconomyItem(srrs[0], Integer.parseInt(srrs[1]));
                ItemType          itemType          = recipeEconomyItem.asItemType();
                if (itemType != null) {
                    result = recipeEconomyItem;
                }
            } catch (Exception e) {
                Fusion.getInstance()
                        .error("Error on loading configuration of RecipeItem: " + obj + ", exception: " + e.getMessage()
                                + ", more in console.");
                throw new RuntimeException(e);
            }
        }

        return result;
    }

    private static @NotNull RecipeItem getItemBuilder(Map<String, Object> obj) {
        ItemBuilder itemBuilder = new ItemBuilder(obj);
        ItemStack   item;

        // In case we face an enchantment book, properly handle the enchantments
        // in a seperated meta before handling the item later on
        if (itemBuilder.getMaterial() == Material.ENCHANTED_BOOK) {
            Map<Enchantment, Integer> enchantments = new LinkedHashMap<>(itemBuilder.getEnchants());
            itemBuilder.clearEnchants();
            item = itemBuilder.build().clone();
            EnchantmentStorageMeta storage = (EnchantmentStorageMeta) item.getItemMeta();
            if (storage != null) {
                for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                    storage.addStoredEnchant(entry.getKey(), entry.getValue(), true);
                }
                item.setItemMeta(storage);
            }
            return new RecipeCustomItem(item, itemBuilder.getAmount(), false);
        }

        return new RecipeCustomItem(itemBuilder.build(), itemBuilder.getAmount(), false);
    }

    static RecipeItem fromDivinityRecipeMeta(DivinityRecipeMeta meta) {
        return new RecipeCustomItem(meta.generateItem(), meta.getAmount(), false);
    }
}
