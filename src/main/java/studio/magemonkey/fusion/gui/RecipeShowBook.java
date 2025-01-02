package studio.magemonkey.fusion.gui;

import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.data.recipes.CalculatedRecipe;
import studio.magemonkey.fusion.data.recipes.CraftingTable;
import studio.magemonkey.fusion.data.recipes.Recipe;
import studio.magemonkey.fusion.data.recipes.RecipeItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecipeShowBook {


    public static void showIngredientUsage(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if(item.getType().isAir()) {
            return;
        }

        Book.Builder builder = Book.builder();
        builder.title(Component.text("Ingredient Search"));
        builder.author(Component.text("Server"));
        List<Component> pages = new ArrayList<>();

        Map<String, List<Recipe>> existentRecipes = new HashMap<>();
        for(CraftingTable table : ProfessionsCfg.getMap().values()) {
            for(Recipe recipe : table.getRecipes().values()) {
                for(RecipeItem ingredient : recipe.getConditions().getRequiredItems()) {
                    if(CalculatedRecipe.isSimilar(ingredient.getItemStack(), item)) {
                        if(existentRecipes.containsKey(table.getName())) {
                            existentRecipes.get(table.getName()).add(recipe);
                        } else {
                            List<Recipe> recipes = new ArrayList<>();
                            recipes.add(recipe);
                            existentRecipes.put(table.getName(), recipes);
                        }
                    }
                }
            }
        }

        for(Map.Entry<String, List<Recipe>> entry : existentRecipes.entrySet()) {
            Component page = Component.text("Recipes for " + entry.getKey());
            for(Recipe recipe : entry.getValue()) {
                page = page.append(Component.text("\n\n" + recipe.getName()));
                for(RecipeItem ingredient : recipe.getConditions().getRequiredItems()) {
                    if(CalculatedRecipe.isSimilar(ingredient.getItemStack(), item)) {
                        page = page.append(Component.text("\n" + ingredient.getItemStack().getAmount() + "x " + ingredient.getItemStack().getItemMeta().getDisplayName()));
                    }
                }
            }
            builder.addPage(page);
        }

        Book book = builder.build();

        Fusion.getPlayerAudience(player).openBook(book);
        Fusion.getPlayerAudience(player).sendMessage(Component.text("Opened book"));
    }
}
