package studio.magemonkey.fusion.data.recipes;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.CraftingRequirementsCfg;
import studio.magemonkey.fusion.data.player.PlayerLoader;
import studio.magemonkey.fusion.data.player.PlayerRecipeLimit;
import studio.magemonkey.fusion.util.ChatUT;
import studio.magemonkey.fusion.util.ExperienceManager;
import studio.magemonkey.fusion.util.InvalidPatternItemException;
import studio.magemonkey.fusion.util.Utils;

import java.util.*;

@Getter
public class CalculatedRecipe {
    private final Recipe recipe;

    private final ItemStack icon;
    private final boolean   canCraft;

    CalculatedRecipe(Recipe recipe, ItemStack icon, boolean canCraft) {
        this.recipe = recipe;
        this.icon = icon;
        this.canCraft = canCraft;
    }

    public static CalculatedRecipe create(Recipe recipe,
                                          Collection<ItemStack> items,
                                          Player player,
                                          CraftingTable craftingTable) throws InvalidPatternItemException {
        try {
            Fusion pl = Fusion.getInstance();

            StringBuilder lore = new StringBuilder(512);
            ItemStack result =
                    recipe.getDivinityRecipeMeta() == null ? recipe.getResults().getResultItem().getItemStack()
                            : recipe.getDivinityRecipeMeta().getIcon();
            List<String> resultLore = result.getItemMeta().getLore();

            if (!recipe.getSettings().isEnableLore()) {
                if ((resultLore != null) && !resultLore.isEmpty()) {
                    resultLore.forEach((str) -> lore.append(str).append('\n'));
                    lore.append('\n');
                }
            } else if (recipe.getSettings().getLore() != null && !recipe.getSettings().getLore().isEmpty()) {
                recipe.getSettings().getLore().forEach((str) -> lore.append(ChatUT.hexString(str)).append('\n'));
                lore.append('\n');
            }

            String requirementLine = CraftingRequirementsCfg.getCraftingRequirementLine("recipes");
            if (!requirementLine.isEmpty())
                lore.append(requirementLine).append('\n');

            boolean canCraft = true;


            String recipePermissionLine;
            if (!Utils.hasCraftingPermission(player, recipe.getName())) {
                canCraft = false;
            }
            recipePermissionLine = CraftingRequirementsCfg.getLearned("recipes",
                    Utils.hasCraftingPermission(player, recipe.getName()));

            String moneyLine = null;
            if (recipe.getConditions().getMoneyCost() != 0) {
                if (CodexEngine.get().getVault() == null || !CodexEngine.get()
                        .getVault()
                        .canPay(player, recipe.getConditions().getMoneyCost())) {
                    canCraft = false;
                }
                moneyLine = CraftingRequirementsCfg.getMoney("recipes",
                        CodexEngine.get().getVault() == null ? 0
                                : CodexEngine.get().getVault().getBalance(player),
                        recipe.getConditions().getMoneyCost());
            }

            String expLine = null;
            if (recipe.getConditions().getExpCost() != 0) {
                if (ExperienceManager.getTotalExperience(player) < recipe.getConditions().getExpCost()) {
                    canCraft = false;
                }
                expLine = CraftingRequirementsCfg.getExp("recipes",
                        ExperienceManager.getTotalExperience(player),
                        recipe.getConditions().getExpCost());
            }

            String levelsLine = null;
            if (recipe.getConditions().getProfessionLevel() != 0) {
                if (recipe.getTable().getLevelFunction().getLevel(player) < recipe.getConditions()
                        .getProfessionLevel()) {
                    canCraft = false;
                }
                levelsLine = CraftingRequirementsCfg.getProfessionLevel("recipes",
                        recipe.getTable().getLevelFunction().getLevel(player),
                        recipe.getConditions().getProfessionLevel());
            }

            String masteryLine = null;
            if (recipe.getConditions().isMastery()) {
                if (!PlayerLoader.getPlayer(player).hasMastered(craftingTable.getName())) {
                    canCraft = false;
                }
                masteryLine = CraftingRequirementsCfg.getMastery("recipes",
                        PlayerLoader.getPlayer(player).hasMastered(craftingTable.getName()),
                        recipe.getConditions().isMastery());
            }

            String limitLine = null;
            if (recipe.getCraftingLimit() > 0) {
                PlayerRecipeLimit limit = PlayerLoader.getPlayer(player).getRecipeLimit(recipe);
                if (limit.getLimit() > 0) {
                    if (limit.getCooldownTimestamp() > 0 && !limit.hasCooldown()) {
                        limit.resetLimit();
                        Bukkit.getConsoleSender()
                                .sendMessage(
                                        "§aResetting limit for " + player.getName() + " on " + recipe.getRecipePath());
                    }
                }
                if (limit.getLimit() >= recipe.getCraftingLimit()) {
                    canCraft = false;
                }
                limitLine = CraftingRequirementsCfg.getLimit("recipes", limit.getLimit(), recipe.getCraftingLimit());
            }

            List<Map.Entry<Boolean, String>> conditionLines = recipe.getConditions().getConditionLines(player);
            for (Map.Entry<Boolean, String> entry : conditionLines) {
                if (!entry.getKey()) {
                    canCraft = false;
                    break;
                }
            }

            List<Pair<ItemStack, Integer>> eqItems = Recipe.getItems(items);


            Collection<RecipeItem> localPattern       = new LinkedHashSet<>(recipe.getConditions().getRequiredItems());
            boolean                isExtensionEnabled = CraftingRequirementsCfg.hasExtensionEnabled("recipes");
            boolean                isVanillaOnly      = CraftingRequirementsCfg.hasOnlyVanillaExtension("recipes");
            for (Iterator<RecipeItem> it = localPattern.iterator(); it.hasNext(); ) {
                RecipeItem recipeItem         = it.next();
                ItemStack  recipeItemStack    = recipeItem.getItemStack();
                ItemStack  recipeItemStackOne = recipeItemStack.clone();
                recipeItemStackOne.setAmount(1);
                Pair<ItemStack, Integer> eqEntry        = null;
                List<String>             extensionLines = new ArrayList<>();
                for (Pair<ItemStack, Integer> entry : eqItems) {
                    ItemStack item = entry.getKey().clone();
                    if (CalculatedRecipe.isSimilar("recipes", recipeItemStackOne, item, extensionLines)) {
                        eqEntry = entry;
                        break;
                    }
                }

                int eqAmount      = eqEntry != null ? eqEntry.getValue() : 0;
                int patternAmount = recipeItem.getAmount();
                if (eqAmount < patternAmount) {
                    canCraft = false;
                    lore.append(CraftingRequirementsCfg.getIngredientLine("recipes",
                            recipeItem,
                            eqAmount,
                            patternAmount)).append('\n');
                    if (isExtensionEnabled) {
                        if ((isVanillaOnly && !(recipeItem instanceof RecipeEconomyItem)) || (!isVanillaOnly
                                && (recipeItem instanceof RecipeEconomyItem))) {
                            for (String extension : extensionLines) {
                                lore.append(extension).append('\n');
                            }
                        }
                    }
                    continue;
                }
                if (eqAmount == patternAmount) {
                    eqItems.remove(eqEntry);
                }
                int rest = eqAmount - patternAmount;
                if (rest > 0 && eqEntry != null) {
                    eqItems.add(Pair.of(eqEntry.getKey(), rest));
                }
                it.remove();
                lore.append(CraftingRequirementsCfg.getIngredientLine("recipes", recipeItem, eqAmount, patternAmount))
                        .append('\n');
                if (isExtensionEnabled) {
                    if ((isVanillaOnly && !(recipeItem instanceof RecipeEconomyItem)) || (!isVanillaOnly
                            && (recipeItem instanceof RecipeEconomyItem))) {
                        for (String extension : extensionLines) {
                            lore.append(extension).append('\n');
                        }
                    }
                }
            }

            String canCraftLine = CraftingRequirementsCfg.getCanCraft(canCraft);

            lore.append('\n');
            if (moneyLine != null) {
                lore.append(moneyLine).append('\n');
            }
            if (levelsLine != null) {
                lore.append(levelsLine).append('\n');
            }
            if (expLine != null) {
                lore.append(expLine).append('\n');
            }
            if (masteryLine != null) {
                lore.append(masteryLine).append('\n');
            }
            if (limitLine != null) {
                lore.append(limitLine).append('\n');
            }

            if (!conditionLines.isEmpty()) {
                for (Map.Entry<Boolean, String> entry : conditionLines) {
                    lore.append('\n').append(entry.getValue());
                }
            }

            if (recipePermissionLine != null) {
                lore.append('\n').append(recipePermissionLine);
            }

            lore.append('\n').append(canCraftLine);

            ItemStack icon     = result.clone();
            ItemMeta  itemMeta = icon.getItemMeta();
            itemMeta.setLore(Arrays.asList(StringUtils.split(lore.toString(), '\n')));
            icon.setItemMeta(itemMeta);

            return new CalculatedRecipe(recipe, icon, canCraft);
        } catch (Exception e) {
            Fusion.getInstance()
                    .error("The recipe-item seems not to be recognized. Please check your setup on the following recipe '"
                            + recipe.getName());
            Fusion.getInstance().error("Result: " + recipe.getResults().getResultName());
            Fusion.getInstance().error("Pattern Items: ");
            for (Object patternItem : recipe.getConditions().getRequiredItemNames()) {
                Fusion.getInstance().error("- " + patternItem);
            }
            Fusion.getInstance().error("Error on creating CalculatedRecipe: " + e.getMessage());
            e.printStackTrace();
            throw new InvalidPatternItemException(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof CalculatedRecipe that)) {
            return false;
        }

        return new EqualsBuilder().append(this.recipe, that.recipe).append(this.icon, that.icon).isEquals();
    }

    public static boolean isSimilar(ItemStack is1, ItemStack is2) {
        return isSimilar("recipes", is1, is2);
    }

    public static boolean isSimilar(String path, ItemStack is1, ItemStack is2) {
        return isSimilar(path, is1, is2, new ArrayList<>());
    }

    public static boolean isSimilar(String path, ItemStack is1, ItemStack is2, List<String> checkingLines) {
        //More relaxed comparison
        if (is1.getType() != is2.getType())
            return false;

        ItemMeta im1 = is1.getItemMeta();
        ItemMeta im2 = is2.getItemMeta();
        if ((im1 == null && im2 != null) || (im1 != null && im2 == null)) return false;
        if (im1 == null) return true;

        // Check for name
        if (im1.hasDisplayName()) {
            String displayName1 = im1.getDisplayName().trim();
            String displayName2 = im2.hasDisplayName() ? im2.getDisplayName().trim() : "";
            if (!displayName1.equals(displayName2))
                return false;
        } else if (!im1.hasDisplayName() && im2.hasDisplayName()) {
            return false;
        }

        boolean isValid = true;
        // Check for lore
        if (im1.hasLore()) {
            List<String> lore1 = im1.getLore();
            List<String> lore2 = im2.hasLore() ? im2.getLore() : new ArrayList<>();
            if (lore1 == null || lore2 == null) {
                isValid = false;
            } else {
                if (lore1.isEmpty() || (lore1.size() != lore2.size())) {
                    isValid = false;
                } else {
                    for (int i = 0; i < lore1.size(); i++) {
                        if (!lore1.get(i).equals(lore2.get(i))) {
                            isValid = false;
                            break;
                        }
                    }
                }
            }
            checkingLines.addAll(CraftingRequirementsCfg.getExtensionLoreLine(path, lore2, lore1));
        }

        // Check for enchantments
        if (im1 instanceof EnchantmentStorageMeta storage1) {
            EnchantmentStorageMeta    storage2 = (EnchantmentStorageMeta) im2;
            Map<Enchantment, Integer> ench1    = storage1.getStoredEnchants();
            Map<Enchantment, Integer> ench2    = storage2.getStoredEnchants();

            if (ench1.size() != ench2.size())
                isValid = false;
            for (Map.Entry<Enchantment, Integer> entry : ench1.entrySet()) {
                if (!ench2.containsKey(entry.getKey()) || !ench2.get(entry.getKey()).equals(entry.getValue()))
                    isValid = false;
            }
            checkingLines.addAll(CraftingRequirementsCfg.getExtensionEnchantmentLine(path, ench2, ench1));
        } else {
            if (im1.hasEnchants()) {
                Map<Enchantment, Integer> ench1 = im1.getEnchants();
                Map<Enchantment, Integer> ench2 = im2.getEnchants();
                if (ench1.size() != ench2.size())
                    isValid = false;
                for (Map.Entry<Enchantment, Integer> entry : ench1.entrySet()) {
                    if (!ench2.containsKey(entry.getKey()) || !ench2.get(entry.getKey()).equals(entry.getValue()))
                        isValid = false;
                }
                checkingLines.addAll(CraftingRequirementsCfg.getExtensionEnchantmentLine(path, ench2, ench1));
            }
        }
        // Check for flags
        if (!im1.getItemFlags().isEmpty()) {
            if (im1.getItemFlags().size() != im2.getItemFlags().size())
                isValid = false;
            for (ItemFlag flag : im1.getItemFlags()) {
                if (!im2.getItemFlags().contains(flag))
                    isValid = false;
            }
            checkingLines.addAll(CraftingRequirementsCfg.getExtensionFlagLine(path,
                    im2.getItemFlags(),
                    im1.getItemFlags()));
        }

        // Check for custom model data
        if (im1.hasCustomModelData()) {
            if (im1.getCustomModelData() != im2.getCustomModelData())
                isValid = false;
            checkingLines.add(CraftingRequirementsCfg.getExtensionCustomModelDataLine(path,
                    im2.getCustomModelData(),
                    im1.getCustomModelData()));
        }
        // Check if unbreakable
        if (im1.isUnbreakable()) {
            if (im2.isUnbreakable())
                isValid = false;
            checkingLines.add(CraftingRequirementsCfg.getExtensionUnbreakableLine(path,
                    im2.isUnbreakable(),
                    im1.isUnbreakable()));
        }
        // Check for durability if instanceof Damageable
        if (im1 instanceof Damageable dmg && dmg.getDamage() > 0) {
            int damage1 = dmg.getDamage();
            int damage2 = im2 instanceof Damageable dmg2 ? dmg2.getDamage() : 0;
            if (damage1 != damage2)
                isValid = false;
            checkingLines.add(CraftingRequirementsCfg.getExtensionDurabilityLine(path, damage2, damage1));
        }

        // If all those checks failed, try to check once more through the native item meta check
        // This is useful for custom items like from Divinity, etc.
        return isValid || is1.isSimilar(is2);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(this.recipe).append(this.icon).toHashCode();
    }
}
