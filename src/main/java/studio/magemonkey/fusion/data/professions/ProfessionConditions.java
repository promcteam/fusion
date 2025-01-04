package studio.magemonkey.fusion.data.professions;

import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.container.Job;
import com.gamingmesh.jobs.container.JobProgression;
import com.gmail.nossr50.api.ExperienceAPI;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.ability.Ability;
import dev.aurelium.auraskills.api.mana.ManaAbility;
import dev.aurelium.auraskills.api.registry.NamespacedId;
import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.api.stat.Stat;
import dev.aurelium.auraskills.api.user.SkillsUser;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.codex.util.DeserializationWorker;
import studio.magemonkey.codex.util.SerializationBuilder;
import studio.magemonkey.codex.util.messages.MessageData;
import studio.magemonkey.fabled.Fabled;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.BrowseConfig;
import studio.magemonkey.fusion.cfg.CraftingRequirementsCfg;
import studio.magemonkey.fusion.cfg.hooks.HookType;
import studio.magemonkey.fusion.data.player.FusionPlayer;
import studio.magemonkey.fusion.data.player.PlayerLoader;
import studio.magemonkey.fusion.data.recipes.RecipeItem;
import studio.magemonkey.fusion.util.ExperienceManager;
import studio.magemonkey.fusion.util.PlayerUtil;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
@Getter
public class ProfessionConditions implements ConfigurationSerializable {

    private final String profession;

    // Costs
    @Setter
    private   double                 moneyCost;
    @Setter
    private   int                    expCost;
    protected LinkedList<RecipeItem> requiredItems = new LinkedList<>();

    // Conditions
    @Setter
    private int     professionLevel;
    @Setter
    private boolean isMastery;
    @Setter
    private String  rank;

    private final Map<String, Integer> professionConditions      = new LinkedHashMap<>();
    private final Map<String, Integer> fabledClassConditions     = new LinkedHashMap<>();
    private final Map<String, Integer> mcMMOConditions           = new LinkedHashMap<>();
    private final Map<String, Integer> jobsConditions            = new LinkedHashMap<>();
    private final Map<String, Integer> auraAbilityConditions     = new LinkedHashMap<>();
    private final Map<String, Integer> auraManaAbilityConditions = new LinkedHashMap<>();
    private final Map<String, Integer> auraSkillsConditions      = new LinkedHashMap<>();
    private final Map<String, Integer> auraStatsConditions       = new LinkedHashMap<>();

    @Getter
    protected List<Object> requiredItemNames = new LinkedList<>();

    public ProfessionConditions(String profession,
                                double moneyCost,
                                int expCost,
                                LinkedList<Object> requiredItemNames,
                                int professionLevel,
                                boolean isMastery,
                                String rank,
                                Map<String, Integer> professionConditions,
                                Map<String, Integer> fabledClassConditions,
                                Map<String, Integer> mcMMOConditions,
                                Map<String, Integer> jobsConditions,
                                Map<String, Integer> auraAbilityConditions,
                                Map<String, Integer> auraManaAbilityConditions,
                                Map<String, Integer> auraSkillsConditions,
                                Map<String, Integer> auraStatsConditions) {
        this.profession = profession;
        this.moneyCost = moneyCost;
        this.expCost = expCost;
        this.requiredItemNames = requiredItemNames;
        this.requiredItems = requiredItemNames.stream()
                .map(RecipeItem::fromConfig)
                .collect(Collectors.toCollection(LinkedList::new));
        this.professionLevel = professionLevel;
        this.isMastery = isMastery;
        this.rank = rank;
        this.professionConditions.putAll(professionConditions);
        this.fabledClassConditions.putAll(fabledClassConditions);
        this.mcMMOConditions.putAll(mcMMOConditions);
        this.jobsConditions.putAll(jobsConditions);
        this.auraAbilityConditions.putAll(auraAbilityConditions);
        this.auraManaAbilityConditions.putAll(auraManaAbilityConditions);
        this.auraSkillsConditions.putAll(auraSkillsConditions);
        this.auraStatsConditions.putAll(auraStatsConditions);
    }

    public ProfessionConditions(String profession, ConfigurationSection config) {
        this.profession = profession;
        this.moneyCost = config.getDouble("costs.money", 0.0);
        this.expCost = config.getInt("costs.exp", 0);
        this.requiredItems = config.getStringList("costs.items")
                .stream()
                .map(RecipeItem::fromConfig)
                .collect(Collectors.toCollection(LinkedList::new));
        this.requiredItemNames = (List<Object>) config.getList("costs.items");

        this.professionLevel = config.getInt("conditions.professionLevel", 0);
        this.isMastery = config.getBoolean("conditions.mastery", false);
        this.rank = config.getString("conditions.rank");

        if (config.isSet("conditions.professions")) {
            for (String key : Objects.requireNonNull(config.getConfigurationSection("conditions.professions"))
                    .getKeys(false)) {
                professionConditions.put(key, config.getInt("conditions.professions." + key));
            }
        }

        if (config.isSet("conditions.fabled")) {
            if (Fusion.getHookManager().isHooked(HookType.Fabled)) {
                for (String key : Objects.requireNonNull(config.getConfigurationSection("conditions.fabled"))
                        .getKeys(false)) {
                    fabledClassConditions.put(key, config.getInt("conditions.fabled." + key));
                }
            }
        }
        if (Fusion.getHookManager().isHooked(HookType.mcMMO)) {
            if (config.isSet("conditions.mcmmo")) {
                for (String key : Objects.requireNonNull(config.getConfigurationSection("conditions.mcmmo"))
                        .getKeys(false)) {
                    mcMMOConditions.put(key, config.getInt("conditions.mcmmo." + key));
                }
            }
        }
        if (Fusion.getHookManager().isHooked(HookType.Jobs)) {
            if (config.isSet("conditions.jobs")) {
                for (String key : Objects.requireNonNull(config.getConfigurationSection("conditions.jobs"))
                        .getKeys(false)) {
                    jobsConditions.put(key, config.getInt("conditions.jobs." + key));
                }
            }
        }
        if (Fusion.getHookManager().isHooked(HookType.AuraSkills)) {
            if (config.isSet("conditions.aura_abilities")) {
                for (String key : Objects.requireNonNull(config.getConfigurationSection("conditions.aura_abilities"))
                        .getKeys(false)) {
                    auraAbilityConditions.put(key, config.getInt("conditions.aura_abilities." + key));
                }
            }
            if (config.isSet("conditions.aura_mana_abilities")) {
                for (String key : Objects.requireNonNull(config.getConfigurationSection("conditions.aura_mana_abilities"))
                        .getKeys(false)) {
                    auraAbilityConditions.put(key, config.getInt("conditions.aura_mana_abilities." + key));
                }
            }
            if (config.isSet("conditions.aura_skills")) {
                for (String key : Objects.requireNonNull(config.getConfigurationSection("conditions.aura_skills"))
                        .getKeys(false)) {
                    auraSkillsConditions.put(key, config.getInt("conditions.aura_skills." + key));
                }
            }
            if (config.isSet("conditions.aura_stats")) {
                for (String key : Objects.requireNonNull(config.getConfigurationSection("conditions.aura_stats"))
                        .getKeys(false)) {
                    auraSkillsConditions.put(key, config.getInt("conditions.aura_stats." + key));
                }
            }
        }
    }

    public ProfessionConditions(String profession, DeserializationWorker dw) {
        this.profession = profession;
        this.professionLevel = 0;
        this.moneyCost = 0;
        this.expCost = 0;

        Map<String, Object> costsSection = dw.getSection("costs");
        if (costsSection != null) {
            this.moneyCost = (double) costsSection.getOrDefault("money", 0.0);
            this.expCost = (int) costsSection.getOrDefault("exp", 0);
            this.requiredItems = ((List<Object>) costsSection.get("items"))
                    .stream()
                    .map(RecipeItem::fromConfig)
                    .collect(Collectors.toCollection(LinkedList::new));
            this.requiredItemNames = (List<Object>) costsSection.get("items");
        }

        Map<String, Object> conditionsSection = dw.getSection("conditions");


        if (conditionsSection != null) {
            this.professionLevel = (int) conditionsSection.getOrDefault("professionLevel", 0);
            this.isMastery = (boolean) conditionsSection.getOrDefault("mastery", false);
            this.rank = (String) conditionsSection.getOrDefault("rank", null);

            Map<String, Object> conditions = (Map<String, Object>) conditionsSection.get("professions");
            if (conditions != null) {
                for (Map.Entry<String, Object> entry : conditions.entrySet()) {
                    professionConditions.put(entry.getKey(), (int) entry.getValue());
                }
            }

            conditions = (Map<String, Object>) conditionsSection.get("fabled");
            if (Fusion.getHookManager().isHooked(HookType.Fabled) && conditions != null) {
                for (Map.Entry<String, Object> entry : conditions.entrySet()) {
                    fabledClassConditions.put(entry.getKey(), (int) entry.getValue());
                }
            }

            conditions = (Map<String, Object>) conditionsSection.get("mcmmo");
            if (Fusion.getHookManager().isHooked(HookType.mcMMO) && conditions != null) {
                for (Map.Entry<String, Object> entry : conditions.entrySet()) {
                    mcMMOConditions.put(entry.getKey(), (int) entry.getValue());
                }
            }

            conditions = (Map<String, Object>) conditionsSection.get("jobs");
            if (Fusion.getHookManager().isHooked(HookType.Jobs) && conditions != null) {
                for (Map.Entry<String, Object> entry : conditions.entrySet()) {
                    jobsConditions.put(entry.getKey(), (int) entry.getValue());
                }
            }

            if (Fusion.getHookManager().isHooked(HookType.AuraSkills)) {
                conditions = (Map<String, Object>) conditionsSection.get("aura_abilities");
                if (conditions != null) {
                    for (Map.Entry<String, Object> entry : conditions.entrySet()) {
                        auraAbilityConditions.put(entry.getKey(), (int) entry.getValue());
                    }
                }
                conditions = (Map<String, Object>) conditionsSection.get("aura_mana_abilities");
                if (conditions != null) {
                    for (Map.Entry<String, Object> entry : conditions.entrySet()) {
                        auraManaAbilityConditions.put(entry.getKey(), (int) entry.getValue());
                    }
                }
                conditions = (Map<String, Object>) conditionsSection.get("aura_skills");
                if (conditions != null) {
                    for (Map.Entry<String, Object> entry : conditions.entrySet()) {
                        auraSkillsConditions.put(entry.getKey(), (int) entry.getValue());
                    }
                }
                conditions = (Map<String, Object>) conditionsSection.get("aura_stats");
                if (conditions != null) {
                    for (Map.Entry<String, Object> entry : conditions.entrySet()) {
                        auraStatsConditions.put(entry.getKey(), (int) entry.getValue());
                    }
                }
            }
        }
    }

    public boolean isValid(FusionPlayer player) {
        Player _player  = player.getPlayer();
        int    unlocked = player.getJoinedProfessions().size();
        int    allowed  = PlayerUtil.getPermOption(_player, "fusion.limit");

        MessageData[] data = {
                new MessageData("profession", profession),
                new MessageData("unlocked", unlocked),
                new MessageData("limit", allowed),
                new MessageData("bal",
                        CodexEngine.get().getVault() == null ? 0
                                : CodexEngine.get().getVault().getBalance(_player)),

                new MessageData("costs.money", moneyCost),
                new MessageData("costs.exp", expCost),

                new MessageData("conditions.professionLevel", professionLevel),
                new MessageData("conditions.mastery", isMastery),
                new MessageData("condition.level", null),
                new MessageData("condition.name", null),
        };

        if (player.hasProfession(profession)) {
            _player.playSound(_player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1f, 1f);
            CodexEngine.get().getMessageUtil().sendMessage("fusion.browse.alreadyUnlocked", _player, data);
            return false;
        }

        if (unlocked >= allowed) {
            _player.playSound(_player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1f, 1f);
            CodexEngine.get().getMessageUtil().sendMessage("fusion.error.limitReached", _player, data);
            return false;
        }

        if (moneyCost > 0 && !CodexEngine.get().getVault().canPay(_player, moneyCost)) {
            _player.playSound(_player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1f, 1f);
            CodexEngine.get().getMessageUtil().sendMessage("fusion.browse.noFundsMoney", _player, data);
            return false;
        }


        if (expCost > 0 && ExperienceManager.getTotalExperience(_player) < expCost) {
            _player.playSound(_player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1f, 1f);
            CodexEngine.get().getMessageUtil().sendMessage("fusion.browse.noFundsExperience", _player, data);
            return false;
        }

        if (professionLevel > 0 && player.getProfession(profession).getLevel() < professionLevel) {
            _player.playSound(_player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1f, 1f);
            CodexEngine.get().getMessageUtil().sendMessage("fusion.error.noLevel", _player, data);
            return false;
        }

        if (isMastery && !player.getProfession(profession).isMastered()) {
            _player.playSound(_player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1f, 1f);
            CodexEngine.get().getMessageUtil().sendMessage("fusion.error.noMastery", _player, data);
            return false;
        }

        if (rank != null && !player.getPlayer().hasPermission("fusion.rank." + rank)) {
            _player.playSound(_player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1f, 1f);
            CodexEngine.get().getMessageUtil().sendMessage("fusion.gui.professions.rank." + rank, player.getPlayer());
            return false;
        }

        for (Map.Entry<String, Integer> entry : professionConditions.entrySet()) {
            if (!BrowseConfig.getProfessions().contains(entry.getKey())) {
                Fusion.getInstance()
                        .getLogger()
                        .warning("Invalid profession-condition in browse.yml: " + entry.getKey());
                continue;
            }
            if (player.getProfession(entry.getKey()).getLevel() < entry.getValue()) {
                _player.playSound(_player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1f, 1f);
                data[data.length - 2] = new MessageData("condition.level", entry.getValue());
                data[data.length - 1] = new MessageData("condition.name",
                        CodexEngine.get()
                                .getMessageUtil()
                                .getMessageAsString("fusion.conditionFormatting.profession",
                                        entry.getKey(),
                                        new MessageData("profession", entry.getKey())));
                CodexEngine.get().getMessageUtil().sendMessage("fusion.browse.noConditionLevel", _player, data);
                return false;
            }
        }

        if (Fusion.getHookManager().isHooked(HookType.Fabled)) {
            for (Map.Entry<String, Integer> entry : fabledClassConditions.entrySet()) {
                if (!Fabled.getClasses().containsKey(entry.getKey())) {
                    Fusion.getInstance()
                            .getLogger()
                            .warning("Invalid fabled-class condition in browse.yml: " + entry.getKey());
                    continue;
                }
                int level = Fabled.getPlayerAccounts(_player).getActiveData().getClass(entry.getKey()).getLevel();
                if (level < entry.getValue()) {
                    _player.playSound(_player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1f, 1f);
                    data[data.length - 2] = new MessageData("condition.level", entry.getValue());
                    data[data.length - 1] = new MessageData("condition.name",
                            CodexEngine.get()
                                    .getMessageUtil()
                                    .getMessageAsString("fusion.conditionFormatting.fabled",
                                            entry.getKey(),
                                            new MessageData("class",
                                                    Fabled.getClasses().get(entry.getKey()).getName())));
                    CodexEngine.get().getMessageUtil().sendMessage("fusion.browse.noConditionLevel", _player, data);
                    return false;
                }
            }
        }

        if (Fusion.getHookManager().isHooked(HookType.mcMMO)) {
            for (Map.Entry<String, Integer> entry : mcMMOConditions.entrySet()) {
                PrimarySkillType skill = PrimarySkillType.getSkill(entry.getKey().toUpperCase());
                if (skill == null) {
                    Fusion.getInstance()
                            .getLogger()
                            .warning("Invalid mcmmo-skill condition in browse.yml: " + entry.getKey());
                    continue;
                }

                int level = ExperienceAPI.getLevel(_player, skill);
                if (level < entry.getValue()) {
                    _player.playSound(_player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1f, 1f);
                    data[data.length - 2] = new MessageData("condition.level", entry.getValue());
                    data[data.length - 1] = new MessageData("condition.name",
                            CodexEngine.get()
                                    .getMessageUtil()
                                    .getMessageAsString("fusion.conditionFormatting.mcmmo",
                                            entry.getKey(),
                                            new MessageData("skill", skill.getName())));
                    CodexEngine.get().getMessageUtil().sendMessage("fusion.browse.noConditionLevel", _player, data);
                    return false;
                }
            }
        }

        if (Fusion.getHookManager().isHooked(HookType.Jobs)) {
            for (Map.Entry<String, Integer> entry : jobsConditions.entrySet()) {
                Job job = Jobs.getJob(entry.getKey());
                if (job == null) {
                    Fusion.getInstance().getLogger().warning("Invalid job condition in browse.yml: " + entry.getKey());
                    continue;
                }
                JobProgression progression = Jobs.getPlayerManager().getJobsPlayer(_player).getJobProgression(job);
                int            level       = progression != null ? progression.getLevel() : 0;
                if (level < entry.getValue()) {
                    _player.playSound(_player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1f, 1f);
                    data[data.length - 2] = new MessageData("condition.level", entry.getValue());
                    data[data.length - 1] = new MessageData("condition.name",
                            CodexEngine.get()
                                    .getMessageUtil()
                                    .getMessageAsString("fusion.conditionFormatting.jobs",
                                            entry.getKey(),
                                            new MessageData("job", job.getDisplayName())));
                    CodexEngine.get().getMessageUtil().sendMessage("fusion.browse.noConditionLevel", _player, data);
                    return false;
                }
            }
        }

        if (Fusion.getHookManager().isHooked(HookType.AuraSkills)) {
            SkillsUser user = AuraSkillsApi.get().getUser(_player.getUniqueId());
            for (Map.Entry<String, Integer> entry : auraAbilityConditions.entrySet()) {
                Ability ability =
                        AuraSkillsApi.get().getGlobalRegistry().getAbility(NamespacedId.fromString(entry.getKey()));
                if (ability == null) {
                    Fusion.getInstance()
                            .getLogger()
                            .warning("Invalid aura-ability condition in browse.yml: " + entry.getKey());
                    continue;
                }

                int level = user.getAbilityLevel(ability);
                if (level < entry.getValue()) {
                    _player.playSound(_player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1f, 1f);
                    data[data.length - 2] = new MessageData("condition.level", entry.getValue());
                    data[data.length - 1] = new MessageData("condition.name",
                            CodexEngine.get()
                                    .getMessageUtil()
                                    .getMessageAsString("fusion.conditionFormatting.auraAbility",
                                            entry.getKey(),
                                            new MessageData("ability", ability.getDisplayName(Locale.ENGLISH))));
                    CodexEngine.get().getMessageUtil().sendMessage("fusion.browse.noConditionLevel", _player, data);
                    return false;
                }
            }

            for (Map.Entry<String, Integer> entry : auraManaAbilityConditions.entrySet()) {
                ManaAbility ability =
                        AuraSkillsApi.get().getGlobalRegistry().getManaAbility(NamespacedId.fromString(entry.getKey()));
                if (ability == null) {
                    Fusion.getInstance()
                            .getLogger()
                            .warning("Invalid aura-mana-ability condition in browse.yml: " + entry.getKey());
                    continue;
                }

                int level = user.getManaAbilityLevel(ability);
                if (level < entry.getValue()) {
                    _player.playSound(_player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1f, 1f);
                    data[data.length - 2] = new MessageData("condition.level", entry.getValue());
                    data[data.length - 1] = new MessageData("condition.name",
                            CodexEngine.get()
                                    .getMessageUtil()
                                    .getMessageAsString("fusion.conditionFormatting.auraManaAbility",
                                            entry.getKey(),
                                            new MessageData("ability", ability.getDisplayName(Locale.ENGLISH))));
                    CodexEngine.get().getMessageUtil().sendMessage("fusion.browse.noConditionLevel", _player, data);
                    return false;
                }
            }

            for (Map.Entry<String, Integer> entry : auraSkillsConditions.entrySet()) {
                Skill skill = AuraSkillsApi.get().getGlobalRegistry().getSkill(NamespacedId.fromString(entry.getKey()));
                if (skill == null) {
                    Fusion.getInstance()
                            .getLogger()
                            .warning("Invalid aura-skill condition in browse.yml: " + entry.getKey());
                    continue;
                }
                int level = user.getSkillLevel(skill);
                if (level < entry.getValue()) {
                    _player.playSound(_player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1f, 1f);
                    data[data.length - 2] = new MessageData("condition.level", entry.getValue());
                    data[data.length - 1] = new MessageData("condition.name",
                            CodexEngine.get()
                                    .getMessageUtil()
                                    .getMessageAsString("fusion.conditionFormatting.auraSkill",
                                            entry.getKey(),
                                            new MessageData("skill", skill.getDisplayName(Locale.ENGLISH))));
                    CodexEngine.get().getMessageUtil().sendMessage("fusion.browse.noConditionLevel", _player, data);
                    return false;
                }
            }

            for (Map.Entry<String, Integer> entry : auraStatsConditions.entrySet()) {
                Stat stat = AuraSkillsApi.get().getGlobalRegistry().getStat(NamespacedId.fromString(entry.getKey()));
                if (stat == null) {
                    Fusion.getInstance()
                            .getLogger()
                            .warning("Invalid aura-stat condition in browse.yml: " + entry.getKey());
                    continue;
                }
                double level = user.getStatLevel(stat);
                if (level < entry.getValue()) {
                    _player.playSound(_player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1f, 1f);
                    data[data.length - 2] = new MessageData("condition.level", entry.getValue());
                    data[data.length - 1] = new MessageData("condition.name",
                            CodexEngine.get()
                                    .getMessageUtil()
                                    .getMessageAsString("fusion.conditionFormatting.auraSkill",
                                            entry.getKey(),
                                            new MessageData("skill", stat.name())));
                    CodexEngine.get().getMessageUtil().sendMessage("fusion.browse.noConditionLevel", _player, data);
                    return false;
                }
            }
        }
        return true;
    }

    public Map<String, Map<String, Integer>> getFullConditions() {
        Map<String, Map<String, Integer>> conditions = new TreeMap<>();
        conditions.put("profession", professionConditions);
        conditions.put("fabled", fabledClassConditions);
        conditions.put("mcmmo", mcMMOConditions);
        conditions.put("jobs", jobsConditions);
        conditions.put("auraAbility", auraAbilityConditions);
        conditions.put("auraManaAbility", auraManaAbilityConditions);
        conditions.put("auraSkill", auraSkillsConditions);
        conditions.put("auraStats", auraStatsConditions);
        return conditions;
    }

    public List<Map.Entry<Boolean, String>> getConditionLines(Player player) {
        FusionPlayer                     fusionPlayer = PlayerLoader.getPlayer(player.getUniqueId());
        List<Map.Entry<Boolean, String>> lines        = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : professionConditions.entrySet()) {
            Profession profession = fusionPlayer.getProfession(entry.getKey());
            boolean    condition  = profession != null && profession.getLevel() >= entry.getValue();
            lines.add(Map.entry(condition,
                    CraftingRequirementsCfg.getConditionLine("recipes",
                            "profession",
                            entry.getKey(),
                            profession != null ? profession.getLevel() : 0,
                            entry.getValue())));
        }
        if (Fusion.getHookManager().isHooked(HookType.Fabled)) {
            for (Map.Entry<String, Integer> entry : fabledClassConditions.entrySet()) {
                boolean condition = Fabled.getPlayerAccounts(player).getActiveData().getClass(entry.getKey()).getLevel()
                        >= entry.getValue();
                lines.add(Map.entry(condition,
                        CraftingRequirementsCfg.getConditionLine("recipes",
                                "fabled",
                                entry.getKey(),
                                Fabled.getPlayerAccounts(player).getActiveData().getClass(entry.getKey()).getLevel(),
                                entry.getValue())));
            }
        }
        if (Fusion.getHookManager().isHooked(HookType.mcMMO)) {
            for (Map.Entry<String, Integer> entry : mcMMOConditions.entrySet()) {
                PrimarySkillType skill     = PrimarySkillType.getSkill(entry.getKey().toUpperCase());
                boolean          condition = ExperienceAPI.getLevel(player, skill) >= entry.getValue();
                lines.add(Map.entry(condition,
                        CraftingRequirementsCfg.getConditionLine("recipes",
                                "mcmmo",
                                entry.getKey(),
                                ExperienceAPI.getLevel(player, skill),
                                entry.getValue())));
            }
        }
        if (Fusion.getHookManager().isHooked(HookType.Jobs)) {
            for (Map.Entry<String, Integer> entry : jobsConditions.entrySet()) {
                Job            job         = Jobs.getJob(entry.getKey());
                JobProgression progression = Jobs.getPlayerManager().getJobsPlayer(player).getJobProgression(job);
                boolean condition = progression != null
                        && Jobs.getPlayerManager().getJobsPlayer(player).getJobProgression(job).getLevel()
                        >= entry.getValue();
                lines.add(Map.entry(condition,
                        CraftingRequirementsCfg.getConditionLine("recipes",
                                "jobs",
                                entry.getKey(),
                                progression != null ? progression.getLevel() : 0,
                                entry.getValue())));
            }
        }
        if (Fusion.getHookManager().isHooked(HookType.AuraSkills)) {
            for (Map.Entry<String, Integer> entry : auraAbilityConditions.entrySet()) {
                Ability ability =
                        AuraSkillsApi.get().getGlobalRegistry().getAbility(NamespacedId.fromString(entry.getKey()));
                if (ability == null) continue;
                boolean condition =
                        AuraSkillsApi.get().getUser(player.getUniqueId()).getAbilityLevel(ability) >= entry.getValue();
                lines.add(Map.entry(condition,
                        CraftingRequirementsCfg.getConditionLine("recipes",
                                "auraAbility",
                                entry.getKey(),
                                AuraSkillsApi.get().getUser(player.getUniqueId()).getAbilityLevel(ability),
                                entry.getValue())));
            }
            for (Map.Entry<String, Integer> entry : auraManaAbilityConditions.entrySet()) {
                ManaAbility ability =
                        AuraSkillsApi.get().getGlobalRegistry().getManaAbility(NamespacedId.fromString(entry.getKey()));
                if (ability == null) continue;
                boolean condition = AuraSkillsApi.get().getUser(player.getUniqueId()).getManaAbilityLevel(ability)
                        >= entry.getValue();
                lines.add(Map.entry(condition,
                        CraftingRequirementsCfg.getConditionLine("recipes",
                                "auraManaAbility",
                                entry.getKey(),
                                AuraSkillsApi.get().getUser(player.getUniqueId()).getManaAbilityLevel(ability),
                                entry.getValue())));
            }
            for (Map.Entry<String, Integer> entry : auraSkillsConditions.entrySet()) {
                Skill skill = AuraSkillsApi.get().getGlobalRegistry().getSkill(NamespacedId.fromString(entry.getKey()));
                if (skill == null) continue;
                boolean condition =
                        AuraSkillsApi.get().getUser(player.getUniqueId()).getSkillLevel(skill) >= entry.getValue();
                lines.add(Map.entry(condition,
                        CraftingRequirementsCfg.getConditionLine("recipes",
                                "auraSkill",
                                entry.getKey(),
                                AuraSkillsApi.get().getUser(player.getUniqueId()).getSkillLevel(skill),
                                entry.getValue())));
            }
            for (Map.Entry<String, Integer> entry : auraStatsConditions.entrySet()) {
                Stat stat = AuraSkillsApi.get().getGlobalRegistry().getStat(NamespacedId.fromString(entry.getKey()));
                if (stat == null) continue;
                boolean condition =
                        AuraSkillsApi.get().getUser(player.getUniqueId()).getStatLevel(stat) >= entry.getValue();
                lines.add(Map.entry(condition,
                        CraftingRequirementsCfg.getConditionLine("recipes",
                                "auraStat",
                                entry.getKey(),
                                (int) AuraSkillsApi.get().getUser(player.getUniqueId()).getStatLevel(stat),
                                entry.getValue())));
            }
        }
        return lines;
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> costsMap = new LinkedHashMap<>();
        costsMap.put("money", this.moneyCost);
        costsMap.put("exp", this.expCost);
        costsMap.put("items", this.requiredItemNames);

        Map<String, Object> conditionsMap = new LinkedHashMap<>();
        conditionsMap.put("professionLevel", this.professionLevel);
        conditionsMap.put("mastery", this.isMastery);
        conditionsMap.put("rank", this.rank);
        if (!professionConditions.isEmpty())
            conditionsMap.put("professions", this.professionConditions);
        if (!fabledClassConditions.isEmpty())
            conditionsMap.put("fabled", this.fabledClassConditions);
        if (!mcMMOConditions.isEmpty())
            conditionsMap.put("mcmmo", this.mcMMOConditions);
        if (!jobsConditions.isEmpty())
            conditionsMap.put("jobs", this.jobsConditions);
        if (!auraAbilityConditions.isEmpty())
            conditionsMap.put("aura_abilities", this.auraAbilityConditions);
        if (!auraManaAbilityConditions.isEmpty())
            conditionsMap.put("aura_mana_abilities", this.auraManaAbilityConditions);
        if (!auraSkillsConditions.isEmpty())
            conditionsMap.put("aura_skills", this.auraSkillsConditions);
        if (!auraStatsConditions.isEmpty())
            conditionsMap.put("aura_stats", this.auraStatsConditions);

        SerializationBuilder builder = SerializationBuilder.start(4)
                .append("costs", costsMap)
                .append("conditions", conditionsMap);
        return builder.build();
    }

    public void removeLastCondition() {
        // Remove the last index of the conditions
        if (checkRemoveFromMap(auraStatsConditions)) return;
        if (checkRemoveFromMap(auraSkillsConditions)) return;
        if (checkRemoveFromMap(auraManaAbilityConditions)) return;
        if (checkRemoveFromMap(auraAbilityConditions)) return;
        if (checkRemoveFromMap(jobsConditions)) return;
        if (checkRemoveFromMap(mcMMOConditions)) return;
        if (checkRemoveFromMap(fabledClassConditions)) return;
        checkRemoveFromMap(professionConditions);
    }

    private boolean checkRemoveFromMap(Map<String, Integer> map) {
        Map.Entry<String, Integer> lastEntry = null;
        for (Map.Entry<String, Integer> _entry : map.entrySet()) {
            lastEntry = _entry;
        }
        if (lastEntry != null) {
            map.remove(lastEntry.getKey());
            return true;
        }
        return false;
    }

    public static ProfessionConditions copy(ProfessionConditions conditions) {
        Map<String, Integer> professionConditions      = new LinkedHashMap<>(conditions.getProfessionConditions());
        Map<String, Integer> fabledClassConditions     = new LinkedHashMap<>(conditions.getFabledClassConditions());
        Map<String, Integer> mcMMOConditions           = new LinkedHashMap<>(conditions.getMcMMOConditions());
        Map<String, Integer> jobsConditions            = new LinkedHashMap<>(conditions.getJobsConditions());
        Map<String, Integer> auraAbilityConditions     = new LinkedHashMap<>(conditions.getAuraAbilityConditions());
        Map<String, Integer> auraManaAbilityConditions = new LinkedHashMap<>(conditions.getAuraManaAbilityConditions());
        Map<String, Integer> auraSkillsConditions      = new LinkedHashMap<>(conditions.getAuraSkillsConditions());
        Map<String, Integer> auraStatsConditions       = new LinkedHashMap<>(conditions.getAuraStatsConditions());


        return new ProfessionConditions(conditions.getProfession(),
                conditions.getMoneyCost(),
                conditions.getExpCost(),
                new LinkedList<>(conditions.getRequiredItemNames()),
                conditions.getProfessionLevel(),
                conditions.isMastery(),
                conditions.getRank(),
                professionConditions,
                fabledClassConditions,
                mcMMOConditions,
                jobsConditions,
                auraAbilityConditions,
                auraManaAbilityConditions,
                auraSkillsConditions,
                auraStatsConditions);
    }
}
