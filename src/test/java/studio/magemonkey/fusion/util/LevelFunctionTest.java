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
        System.out.println(LevelFunction.pre);
        System.out.println(LevelFunction.map);
    }
}