package studio.magemonkey.fusion.cfg.editors.professions;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import studio.magemonkey.codex.legacy.item.ItemBuilder;
import studio.magemonkey.codex.util.messages.MessageUtil;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.YamlParser;
import studio.magemonkey.fusion.data.recipes.CraftingTable;
import studio.magemonkey.fusion.util.ChatUT;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ProfessionEditorCfg {

    private final YamlParser config;

    public ProfessionEditorCfg() {
        this.config = YamlParser.loadOrExtract(Fusion.getInstance(), "Editors/professions/ProfessionEditor.yml");
    }

    public String getTitle(String profession) {
        String title = config.getString("title", "&2Profession &a$<profession>");
        title = title.replace(MessageUtil.getReplacement("profession"), profession);
        return ChatUT.hexString(title);
    }

    public Map<String, ItemStack> getIcons(CraftingTable table) {
        Map<String, ItemStack> icons = new HashMap<>();
        for (String icon : config.getConfigurationSection("icons").getKeys(false)) {
            icons.put(icon, getIcon(table, icon));
        }
        return icons;
    }

    public ItemStack getIcon(CraftingTable table, String icon) {
        Material material = Material.valueOf(config.getString("icons." + icon + ".material", "STONE")
                .replace(MessageUtil.getReplacement("material"),
                        table.getIconItem().create().getType().name())
                .toUpperCase());
        int          amount      = config.getInt("icons." + icon + ".amount", 1);
        int          durability  = config.getInt("icons." + icon + ".durability", 0);
        boolean      unbreakable = config.getBoolean("icon." + icon + ".unbreakable", false);
        String       name        = config.getString("icons." + icon + ".name", "&cInvalid Item: &4" + icon);
        List<String> lore        = config.getStringList("icons." + icon + ".lore");
        for (int i = 0; i < lore.size(); i++) {
            lore.set(i, ChatUT.hexString(lore.get(i)
                    .replace(MessageUtil.getReplacement("name"), table.getInventoryName())
                    .replace(MessageUtil.getReplacement("icon"), table.getIconItem().getID())
                    .replace(MessageUtil.getReplacement("profession"), table.getName())
                    .replace(MessageUtil.getReplacement("masteryUnlock"),
                            String.valueOf(table.getMasteryUnlock()))
                    .replace(MessageUtil.getReplacement("maxLevel"), String.valueOf(table.getMaxLevel()))
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
                .name(name)
                .lore(lore)
                .enchant(enchants)
                .flag(itemFlags)
                .build();
    }
}
