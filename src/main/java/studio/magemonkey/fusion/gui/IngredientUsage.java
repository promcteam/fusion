package studio.magemonkey.fusion.gui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.data.recipes.CalculatedRecipe;
import studio.magemonkey.fusion.data.recipes.CraftingTable;
import studio.magemonkey.fusion.data.recipes.Recipe;
import studio.magemonkey.fusion.data.recipes.RecipeItem;
import studio.magemonkey.fusion.gui.show.ShowRecipesGui;

import java.util.HashMap;
import java.util.Map;

public class IngredientUsage {


    public static void showIngredientUsage(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            return;
        }

        Map<Recipe, RecipeItem> recipeUsage = new HashMap<>();
        for (CraftingTable table : ProfessionsCfg.getMap().values()) {
            for (Recipe recipe : table.getRecipes().values()) {
                for (RecipeItem ingredient : recipe.getConditions().getRequiredItems()) {
                    if (CalculatedRecipe.isSimilar(ingredient.getItemStack(), item)) {
                        recipeUsage.put(recipe, ingredient);
                    }
                }
            }
        }

        new ShowRecipesGui(player, recipeUsage).open(player);
    }
}
