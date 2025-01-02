package studio.magemonkey.fusion.cfg.hooks.divinity;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import studio.magemonkey.codex.api.items.ItemType;
import studio.magemonkey.codex.legacy.item.ItemBuilder;
import studio.magemonkey.codex.util.messages.MessageUtil;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.YamlParser;
import studio.magemonkey.fusion.cfg.hooks.ItemGenEntry;
import studio.magemonkey.fusion.util.ChatUT;

import java.util.List;
import java.util.regex.Pattern;

public class DivinityCfg {

    private final YamlParser config;

    public DivinityCfg() {
        this.config = YamlParser.loadOrExtract(Fusion.getInstance(), "Hooks/Divinity.yml");
    }

    public ItemStack getRecipeIcon(ItemGenEntry entry, String name, ItemType type, int level) {
        Material material = Material.valueOf(config.getString("ItemGenerator.RecipeIcon.material", "$<material>")
                .replace(MessageUtil.getReplacement("material"),
                        type.create().getType().toString()));
        String itemName = ChatUT.hexString(config.getString("ItemGenerator.RecipeIcon.name", "$<name>")
                .replace(MessageUtil.getReplacement("name"), name));

        int customModelData = -1;
        if (entry.getReference().create(level, -1, type).getItemMeta().hasCustomModelData()) {
            customModelData = entry.getReference().create(level, -1, type).getItemMeta().getCustomModelData();
        }

        List<String> lore = config.getStringList("ItemGenerator.RecipeIcon.lore");
        replaceLore(entry, level, lore);

        ItemStack item = ItemBuilder.newItem(material).name(itemName).lore(lore).build();
        if (customModelData > 0) {
            ItemMeta meta = item.getItemMeta();
            meta.setCustomModelData(customModelData);
            item.setItemMeta(meta);
        }
        return item;
    }

    protected void replaceLore(ItemGenEntry entry, int level, List<String> lore) {
        String chanceLevels = level < 0 ? getValueFormat("levels", entry.getMinLevel(), entry.getMaxLevel())
                : getSingleValueFormat("levels", level);
        String chanceEnchants =
                getValueFormat("enchants", entry.getMinEnchantments(), entry.getMaxEnchantments());
        String chanceDamageTypes =
                getValueFormat("damage_types", entry.getMinDamageTypes(), entry.getMaxDamageTypes());
        String chanceDefenseTypes =
                getValueFormat("defense_types", entry.getMinDefenseTypes(), entry.getMaxDefenseTypes());
        String chanceItemStats  = getValueFormat("item_stats", entry.getMinItemStats(), entry.getMaxItemStats());
        String chanceGemSockets = getValueFormat("gem_sockets", entry.getMinGems(), entry.getMaxGems());
        String chanceEssenceSockets =
                getValueFormat("essence_sockets", entry.getMinEssences(), entry.getMaxEssences());
        String chanceRuneSockets = getValueFormat("rune_sockets", entry.getMinRunes(), entry.getMaxRunes());
        String chanceFabledAttributes =
                getValueFormat("fabled_attributes", entry.getMinFabledAttributes(), entry.getMaxFabledAttributes());
        String chanceSkills = getValueFormat("skills", entry.getMinSkill(), entry.getMaxSkill());

        for (int i = 0; i < lore.size(); i++) {
            if (lore.get(i).contains(MessageUtil.getReplacement("lore"))) {
                lore.remove(i);

                // Here, `i` is the index of the removed line.
                // We need to add the lore lines from the actual Divinity item starting there
                // and moving forward.
                for (String line : entry.getReference().getLore()) {
                    // If the lore has any placeholder with '%<placeholder>%',
                    // it will be skipped since it is something dynamic that can't be defined yet.
                    Pattern pattern = Pattern.compile("%[a-zA-Z0-9_]+%");
                    if (line.isEmpty()) continue;
                    if (pattern.matcher(line).find()) continue;
                    lore.add(i++, ChatUT.hexString(line));
                }
                i--;
                continue;
            }
            if (lore.get(i).contains(MessageUtil.getReplacement("levels"))) {
                String loreEntry = lore.get(i);

                if (chanceLevels == null) {
                    lore.remove(i);
                    i--;
                    continue;
                }

                lore.set(i,
                        loreEntry.replace(MessageUtil.getReplacement("levels"),
                                ChatUT.hexString(chanceLevels)));
                continue;
            }
            if (lore.get(i).contains(MessageUtil.getReplacement("enchants"))) {
                String loreEntry = lore.get(i);
                if (chanceEnchants == null) {
                    lore.remove(i);
                    i--;
                    continue;
                } else {
                    lore.set(i,
                            loreEntry.replace(MessageUtil.getReplacement("enchants"),
                                    ChatUT.hexString(chanceEnchants)));
                }
            }
            if (lore.get(i).contains(MessageUtil.getReplacement("damage_types"))) {
                String loreEntry = lore.get(i);

                if (chanceDamageTypes == null) {
                    lore.remove(i);
                    i--;
                    continue;
                } else {
                    lore.set(i,
                            loreEntry.replace(MessageUtil.getReplacement("damage_types"),
                                    ChatUT.hexString(chanceDamageTypes)));
                }
            }
            if (lore.get(i).contains(MessageUtil.getReplacement("defense_types"))) {
                String loreEntry = lore.get(i);

                if (chanceDefenseTypes == null) {
                    lore.remove(i);
                    i--;
                    continue;
                } else {
                    lore.set(i,
                            loreEntry.replace(MessageUtil.getReplacement("defense_types"),
                                    ChatUT.hexString(chanceDefenseTypes)));
                }
            }
            if (lore.get(i).contains(MessageUtil.getReplacement("item_stats"))) {
                String loreEntry = lore.get(i);
                if (chanceItemStats == null) {
                    lore.remove(i);
                    i--;
                    continue;
                } else {
                    lore.set(i,
                            loreEntry.replace(MessageUtil.getReplacement("item_stats"),
                                    ChatUT.hexString(chanceItemStats)));
                }
            }
            if (lore.get(i).contains(MessageUtil.getReplacement("gem_sockets"))) {
                String loreEntry = lore.get(i);

                if (chanceGemSockets == null) {
                    lore.remove(i);
                    i--;
                    continue;
                } else {
                    lore.set(i,
                            loreEntry.replace(MessageUtil.getReplacement("gem_sockets"),
                                    ChatUT.hexString(chanceGemSockets)));
                }
            }
            if (lore.get(i).contains(MessageUtil.getReplacement("essence_sockets"))) {
                String loreEntry = lore.get(i);

                if (chanceEssenceSockets == null) {
                    lore.remove(i);
                    i--;
                    continue;
                } else {
                    lore.set(i,
                            loreEntry.replace(MessageUtil.getReplacement("essence_sockets"),
                                    ChatUT.hexString(chanceEssenceSockets)));
                }
            }
            if (lore.get(i).contains(MessageUtil.getReplacement("rune_sockets"))) {
                String loreEntry = lore.get(i);

                if (chanceRuneSockets == null) {
                    lore.remove(i);
                    i--;
                    continue;
                } else {
                    lore.set(i,
                            loreEntry.replace(MessageUtil.getReplacement("rune_sockets"),
                                    ChatUT.hexString(chanceRuneSockets)));
                }
            }
            if (lore.get(i).contains(MessageUtil.getReplacement("fabled_attributes"))) {
                String loreEntry = lore.get(i);

                if (chanceFabledAttributes == null) {
                    lore.remove(i);
                    i--;
                    continue;
                } else {
                    lore.set(i,
                            loreEntry.replace(MessageUtil.getReplacement("fabled_attributes"),
                                    ChatUT.hexString(chanceFabledAttributes)));
                }
            }
            if (lore.get(i).contains(MessageUtil.getReplacement("skills"))) {
                String loreEntry = lore.get(i);

                if (chanceSkills == null) {
                    lore.remove(i);
                    i--;
                    continue;
                } else {
                    lore.set(i,
                            loreEntry.replace(MessageUtil.getReplacement("skills"),
                                    ChatUT.hexString(chanceSkills)));
                }
            }

            lore.set(i, ChatUT.hexString(lore.get(i)));
        }
    }

    private String getValueFormat(String path, int min, int max) {
        if (min <= 1 && max <= 1) return null;
        if (min == max)
            return ChatUT.hexString(getSingleValueFormat(path, min));
        return ChatUT.hexString(getRangeValueFormat(path, min, max));
    }

    private String getSingleValueFormat(String path, int amount) {
        return config.getString("ItemGenerator.RecipeIcon.LoreFormatting.single." + path, path + " $<amount>")
                .replace(MessageUtil.getReplacement("amount"), String.valueOf(amount));
    }

    private String getRangeValueFormat(String path, int min, int max) {
        return config.getString("ItemGenerator.RecipeIcon.LoreFormatting.range." + path, path + " $<min> - $<max>")
                .replace(MessageUtil.getReplacement("min"), String.valueOf(min))
                .replace(MessageUtil.getReplacement("max"), String.valueOf(max));
    }
}