package studio.magemonkey.fusion.cfg.hooks;

public enum HookType {
    AuraSkills("AuraSkills"),
    Divinity("Divinity"),
    Fabled("Fabled"),
    Jobs("Jobs"),
    mcMMO("mcMMO"),
    Oraxen("Oraxen"),
    PlaceholderAPI("PlaceholderAPI"),
    Vault("Vault");





    private final String pluginName;

    HookType(String pluginName) {
        this.pluginName = pluginName;
    }

    @Override
    public String toString() {return pluginName;}
}
