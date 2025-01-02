package studio.magemonkey.fusion.gui;

import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.data.recipes.CalculatedRecipe;
import studio.magemonkey.fusion.data.recipes.CraftingTable;
import studio.magemonkey.fusion.data.recipes.Recipe;
import studio.magemonkey.fusion.data.recipes.RecipeItem;
import studio.magemonkey.fusion.util.ChatUT;

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

        Bukkit.getConsoleSender().sendMessage("Opening book for " + player.getName());
        Fusion.getPlayerAudience(player).openBook(book);
        Fusion.getPlayerAudience(player).sendMessage(Component.text("Opened book"));

        // Create a written book
        ItemStack _book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();

        if (meta != null) {
            // Title and author
            meta.setTitle("Interactive Book");
            meta.setAuthor("Adventure");

            // Create a page with a clickable and hoverable text
            Component page = Component.text("Click here!")
                    .clickEvent(ClickEvent.runCommand("/say Hello, world!"))
                    .hoverEvent(HoverEvent.showText(Component.text("Say something in chat!")));

            // Convert Adventure Component to JSON and add it as a page
            meta.spigot().addPage(Component.toJson(page));

            // Set the meta back to the book
            book.setItemMeta(meta);
    }
}
