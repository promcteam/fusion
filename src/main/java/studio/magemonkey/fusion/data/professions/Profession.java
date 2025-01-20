package studio.magemonkey.fusion.data.professions;

import lombok.Getter;
import lombok.Setter;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.cfg.sql.SQLManager;

import java.util.UUID;

@Getter

public class Profession {

    private final long    id;
    private final UUID    uuid;
    private final String  name;
    @Setter
    private       long    exp;
    @Setter
    private       boolean mastered;
    @Setter
    private       boolean joined;

    public Profession(long id, UUID uuid, String name, long exp, boolean mastered, boolean joined) {
        this.id = id;
        this.uuid = uuid;
        this.name = name;
        this.exp = exp;
        this.mastered = mastered;
        this.joined = joined;
    }

    public void addExp(long exp) {
        this.exp += exp;
    }

    public void removeExp(long exp) {
        this.exp -= exp;
    }

    public void resetExp() {
        this.exp = 0;
    }

    public void resetMastered() {
        this.mastered = false;
    }

    public void resetJoined() {
        this.joined = false;
    }

    public void reset() {
        resetExp();
        resetMastered();
        resetJoined();
    }

    public void update() {
        SQLManager.professions().setProfession(uuid, this);
    }

    public int getLevel() {
        return ProfessionsCfg.getTable(name).getLevelFunction().getLevel(exp);
    }

    public void setLevel(int level) {
        if (level < getLevel()) {
            long diffExp = (long) (exp - ProfessionsCfg.getTable(name).getLevelFunction().getXP(level));
            removeExp(diffExp);
        } else {
            long requiredExp = (long) (ProfessionsCfg.getTable(name).getLevelFunction().getXP(level) - exp);
            addExp(requiredExp);
        }
    }
}
