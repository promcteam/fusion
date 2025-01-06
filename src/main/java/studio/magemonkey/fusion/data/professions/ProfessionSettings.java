package studio.magemonkey.fusion.data.professions;

import lombok.Getter;
import lombok.Setter;

@Getter
public class ProfessionSettings {

    @Setter
    private Boolean enableItemLore;
    @Setter
    private Boolean hideNoPermission;
    @Setter
    private Boolean hideNoRank;
    @Setter
    private Boolean hideRecipeLimitReached;


}
