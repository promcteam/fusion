package studio.magemonkey.fusion.cfg.editors.pattern;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import studio.magemonkey.codex.api.DelayedCommand;
import studio.magemonkey.codex.legacy.item.ItemBuilder;
import studio.magemonkey.codex.util.messages.MessageUtil;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.YamlParser;
import studio.magemonkey.fusion.data.recipes.CraftingTable;
import studio.magemonkey.fusion.gui.editors.browse.BrowseEditor;
import studio.magemonkey.fusion.util.ChatUT;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PatternItemEditorCfg {

    private final YamlParser config;

    public PatternItemEditorCfg() {
        this.config = YamlParser.loadOrExtract(Fusion.getInstance(), "Editors/pattern/PatternItemEditor.yml");
    }

    public String getTitle() {
        return ChatUT.hexString(config.getString("title", "&2Pattern Item Editor"));
    }

    public Map<String, ItemStack> getIcons(CraftingTable table) {
        Map<String, ItemStack> icons = new HashMap<>();
        for (String icon : config.getConfigurationSection("icons").getKeys(false)) {
            if (icon.equalsIgnoreCase("patternItem")) continue;
            icons.put(icon, getIcon(table, icon));
        }
        return icons;
    }

    public Map<String, ItemStack> getIcons(BrowseEditor browseEditor) {
        Map<String, ItemStack> icons = new HashMap<>();
        for (String icon : config.getConfigurationSection("icons").getKeys(false)) {
            if (icon.equalsIgnoreCase("patternItem")) continue;
            icons.put(icon, getIcon(browseEditor, icon));
        }
        return icons;
    }

    public ItemStack getIcon(CraftingTable table, String icon) {
        Material material =
                Material.valueOf(config.getString("icons." + icon + ".material", "STONE").toUpperCase());
        int          amount      = config.getInt("icons." + icon + ".amount", 1);
        int          durability  = config.getInt("icons." + icon + ".durability", 0);
        boolean      unbreakable = config.getBoolean("icon." + icon + ".unbreakable", false);
        String       name        = config.getString("icons." + icon + ".name", "&cInvalid Item: &4" + icon);
        List<String> lore        = config.getStringList("icons." + icon + ".lore");
        for (int i = 0; i < lore.size(); i++) {
            lore.set(i, ChatUT.hexString(lore.get(i)
                    .replace(MessageUtil.getReplacement("name"), table.getInventoryName())
                    .replace(MessageUtil.getReplacement("profession"), table.getName())
                    .replace(MessageUtil.getReplacement("masteryUnlock"),
                            String.valueOf(table.getMasteryUnlock()))
                    .replace(MessageUtil.getReplacement("masteryCost"),
                            String.valueOf(table.getMasteryFee()))
                    .replace(MessageUtil.getReplacement("useCategories"),
                            String.valueOf(table.getUseCategories()))));
        }
        Map<Enchantment, Integer> enchants  = config.getEnchantmentSection("icons." + icon + ".enchants");
        List<ItemFlag>            flags     = config.getItemFlags("icons." + icon + ".flags");
        ItemFlag[]                itemFlags = flags.toArray(new ItemFlag[0]);
        return ItemBuilder.newItem(material)
                .amount(amount)
                .durability(durability)
                .unbreakable(unbreakable)
                .name(ChatUT.hexString(name))
                .lore(lore)
                .enchant(enchants)
                .flag(itemFlags)
                .build();
    }

    public ItemStack getIcon(BrowseEditor browseEditor, String icon) {
        Material material =
                Material.valueOf(config.getString("icons." + icon + ".material", "STONE").toUpperCase());
        int          amount      = config.getInt("icons." + icon + ".amount", 1);
        int          durability  = config.getInt("icons." + icon + ".durability", 0);
        boolean      unbreakable = config.getBoolean("icon." + icon + ".unbreakable", false);
        String       name        = config.getString("icons." + icon + ".name", "&cInvalid Item: &4" + icon);
        List<String> lore        = config.getStringList("icons." + icon + ".lore");
        for (int i = 0; i < lore.size(); i++) {
            lore.set(i, ChatUT.hexString(lore.get(i)
                    .replace(MessageUtil.getReplacement("name"), browseEditor.getName())));
        }
        Map<Enchantment, Integer> enchants  = config.getEnchantmentSection("icons." + icon + ".enchants");
        List<ItemFlag>            flags     = config.getItemFlags("icons." + icon + ".flags");
        ItemFlag[]                itemFlags = flags.toArray(new ItemFlag[0]);
        return ItemBuilder.newItem(material)
                .amount(amount)
                .durability(durability)
                .unbreakable(unbreakable)
                .name(ChatUT.hexString(name))
                .lore(lore)
                .enchant(enchants)
                .flag(itemFlags)
                .build();
    }

    public ItemStack getPatternItemIcon(char c, ItemStack item) {
        String itemName = ItemBuilder.newItem(item).getName();
        Material material = Material.valueOf(config.getString("icons.patternItem.material", "STONE")
                .replace(MessageUtil.getReplacement("material"),
                        item.getType().name().toUpperCase())
                .toUpperCase());
        int     amount      = config.getInt("icons.patternItem.amount", 1);
        int     durability  = config.getInt("icons.patternItem.durability", 0);
        boolean unbreakable = config.getBoolean("icon.patternItem.unbreakable", false);
        String name = config.getString("icons.patternItem.name", "$<id>")
                .replace(MessageUtil.getReplacement("id"), String.valueOf(c))
                .replace(MessageUtil.getReplacement("pattern.name"), itemName);
        List<String> lore = config.getStringList("icons.patternItem.lore");

        for (int i = 0; i < lore.size(); i++) {
            if (lore.get(i).contains(MessageUtil.getReplacement("pattern.lore"))) {
                lore.remove(i);
                if (item.getItemMeta() == null || item.getItemMeta().getLore() == null) continue;
                int newLines = 1;
                for (String line : item.getItemMeta().getLore()) {
                    lore.add(i - 1 + newLines, ChatUT.hexString(line));
                    newLines++;
                }
                i += newLines;
                continue;
            }
            lore.set(i, ChatUT.hexString(lore.get(i)
                    .replace(MessageUtil.getReplacement("pattern.name"), itemName)
                    .replace(MessageUtil.getReplacement("pattern.id"), String.valueOf(c))));
        }
        Map<Enchantment, Integer> enchants  = config.getEnchantmentSection("icons.patternItem.enchants");
        List<ItemFlag>            flags     = config.getItemFlags("icons.patternItem.flags");
        ItemFlag[]                itemFlags = flags.toArray(new ItemFlag[0]);
        return ItemBuilder.newItem(material)
                .amount(amount)
                .durability(durability)
                .unbreakable(unbreakable)
                .name(name)
                .lore(lore)
                .enchant(enchants)
                .flag(itemFlags)
                .build();
    }

    public String getSubTitle(char c) {
        return ChatUT.hexString(config.getString("subEditor.title", "&2Pattern Item &a$<id>")
                .replace(MessageUtil.getReplacement("id"), String.valueOf(c)));
    }

    public Map<String, ItemStack> getSubIcons(char c, ItemBuilder builder, Collection<DelayedCommand> commands) {
        Map<String, ItemStack> icons = new HashMap<>();
        for (String icon : config.getConfigurationSection("subEditor.icons").getKeys(false)) {
            icons.put(icon, getSubIcon(c, builder, commands, icon));
        }
        return icons;
    }

    public ItemStack getSubIcon(char c, ItemBuilder builder, Collection<DelayedCommand> commands, String icon) {
        Material material = Material.valueOf(config.getString("subEditor.icons." + icon + ".material", "STONE")
                .replace(MessageUtil.getReplacement("material"), builder.getMaterial().getID())
                .toUpperCase());
        int amount = config.getInt("subEditor.icons." + icon + ".amount", 1);
        int durability = config.isString("subEditor.icons." + icon + ".durability") ?
                Integer.parseInt(config.getString("subEditor.icons." + icon + ".durability", "$<durability>")
                        .replace(MessageUtil.getReplacement("durability"),
                                String.valueOf(builder.getDurability()))) :
                config.getInt("subEditor.icons." + icon + ".durability", 0);
        boolean unbreakable = config.getBoolean("subEditor.icon." + icon + ".unbreakable", false);
        String name = config.getString("subEditor.icons." + icon + ".name", "&cInvalid Item: &4" + icon)
                .replace(MessageUtil.getReplacement("id"), String.valueOf(c));
        List<String> lore = config.getStringList("subEditor.icons." + icon + ".lore");
        for (int i = 0; i < lore.size(); i++) {
            if (lore.get(i).contains(MessageUtil.getReplacement("lore"))) {
                lore.remove(i);
                int newLines = 1;
                for (String line : builder.getLore()) {
                    lore.add(i - 1 + newLines, line);
                    newLines++;
                }
                i += newLines;
                continue;
            } else if (lore.get(i).contains(MessageUtil.getReplacement("commands"))) {
                lore.remove(i);
                int newLines = 1;

                for (DelayedCommand line : commands) {
                    lore.add(i - 1 + newLines,
                            config.getString("subEditor.icons.commands.commandPrefix", "&7- &a$<command>")
                                    .replace(MessageUtil.getReplacement("command"),
                                            line.getCmd())
                                    .replace(MessageUtil.getReplacement("delay"),
                                            String.valueOf(line.getDelay()))
                                    .replace(MessageUtil.getReplacement("caster"),
                                            line.getAs().name())
                    );
                    newLines++;
                }
                i += newLines;
                continue;
            } else if (lore.get(i).contains(MessageUtil.getReplacement("enchants"))) {
                lore.remove(i);
                int newLines = 1;
                for (Enchantment enchant : builder.getEnchants().keySet()) {
                    lore.add(i - 1 + newLines,
                            config.getString("subEditor.icons.enchants.enchantPrefix", "&7- &a$<enchant> $<level>")
                                    .replace(MessageUtil.getReplacement("enchant"),
                                            enchant.getKey().getKey())
                                    .replace(MessageUtil.getReplacement("level"),
                                            String.valueOf(builder.getEnchants().get(enchant)))
                    );
                    newLines++;
                }
                i += newLines;
                continue;
            } else if (lore.get(i).contains(MessageUtil.getReplacement("flags"))) {
                lore.remove(i);
                int newLines = 1;
                for (ItemFlag flag : builder.getFlags()) {
                    lore.add(i - 1 + newLines, config.getString("subEditor.icons.flags.flagPrefix", "&7- &a$<flag>")
                            .replace(MessageUtil.getReplacement("flag"),
                                    flag.name().toLowerCase())
                    );
                    newLines++;
                }
                i += newLines;
                continue;
            }

            lore.set(i, ChatUT.hexString(lore.get(i)
                            .replace(MessageUtil.getReplacement("id"), String.valueOf(c))
                            .replace(MessageUtil.getReplacement("name"), builder.getName())
                            .replace(MessageUtil.getReplacement("unbreakable"),
                                    String.valueOf(builder.isUnbreakable()))
                            .replace(MessageUtil.getReplacement("durability"),
                                    String.valueOf(builder.getDurability()))
                            .replace(MessageUtil.getReplacement("glowing"),
                                    String.valueOf(!builder.getEnchants().isEmpty())))
                    .replace(MessageUtil.getReplacement("material"),
                            builder.getMaterial().getID())
                    .replace(MessageUtil.getReplacement("amount"),
                            String.valueOf(builder.getAmount()))
            );
        }
        Map<Enchantment, Integer> enchants  = config.getEnchantmentSection("subEditor.icons." + icon + ".enchants");
        List<ItemFlag>            flags     = config.getItemFlags("subEditor.icons." + icon + ".flags");
        ItemFlag[]                itemFlags = flags.toArray(new ItemFlag[0]);
        return ItemBuilder.newItem(material)
                .amount(amount)
                .durability(durability)
                .unbreakable(unbreakable)
                .name(ChatUT.hexString(name))
                .lore(lore)
                .enchant(enchants)
                .flag(itemFlags)
                .build();
    }
}
