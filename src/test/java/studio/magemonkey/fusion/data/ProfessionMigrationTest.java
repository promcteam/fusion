package studio.magemonkey.fusion.data;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import studio.magemonkey.fusion.Fusion;

import java.io.InputStreamReader;
import java.util.Objects;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class ProfessionMigrationTest {
    MockedStatic<Fusion> fusionMockedStatic;

    @BeforeEach
    void setUp() {
        fusionMockedStatic = mockStatic(Fusion.class);
        // Supply a logger on Fusion#getInstance()#getLogger()
        Fusion fusion = mock(Fusion.class);
        fusionMockedStatic.when(Fusion::getInstance).thenReturn(fusion);
        when(fusion.getLogger()).thenReturn(Logger.getLogger("Fusion"));
    }

    @AfterEach
    void tearDown() {
        fusionMockedStatic.close();
    }

    @Test
    void test1_01Conversion() {
        InputStreamReader reader =
                new InputStreamReader(Objects.requireNonNull(
                        getClass().getClassLoader().getResourceAsStream(
                                "migrate/recipes.yml")));
        FileConfiguration config = YamlConfiguration.loadConfiguration(reader);

        ProfessionMigration.migrate(config);

        String configString = config.saveToString();
        assertTrue(configString.contains("version: '1.1'"));
        assertFalse(configString.contains("CUSTOMITEMS"));
        assertTrue(configString.contains("DIVINITY_custom_items"));
        assertTrue(configString.contains("~level"));
        assertTrue(configString.contains("fire_essenceq1:1"));
    }
}