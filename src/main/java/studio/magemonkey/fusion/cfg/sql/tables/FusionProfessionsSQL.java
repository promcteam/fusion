package studio.magemonkey.fusion.cfg.sql.tables;

import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.sql.SQLManager;
import studio.magemonkey.fusion.data.professions.Profession;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FusionProfessionsSQL {

    private final String Table = "fusion_professions";

    public FusionProfessionsSQL() {
        try (PreparedStatement create = SQLManager.connection()
                .prepareStatement("CREATE TABLE IF NOT EXISTS " + Table + "("
                        + "Id numeric, "
                        + "UUID varchar(36), "
                        + "Profession varchar(100),"
                        + "Experience numeric,"
                        + "Mastered boolean,"
                        + "Joined boolean)")) {
            create.execute();
        } catch (SQLException e) {
            Fusion.getInstance()
                    .getLogger()
                    .warning(
                            "[SQL:FusionProfessionsSQL:FusionProfessionsSQL] Something went wrong with the sql-connection: "
                                    + e.getMessage());
        }
    }

    public long getNextId() {
        try (PreparedStatement select = SQLManager.connection().prepareStatement("SELECT COUNT(*) FROM " + Table)) {
            ResultSet result = select.executeQuery();
            if (result.next()) {
                return result.getLong(1);
            }
        } catch (SQLException e) {
            Fusion.getInstance()
                    .getLogger()
                    .warning("[SQL:FusionProfessionsSQL:getNextId] Something went wrong with the sql-connection: "
                            + e.getMessage());
        }
        return 0;
    }

    public void setProfession(UUID uuid, Profession profession) {
        if (hasProfession(uuid, profession.getName())) {
            updateProfession(profession);
        } else {
            addProfession(profession);
        }
    }

    public void addProfession(Profession profession) {
        try (PreparedStatement insert = SQLManager.connection()
                .prepareStatement("INSERT INTO " + Table
                        + "(Id, UUID, Profession, Experience, Mastered, Joined) VALUES(?,?,?,?,?,?)")) {
            insert.setLong(1, getNextId());
            insert.setString(2, profession.getUuid().toString());
            insert.setString(3, profession.getName());
            insert.setDouble(4, profession.getExp());
            insert.setBoolean(5, profession.isMastered());
            insert.setBoolean(6, profession.isJoined());
            insert.execute();
        } catch (SQLException e) {
            Fusion.getInstance()
                    .getLogger()
                    .warning("[SQL:FusionProfessionsSQL:addProfession] Something went wrong with the sql-connection: "
                            + e.getMessage());
        }
    }

    public void updateProfession(Profession profession) {
        try (PreparedStatement update = SQLManager.connection()
                .prepareStatement("UPDATE " + Table + " SET Experience=?, Mastered=?, Joined=? WHERE Id=?")) {
            update.setDouble(1, profession.getExp());
            update.setBoolean(2, profession.isMastered());
            update.setBoolean(3, profession.isJoined());
            update.setLong(4, profession.getId());
            update.execute();
        } catch (SQLException e) {
            Fusion.getInstance()
                    .getLogger()
                    .warning(
                            "[SQL:FusionProfessionsSQL:updateProfession] Something went wrong with the sql-connection: "
                                    + e.getMessage());
        }
    }

    public boolean hasProfession(UUID uuid, String profession) {
        try (PreparedStatement select = SQLManager.connection()
                .prepareStatement("SELECT * FROM " + Table + " WHERE UUID=? AND Profession=?")) {
            select.setString(1, uuid.toString());
            select.setString(2, profession);
            return select.executeQuery().next();
        } catch (SQLException e) {
            Fusion.getInstance()
                    .getLogger()
                    .warning("[SQL:FusionProfessionsSQL:hasProfession] Something went wrong with the sql-connection: "
                            + e.getMessage());
        }
        return false;
    }

    public void removeProfession(String uuid, String profession) {
        try (PreparedStatement delete = SQLManager.connection()
                .prepareStatement("DELETE FROM " + Table + " WHERE UUID=? AND Profession=?")) {
            delete.setString(1, uuid);
            delete.setString(2, profession);
            delete.execute();
        } catch (SQLException e) {
            Fusion.getInstance()
                    .getLogger()
                    .warning(
                            "[SQL:FusionProfessionsSQL:removeProfession] Something went wrong with the sql-connection: "
                                    + e.getMessage());
        }
    }

    public List<Profession> getProfessions(UUID uuid) {
        List<Profession> entries = new ArrayList<>();
        entries.addAll(getProfessions(uuid, true));
        entries.addAll(getProfessions(uuid, false));
        return entries;
    }

    public List<Profession> getProfessions(UUID uuid, boolean joined) {
        List<Profession> entries = new ArrayList<>();
        try (PreparedStatement select = SQLManager.connection()
                .prepareStatement("SELECT * FROM " + Table + " WHERE UUID=? AND Joined=?")) {
            select.setString(1, uuid.toString());
            select.setBoolean(2, joined);
            ResultSet result = select.executeQuery();
            while (result.next()) {
                long    id         = result.getLong("Id");
                String  profession = result.getString("Profession");
                long  exp        = result.getLong("Experience");
                boolean mastered   = result.getBoolean("Mastered");
                boolean joined1    = result.getBoolean("Joined");
                entries.add(new Profession(id, uuid, profession, exp, mastered, joined1));
            }
            return entries;
        } catch (SQLException e) {
            Fusion.getInstance()
                    .getLogger()
                    .warning("[SQL:FusionProfessionsSQL:getProfessions] Something went wrong with the sql-connection: "
                            + e.getMessage());
        }
        return entries;
    }
}
