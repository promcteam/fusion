package studio.magemonkey.fusion.cfg;

import org.bukkit.OfflinePlayer;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.data.player.PlayerLoader;
import studio.magemonkey.fusion.data.recipes.CraftingTable;
import studio.magemonkey.fusion.util.LevelFunction;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ProfessionLevelCfg {

    private final Map<Integer, Double> levelMap = new LinkedHashMap<>();
    private final CraftingTable        table;
    private       YamlParser           config;

    public ProfessionLevelCfg(CraftingTable table, String filePath) {
        this.table = table;

        // If the custom-formula is enabled, we load the custom levels from the file
        if (Cfg.useCustomFormula) {
            config = YamlParser.loadOrExtract(Fusion.getInstance(), filePath);

            if (config.getDoubleList("level-exp").isEmpty()) {
                Fusion.getInstance()
                        .getLogger()
                        .warning("No levels found in " + filePath + "! Generating default levels.");
                // Since we make no use of the Double, we are initiating natural numbers to avoid decimals
                List<Integer> levels = List.of(50, 300, 500, 750, 1300, 1600, 3500, 7000, 15000, 30000);
                config.set("level-exp", levels);
                config.save();
            }

            int i = 1;
            for (double expAmount : config.getDoubleList("level-exp")) {
                levelMap.put(i++, expAmount);
            }
        } else {
            // If the custom-formula is disabled, we copy the levels from the LevelFunction class
            LevelFunction.copyLevelMap(levelMap);
        }
    }

    public double getXP(int level) {
        return levelMap.get(level);
    }

    public int getLevel(double xp) {
        boolean                    seen = false;
        Map.Entry<Integer, Double> acc  = null;
        for (Map.Entry<Integer, Double> e : levelMap.entrySet()) {
            if (e.getValue() <= xp) {
                if (!seen) {
                    seen = true;
                    acc = e;
                } else {
                    acc = e;
                }
            }
        }
        Optional<Map.Entry<Integer, Double>> val = seen ? Optional.of(acc) : Optional.empty();

        return val.isPresent() ? val.get().getKey() : 0;
    }

    public int getLevel(OfflinePlayer player) {
        return getLevel(PlayerLoader.getPlayer(player.getUniqueId()).getExperience(table));
    }

    public void reload() {
        if (Cfg.useCustomFormula) {
            config.reload();
            levelMap.clear();

            int i = 1;
            for (double expAmount : config.getDoubleList("level-exp")) {
                levelMap.put(i++, expAmount);
            }
        } else {
            LevelFunction.copyLevelMap(levelMap);
        }
    }
}
