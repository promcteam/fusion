package studio.magemonkey.fusion.cfg.hooks.divinity;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import studio.magemonkey.divinity.modules.list.itemgenerator.ItemGeneratorManager;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.YamlParser;
import studio.magemonkey.fusion.cfg.hooks.ItemGenEntry;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class DivinityCfgTest {
    private ItemGenEntry                       entry;
    private ItemGeneratorManager.GeneratorItem genItem;

    private MockedStatic<Fusion>     fusionStatic;
    private MockedStatic<YamlParser> yamlParser;

    @BeforeEach
    void setup() {
        Fusion fusion = mock(Fusion.class);
        fusionStatic = mockStatic(Fusion.class);
        //noinspection ResultOfMethodCallIgnored
        fusionStatic.when(Fusion::getInstance).thenReturn(fusion);

        YamlParser config = mock(YamlParser.class);

        yamlParser = mockStatic(YamlParser.class);
        yamlParser.when(() -> YamlParser.loadOrExtract(Fusion.getInstance(), "Hooks/Divinity.yml")).thenReturn(config);

        when(config.getString("ItemGenerator.RecipeIcon.LoreFormatting.single.levels", "levels $<amount>")).thenReturn(
                "levels $<amount>");
        when(config.getString("ItemGenerator.RecipeIcon.LoreFormatting.single.enchantments",
                "enchantments $<amount>")).thenReturn("enchantments $<amount>");

        entry = mock(ItemGenEntry.class);
        when(entry.getMinLevel()).thenReturn(1);
        when(entry.getMaxLevel()).thenReturn(1);
        when(entry.getMinEnchantments()).thenReturn(1);
        when(entry.getMaxEnchantments()).thenReturn(1);

        genItem = mock(ItemGeneratorManager.GeneratorItem.class);
        when(entry.getReference()).thenReturn(genItem);
    }

    @AfterEach
    void tearDown() {
        if (yamlParser != null) yamlParser.close();
        if (fusionStatic != null) fusionStatic.close();
    }

    @Test
    void replaceLore() {
        DivinityCfg  divinityCfg = new DivinityCfg();
        List<String> lore        = new ArrayList<>();
        lore.add("$<lore>");
        lore.add("§7§oDivinity: §f§o$<levels>");
        lore.add("§7§oEnchantments: §f§o$<enchants>");

        when(genItem.getLore()).thenReturn(List.of(
                "div-lore-1",
                "div-lore-2",
                "div-lore-3",
                "div-lore-4"
        ));

        divinityCfg.replaceLore(entry, 1, lore);

        assertEquals(5, lore.size());
        assertEquals("div-lore-1", lore.get(0));
        assertEquals("div-lore-2", lore.get(1));
        assertEquals("div-lore-3", lore.get(2));
        assertEquals("div-lore-4", lore.get(3));
        assertEquals("§7§oDivinity: §f§olevels 1", lore.get(4));
        // Because enchants are not set, the line should be removed
    }
}