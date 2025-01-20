package studio.magemonkey.fusion.data.professions;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.CraftingRequirementsCfg;
import studio.magemonkey.fusion.data.recipes.*;
import studio.magemonkey.fusion.util.ExperienceManager;
import studio.magemonkey.fusion.util.InvalidPatternItemException;

import java.util.*;

@Getter
public class CalculatedProfession {

    private final ProfessionConditions conditions;
    private final ItemStack            icon;

    private final boolean canJoin;

    public CalculatedProfession(ProfessionConditions conditions, ItemStack icon, boolean canJoin) {
        this.conditions = conditions;
        this.icon = icon;
        this.canJoin = canJoin;
    }

    public static CalculatedProfession create(ProfessionConditions conditions,
                                              Collection<ItemStack> items,
                                              Player player,
                                              CraftingTable craftingTable) throws InvalidPatternItemException {
        try {
            StringBuilder lore        = new StringBuilder(512);
            ItemStack     defaultIcon = craftingTable.getIconItem().create();
            List<String>  resultLore  = defaultIcon.getItemMeta().getLore();

            if ((resultLore != null) && !resultLore.isEmpty()) {
                resultLore.forEach((str) -> lore.append(str).append('\n'));
                lore.append('\n');
            }

            String requirementLine = CraftingRequirementsCfg.getCraftingRequirementLine("professions");
            if (!requirementLine.isEmpty())
                lore.append(requirementLine).append('\n');

            boolean canJoin = true;

            //Rank line
            String rankLine = null;
            if (conditions.getPermission() != null && !player.hasPermission(
                    "fusion.rank." + conditions.getPermission())) {
                canJoin = false;
                rankLine = CraftingRequirementsCfg.getRank("recipes", conditions.getPermission());
            }

            // TODO: Might consider to make professions controllable by permissions
            String permissionLine;
            permissionLine = CraftingRequirementsCfg.getLearned("professions", true);


            String moneyLine = null;
            if (conditions.getMoneyCost() != 0) {
                if (!CodexEngine.get().getVault().canPay(player, conditions.getMoneyCost())) {
                    canJoin = false;
                }
                moneyLine = CraftingRequirementsCfg.getMoney("recipes",
                        CodexEngine.get().getVault().getBalance(player),
                        conditions.getMoneyCost());
            }

            String expLine = null;
            if (conditions.getExpCost() != 0) {
                if (ExperienceManager.getTotalExperience(player) < conditions.getExpCost()) {
                    canJoin = false;
                }
                expLine = CraftingRequirementsCfg.getExp("recipes",
                        ExperienceManager.getTotalExperience(player),
                        conditions.getExpCost());
            }

            List<Map.Entry<Boolean, String>> conditionLines = conditions.getConditionLines(player);
            for (Map.Entry<Boolean, String> entry : conditionLines) {
                if (!entry.getKey()) {
                    canJoin = false;
                    break;
                }
            }

            List<Pair<ItemStack, Integer>> eqItems = Recipe.getItems(items);

            Collection<RecipeItem> localPattern       = new HashSet<>(conditions.getRequiredItems());
            boolean                isExtensionEnabled = CraftingRequirementsCfg.hasExtensionEnabled("professions");
            boolean                isVanillaOnly      = CraftingRequirementsCfg.hasOnlyVanillaExtension("professions");
            for (Iterator<RecipeItem> it = localPattern.iterator(); it.hasNext(); ) {
                RecipeItem recipeItem         = it.next();
                ItemStack  recipeItemStack    = recipeItem.getItemStack();
                ItemStack  recipeItemStackOne = recipeItemStack.clone();
                recipeItemStackOne.setAmount(1);
                Pair<ItemStack, Integer> eqEntry        = null;
                List<String>             extensionLines = new ArrayList<>();
                for (Pair<ItemStack, Integer> entry : eqItems) {
                    ItemStack item = entry.getKey().clone();
                    if (CalculatedRecipe.isSimilar("professions", recipeItemStackOne, item, extensionLines)) {
                        eqEntry = entry;
                        break;
                    }
                }

                int eqAmount      = eqEntry != null ? eqEntry.getValue() : 0;
                int patternAmount = recipeItem.getAmount();
                if (eqAmount < patternAmount) {
                    canJoin = false;
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

            String canJoinLine = CraftingRequirementsCfg.getCanJoin(canJoin);

            lore.append('\n');
            if (moneyLine != null) {
                lore.append(moneyLine).append('\n');
            }
            if (expLine != null) {
                lore.append(expLine).append('\n');
            }

            if (!conditionLines.isEmpty()) {
                for (Map.Entry<Boolean, String> entry : conditionLines) {
                    lore.append('\n').append(entry.getValue());
                }
            }

            lore.append("\n").append(permissionLine);

            if (rankLine != null) {
                lore.append('\n').append(rankLine);
            }

            lore.append('\n').append(canJoinLine);

            ItemStack icon     = defaultIcon.clone();
            ItemMeta  itemMeta = icon.getItemMeta();
            itemMeta.setLore(Arrays.asList(StringUtils.split(lore.toString(), '\n')));
            icon.setItemMeta(itemMeta);

            return new CalculatedProfession(conditions, icon, canJoin);
        } catch (Exception e) {
            Fusion.getInstance()
                    .error("The profession-item seems not to be recognized. Please check your setup on the following profession '"
                            + craftingTable.getName() + "':");
            Fusion.getInstance().error("Pattern Items: ");
            for (Object patternItem : conditions.getRequiredItemNames()) {
                Fusion.getInstance().error("- " + patternItem);
            }
            Fusion.getInstance().error("Error on creating CalculatedProfession: " + e.getMessage());
            throw new InvalidPatternItemException(e);
        }
    }
}
