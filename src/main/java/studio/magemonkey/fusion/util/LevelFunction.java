package studio.magemonkey.fusion.util;

import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.Cfg;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

public class LevelFunction {
    private static final Map<Integer, Double> map = new HashMap<>();

    public static void generate(int levels) {
        map.clear();

        DecimalFormat format = new DecimalFormat("0");
        for (int level = 1; level <= levels; level++) {
            double xp = 0;

            for (int n = 1; n < level; n++) {
                xp += Maths.eval(Cfg.recursive.replace("n",
                        Integer.toString(n)));//Math.floor(n + 300 * Math.pow(2, n / 7));
            }

            try {
                xp = Maths.eval(Cfg.finalMod.replace("x", format.format(xp)));
            } catch (RuntimeException e) {
                Fusion.getInstance()
                        .getLogger()
                        .info("Added levels up to " + (level - 1) + " before reaching Java limitations.");
                break;
            }
//            xp = Math.floor(xp);
//            xp /= 4;

            map.put(level, xp);
        }
    }

    public static void copyLevelMap(Map<Integer, Double> levelMap) {
        levelMap.putAll(map);
    }
}
