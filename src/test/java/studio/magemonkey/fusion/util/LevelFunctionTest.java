package studio.magemonkey.fusion.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import studio.magemonkey.fusion.cfg.Cfg;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LevelFunctionTest {
    @BeforeEach
    void setUp() {
        Cfg.recursive = "floor(n+300^(n/7)^2)";
        Cfg.finalMod = "floor(x)/4";
    }

    @Test
    void generate() {
        LevelFunction.generate(10);
        assertEquals(10, LevelFunction.map.size());

        assertEquals(0, LevelFunction.pre.get(1));
        assertEquals(2, LevelFunction.pre.get(2));
        assertEquals(5, LevelFunction.pre.get(3));

        assertEquals(0, LevelFunction.map.get(1));
        assertEquals(0.5, LevelFunction.map.get(2));
        assertEquals(1.25, LevelFunction.map.get(3));
    }
}