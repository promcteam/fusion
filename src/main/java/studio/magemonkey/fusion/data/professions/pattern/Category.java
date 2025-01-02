package studio.magemonkey.fusion.data.professions.pattern;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.codex.api.items.ItemType;
import studio.magemonkey.codex.api.items.exception.CodexItemException;
import studio.magemonkey.codex.api.items.providers.VanillaProvider;
import studio.magemonkey.codex.util.DeserializationWorker;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.data.recipes.Recipe;
import studio.magemonkey.fusion.util.ChatUT;

import java.util.*;

public class Category implements ConfigurationSerializable {
    @Getter
    @Setter
    private String name, displayName = null;
    @Getter
    @Setter
    private       List<String>       displayLore = null;
    @Getter
    @Setter
    private       ItemType           iconItem;
    @Getter
    private final Collection<Recipe> recipes     = new ArrayList<>();
    @Setter
    @Getter
    private       InventoryPattern   pattern;
    @Getter
    @Setter
    private       int                order;
    private       boolean            hasPrevious = true;

    @Getter
    private final String iconName;

    public Category(String name) {
        this.name = name;
        this.order = 0;
        this.iconItem = new VanillaProvider.VanillaItemType(Material.PAPER);
        this.iconName = "PAPER";
    }

    public Category(String name, String iconName, InventoryPattern pattern, int order) {
        this.name = name;
        this.iconName = iconName;

        try {
            iconItem = CodexEngine.get().getItemManager().getItemType(iconName);
        } catch (CodexItemException e) {
            Fusion.getInstance().getLogger().severe("Invalid category icon for: " + name);
            Fusion.getInstance().getLogger().warning(e.getMessage());
            Fusion.getInstance().getLogger().warning("Using default icon instead.");
            iconItem = new VanillaProvider.VanillaItemType(Material.PAPER);
        }

        this.pattern = pattern;
        this.order = order;
    }

    @SuppressWarnings("unchecked")
    public Category(Map<String, Object> map) {
        DeserializationWorker dw = DeserializationWorker.start(map);
        name = dw.getString("name");
        order = dw.getInt("order");
        iconName = dw.getString("icon");

        try {
            iconItem = CodexEngine.get().getItemManager().getItemType(iconName);
        } catch (CodexItemException e) {
            Fusion.getInstance().getLogger().severe("Invalid category icon for: " + name);
            Fusion.getInstance().getLogger().warning(e.getMessage());
            Fusion.getInstance().getLogger().warning("Using default icon instead.");
            iconItem = new VanillaProvider.VanillaItemType(Material.PAPER);
        }

        Map<String, Object> displaySection = dw.getSection("display");
        if (displaySection != null) {
            displayName = (String) displaySection.getOrDefault("name", null);
            displayLore = (List<String>) displaySection.getOrDefault("lore", null);
        }

        pattern = dw.getSection("pattern") != null ? new InventoryPattern(dw.getSection("pattern")) : null;
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("order", order);
        map.put("icon", iconName);
        if (pattern != null)
            map.put("pattern", pattern.serialize());
        return map;
    }

    public void hasPrevious(boolean b) {
        this.hasPrevious = b;
    }

    public boolean hasPrevious() {
        return hasPrevious;
    }

    public Recipe getRecipe(String name) {
        for (Recipe recipe : recipes) {
            if (recipe.getName().equals(name)) {
                return recipe;
            }
        }
        return null;
    }

    public ItemStack getDisplayIcon() {
        ItemStack item = iconItem.create();
        ItemMeta  meta = item.getItemMeta();
        if (meta == null) return item;

        if (displayName != null) {
            String translated = ChatUT.hexString(displayName);
            meta.setDisplayName(translated);
            try {
                meta.setItemName(translated);
            } catch (NoSuchMethodError ignored) {
                // Older than 1.19
            }
        }

        if (displayLore != null) {
            List<String> translated = new ArrayList<>();
            for (String line : displayLore) {
                translated.add(ChatUT.hexString(line));
            }
            meta.setLore(translated);
        }

        item.setItemMeta(meta);
        return item;
    }

    public static Category copy(Category category) {
        return new Category(category.getName(),
                category.getIconName(),
                InventoryPattern.copy(category.getPattern()),
                category.getOrder());
    }
}
