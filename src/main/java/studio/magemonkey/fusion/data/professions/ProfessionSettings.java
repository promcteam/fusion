package studio.magemonkey.fusion.data.professions;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.util.DeserializationWorker;
import studio.magemonkey.codex.util.SerializationBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class ProfessionSettings implements ConfigurationSerializable {

    private final String profession;

    @Setter
    private boolean      enableLore;
    @Setter
    private List<String> lore;
    @Setter
    private Boolean      hideNoPermission;
    @Setter
    private Boolean      hideRecipeLimitReached;

    public ProfessionSettings(String profession, boolean enableItemLore, List<String> lore, Boolean hideNoPermission,
                              Boolean hideRecipeLimitReached) {
        this.profession = profession;
        this.enableLore = enableItemLore;
        this.lore = lore;
        this.hideNoPermission = hideNoPermission;
        this.hideRecipeLimitReached = hideRecipeLimitReached;
    }

    public ProfessionSettings(String profession, ConfigurationSection config) {
        this.profession = profession;
        this.enableLore = config.getBoolean("settings.enableLore", true);
        this.lore = config.getStringList("settings.lore");
        this.hideNoPermission = config.getBoolean("settings.hiding.hideNoPermission");
        this.hideRecipeLimitReached = config.getBoolean("settings.hiding.hideRecipeLimitReached");
    }

    public ProfessionSettings(String profession, DeserializationWorker dw) {
        this.profession = profession;
        Map<String, Object> settingsSection = dw.getSection("settings");

        if (settingsSection == null) {
            this.enableLore = true;
            this.lore = new ArrayList<>();
            this.hideNoPermission = null;
            this.hideRecipeLimitReached = null;
        } else {
            this.enableLore = (boolean) settingsSection.getOrDefault("enableLore", true);
            this.lore = (List<String>) settingsSection.getOrDefault("lore", new ArrayList<>());

            Map<String, Object> hiding = (Map<String, Object>) settingsSection.get("hiding");
            this.hideNoPermission =
                    (hiding != null && hiding.get("noPermission") != null) ? (boolean) hiding.get("noPermission")
                            : null;
            this.hideRecipeLimitReached = (hiding != null && hiding.get("recipeLimitReached") != null)
                    ? (boolean) hiding.get("recipeLimitReached") : null;
        }
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> settingsMap = new HashMap<>();

        settingsMap.put("enableLore", enableLore);
        settingsMap.put("lore", lore);

        Map<String, Object> hiding = new HashMap<>(3);
        if (hideNoPermission != null) hiding.put("noPermission", hideNoPermission);
        if (hideRecipeLimitReached != null) hiding.put("recipeLimitReached", hideRecipeLimitReached);
        if (!hiding.isEmpty()) {
            settingsMap.put("hiding", hiding);
        }

        return SerializationBuilder.start(4).append("settings", settingsMap).build();
    }

    public static ProfessionSettings copy(ProfessionSettings results) {
        return new ProfessionSettings(
                results.profession,
                results.enableLore,
                results.lore,
                results.hideNoPermission,
                results.hideRecipeLimitReached);
    }
}
