package studio.magemonkey.fusion.cfg.hooks;

import lombok.Getter;
import studio.magemonkey.codex.api.items.ItemType;
import studio.magemonkey.divinity.modules.list.itemgenerator.ItemGeneratorManager;
import studio.magemonkey.divinity.modules.list.itemgenerator.api.IAttributeGenerator;
import studio.magemonkey.divinity.stats.items.ItemStats;
import studio.magemonkey.divinity.stats.items.attributes.DamageAttribute;
import studio.magemonkey.divinity.stats.items.attributes.DefenseAttribute;
import studio.magemonkey.divinity.stats.items.attributes.FabledAttribute;
import studio.magemonkey.divinity.stats.items.attributes.SocketAttribute;
import studio.magemonkey.divinity.stats.tiers.Tier;
import studio.magemonkey.fusion.cfg.hooks.divinity.DivinityUT;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Getter
public class ItemGenEntry {

    ItemGeneratorManager.GeneratorItem reference;

    private final int minLevel;
    private final int maxLevel;
    private final int minEnchantments;
    private final int maxEnchantments;

    private int minDamageTypes      = -1;
    private int maxDamageTypes      = -1;
    private int minDefenseTypes     = -1;
    private int maxDefenseTypes     = -1;
    private int minItemStats        = -1;
    private int maxItemStats        = -1;
    private int minGems             = -1;
    private int maxGems             = -1;
    private int minEssences         = -1;
    private int maxEssences         = -1;
    private int minRunes            = -1;
    private int maxRunes            = -1;
    private int minFabledAttributes = -1;
    private int maxFabledAttributes = -1;

    private final int minSkill;
    private final int maxSkill;

    private final Collection<ItemType> materials;
    private final Tier                 tier;


    public ItemGenEntry(ItemGeneratorManager.GeneratorItem entry) {
        this.reference = entry;


        minLevel = entry.getMinLevel();
        maxLevel = entry.getMaxLevel();
        minEnchantments = entry.getMinEnchantments();
        maxEnchantments = entry.getMaxEnchantments();

        for (IAttributeGenerator attribute : entry.getAttributeGenerators()) {
            if (attribute instanceof DamageAttribute) {
                minDamageTypes = attribute.getMinAmount();
                maxDamageTypes = attribute.getMaxAmount();
            } else if (attribute instanceof DefenseAttribute) {
                minDefenseTypes = attribute.getMinAmount();
                maxDefenseTypes = attribute.getMaxAmount();
            } else if (attribute instanceof ItemStats) {
                minItemStats = attribute.getMinAmount();
                maxItemStats = attribute.getMaxAmount();
            } else if (attribute instanceof SocketAttribute socketAttribute) {
                switch (socketAttribute.getType()) {
                    case GEM -> {
                        minGems = attribute.getMinAmount();
                        maxGems = attribute.getMaxAmount();
                    }
                    case ESSENCE -> {
                        minEssences = attribute.getMinAmount();
                        maxEssences = attribute.getMaxAmount();
                    }
                    case RUNE -> {
                        minRunes = attribute.getMinAmount();
                        maxRunes = attribute.getMaxAmount();
                    }
                }
            } else if (attribute instanceof FabledAttribute) {
                minFabledAttributes = attribute.getMinAmount();
                maxFabledAttributes = attribute.getMaxAmount();
            }
        }

        minSkill = entry.getAbilityGenerator().getMinAmount();
        maxSkill = entry.getAbilityGenerator().getMaxAmount();

        materials = entry.getMaterialsList();
        tier = entry.getTier();
    }

    public Map<ItemType, Set<String>> loadNames(ItemType type, int level) {
        Map<ItemType, Set<String>> names = new LinkedHashMap<>();
        if (type == null) {
            for (ItemType material : materials) {
                names.put(material, DivinityUT.getAllCombinations(reference, material.create().getType(), tier, level));
            }
        } else {
            names.put(type, DivinityUT.getAllCombinations(reference, type.create().getType(), tier, level));
        }

        return names;
    }
}
