package studio.magemonkey.fusion.cfg.hooks;

public enum HookType {

    Vault("Vault"),
    Fabled("Fabled"),
    mcMMO("mcMMO"),
    Jobs("Jobs"),
    AuraSkills("AuraSkills");

    private final String pluginName;

    HookType(String pluginName) {
        this.pluginName = pluginName;
    }

    @Override
    public String toString() { return pluginName; }
}
