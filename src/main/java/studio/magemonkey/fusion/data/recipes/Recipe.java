package studio.magemonkey.fusion.data.recipes;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.util.DeserializationWorker;
import studio.magemonkey.codex.util.SerializationBuilder;
import studio.magemonkey.fusion.cfg.Cfg;
import studio.magemonkey.fusion.cfg.hooks.divinity.DivinityRecipeMeta;
import studio.magemonkey.fusion.data.player.FusionPlayer;
import studio.magemonkey.fusion.data.player.PlayerLoader;
import studio.magemonkey.fusion.data.professions.ProfessionConditions;
import studio.magemonkey.fusion.data.professions.ProfessionResults;
import studio.magemonkey.fusion.data.professions.ProfessionSettings;
import studio.magemonkey.fusion.util.Utils;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

@Getter
@EqualsAndHashCode
public class Recipe implements ConfigurationSerializable {
    private final CraftingTable table;

    @Setter
    private String name;

    @Setter
    private int    craftingTime;
    @Setter
    private int    craftingLimit;
    @Setter
    private int    craftingLimitCooldown = -1;
    @Setter
    private String category;

    private final ProfessionResults    results;
    private final ProfessionConditions conditions;
    private final ProfessionSettings   settings;

    // This is a optional meta information that is usually empty, until a recipe uses the ItemGenerator function from Divinity.
    // I might outsource this someday but for now it's urgent and requires this solution.
    @Setter
    private DivinityRecipeMeta divinityRecipeMeta;

    public Recipe(CraftingTable table, Map<String, Object> map) {
        this.table = table;
        DeserializationWorker dw = DeserializationWorker.start(map);
        this.name = dw.getString("name");
        this.category = dw.getString("category");

        this.craftingTime = dw.getInt("craftingTime");
        this.craftingLimit = dw.getInt("craftingLimit");
        this.craftingLimitCooldown = dw.getInt("craftingLimitCooldown");

        this.results = new ProfessionResults(name, dw);
        this.conditions = new ProfessionConditions(name, dw);
        this.settings = new ProfessionSettings(name, dw);
    }

    public Recipe(CraftingTable table, Map<String, Object> map, DivinityRecipeMeta meta) {
        this.table = table;
        DeserializationWorker dw = DeserializationWorker.start(map);
        this.name = dw.getString("name");
        this.category = dw.getString("category");

        this.craftingTime = dw.getInt("craftingTime");
        this.craftingLimit = dw.getInt("craftingLimit");
        this.craftingLimitCooldown = dw.getInt("craftingLimitCooldown");

        this.results = new ProfessionResults(name, dw);
        this.conditions = new ProfessionConditions(name, dw);
        this.settings = new ProfessionSettings(name, dw);

        if (meta != null) {
            this.divinityRecipeMeta = meta;
            this.results.setResultItem(RecipeItem.fromDivinityRecipeMeta(meta));
        }
    }

    public Recipe(CraftingTable table,
                  String name,
                  String category,
                  int craftingTime,
                  int craftingLimit,
                  int craftingLimitCooldown,
                  ProfessionResults results,
                  ProfessionConditions conditions,
                  ProfessionSettings settings,
                  DivinityRecipeMeta meta) {
        this.table = table;
        this.name = name;
        this.category = category;
        this.craftingTime = craftingTime;
        this.craftingLimit = craftingLimit;
        this.craftingLimitCooldown = craftingLimitCooldown;
        this.results = results;
        this.conditions = conditions;
        this.settings = settings;
        if (meta != null) {
            this.divinityRecipeMeta = meta;
            this.results.setResultItem(RecipeItem.fromDivinityRecipeMeta(meta));
        }
    }

    public static List<Pair<ItemStack, Integer>> getItems(Collection<ItemStack> items) {
        List<Pair<ItemStack, Integer>> eqItems = new ArrayList<>(20);
        for (ItemStack item : items) {
            // Get left pair and check for its item-meta
            ItemStack finalItem = item.clone();
            finalItem.setAmount(1);
            int amount = item.getAmount();
            Pair<ItemStack, Integer> pair =
                    eqItems.stream().filter(p -> p.getLeft().isSimilar(finalItem)).findFirst().orElse(null);
            if (pair != null) {
                eqItems.remove(pair);
                eqItems.add(Pair.of(pair.getLeft(), pair.getRight() + amount));
            } else {
                eqItems.add(Pair.of(finalItem, amount));
            }
        }
        return eqItems;
    }

    public static List<Pair<ItemStack, Integer>> getPattern(Collection<RecipeItem> items) {
        List<Pair<ItemStack, Integer>> localPattern = new ArrayList<>(20);
        for (RecipeItem recipeItem : items) {
            ItemStack item = recipeItem.getItemStack();
            item.setAmount(1);
            int amount = recipeItem.getAmount();
            Pair<ItemStack, Integer> pair =
                    localPattern.stream().filter(p -> p.getLeft().isSimilar(item)).findFirst().orElse(null);
            if (pair != null) {
                localPattern.remove(pair);
                localPattern.add(Pair.of(pair.getLeft(), pair.getRight() + amount));
            } else {
                localPattern.add(Pair.of(item, amount));
            }
        }
        return localPattern;
    }

    public boolean isValid(Collection<ItemStack> items, Player p, CraftingTable craftingTable) {
        if (items.isEmpty()) {
            return false;
        }
        if (p != null) {
            if (!Utils.hasCraftingPermission(p, getName())) {
                return false;
            }
            if (table.getLevelFunction().getLevel(p) < this.conditions.getProfessionLevel()) {
                return false;
            }
            if (!this.conditions.isValid(PlayerLoader.getPlayer(p))) {
                return false;
            }
        }
        List<Pair<ItemStack, Integer>> eqItems      = getItems(items);
        List<Pair<ItemStack, Integer>> localPattern = getPattern(this.conditions.getRequiredItems());
        for (Iterator<Pair<ItemStack, Integer>> iterator = localPattern.iterator(); iterator.hasNext(); ) {
            Pair<ItemStack, Integer> patternEntry = iterator.next();
            int eqAmount = eqItems.stream()
                    .filter(e -> e.getLeft().isSimilar(patternEntry.getKey()))
                    .mapToInt(Pair::getRight)
                    .findFirst()
                    .orElse(-1);
            if (eqAmount == -1) {
                return false;
            }
            ItemStack eqEntry       = patternEntry.getKey();
            int       patternAmount = patternEntry.getValue();
            if (eqAmount < patternAmount) {
                return false;
            }
            if (eqAmount == patternAmount) {
                eqItems.remove(patternEntry);
            }
            int rest = eqAmount - patternAmount;
            if (rest > 0) {
                eqItems.add(Pair.of(eqEntry, rest));
            }
            iterator.remove();
        }
        return localPattern.isEmpty();
    }

    public List<ItemStack> getItemsToTake() {
        return this.conditions.getRequiredItems().stream().map(RecipeItem::getItemStack).collect(Collectors.toList());
    }

    public double getMoneyCost() {
        return this.conditions.getMoneyCost();
    }

    public int getExpCost() {
        return this.conditions.getExpCost();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).appendSuper(super.toString())
                .append("name", this.name)
                .append("costs.money", this.conditions.getMoneyCost())
                .append("costs.experience", this.conditions.getExpCost())
                .append("costs.items", this.conditions.getRequiredItemNames())
                .append("results.item", this.getResults().getResultItem())
                .append("results.professionExp", this.getResults().getProfessionExp())
                .append("results.vanillaExp", this.getResults().getVanillaExp())
                .append("results.commands", this.getResults().getCommands())
                .append("craftingTime", this.craftingTime)
                .append("craftingLimit", this.craftingLimit)
                .append("craftingLimitCooldown", this.craftingLimitCooldown)
                .toString();
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        SerializationBuilder builder = SerializationBuilder.start(6)
                .append("name", this.name)
                .append("craftingTime", this.craftingTime)
                .append("craftingLimit", this.craftingLimit)
                .append("craftingLimitCooldown", this.craftingLimitCooldown);

        if (category != null) {
            builder.append("category", this.category);
        }

        for (Entry<String, Object> entry : this.results.serialize().entrySet()) {
            builder.append(entry.getKey(), entry.getValue());
        }
        for (Entry<String, Object> entry : this.conditions.serialize().entrySet()) {
            builder.append(entry.getKey(), entry.getValue());
        }
        for (Entry<String, Object> entry : this.settings.serialize().entrySet()) {
            builder.append(entry.getKey(), entry.getValue());
        }
        return builder.build();
    }

    public static Recipe copy(Recipe recipe) {
        return new Recipe(recipe.getTable(),
                recipe.getName(),
                recipe.getCategory(),
                recipe.getCraftingTime(),
                recipe.getCraftingLimit(),
                recipe.getCraftingLimitCooldown(),
                ProfessionResults.copy(recipe.getResults()),
                ProfessionConditions.copy(recipe.getConditions()),
                ProfessionSettings.copy(recipe.getSettings()),
                recipe.getDivinityRecipeMeta());
    }

    public boolean isHidden(Player player) {
        boolean isHidden = false;

        if (conditions.getPermission() != null) {
            if (Cfg.hideRecipesNoPermission && !player.hasPermission(conditions.getPermission())) {
                isHidden = true;
                if (settings.getHideNoPermission() != null) isHidden = settings.getHideNoPermission();
            } else if (!Utils.hasCraftingPermission(player, getName())) {
                if (settings.getHideNoPermission() != null) isHidden = settings.getHideNoPermission();
            }
            if (isHidden) return true;
        }

        FusionPlayer fusionPlayer = PlayerLoader.getPlayer(player);
        if (Cfg.hideRecipesLimitReached && fusionPlayer.hasRecipeLimitReached(this)) {
            isHidden = true;
            if (settings.getHideRecipeLimitReached() != null) isHidden = settings.getHideRecipeLimitReached();
        } else if (fusionPlayer.hasRecipeLimitReached(this)) {
            if (settings.getHideRecipeLimitReached() != null) isHidden = settings.getHideRecipeLimitReached();
        }
        return isHidden;
    }

    public String getRecipePath() {
        String recipeName = name;
        if (recipeName.contains("::")) {
            recipeName = recipeName.split("::")[0];
        }
        return table.getName() + "." + category + "." + recipeName;
    }

    public void appendDivinityRecipeMeta(DivinityRecipeMeta meta) {
        this.divinityRecipeMeta = meta;
        results.setResultItem(RecipeItem.fromDivinityRecipeMeta(meta));
    }
}
