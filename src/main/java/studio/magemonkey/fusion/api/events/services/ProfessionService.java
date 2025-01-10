package studio.magemonkey.fusion.api.events.services;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.codex.util.messages.MessageData;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.api.FusionAPI;
import studio.magemonkey.fusion.api.events.*;
import studio.magemonkey.fusion.data.player.FusionPlayer;
import studio.magemonkey.fusion.data.professions.Profession;
import studio.magemonkey.fusion.data.recipes.CraftingTable;
import studio.magemonkey.fusion.util.ExperienceManager;
import studio.magemonkey.fusion.util.PlayerUtil;

public class ProfessionService {

    /**
     * Call the ProfessionJoinEvent.
     * @param professionName The name of the profession.
     * @param player The player that wants to join the profession.
     * @param moneyCost The money cost to join the profession.
     * @param expCost The experience cost to join the profession.
     */
    public void joinProfession(String professionName, Player player, double moneyCost, int expCost) {
        ProfessionJoinEvent event = new ProfessionJoinEvent(professionName, player);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            event.getFusionPlayer()
                    .addProfession(new Profession(-1, player.getUniqueId(), professionName, 0, false, true));
            if (moneyCost > 0)
                CodexEngine.get().getVault().take(player, moneyCost);
            if (expCost > 0)
                ExperienceManager.setTotalExperience(player, (ExperienceManager.getTotalExperience(player) - expCost));

            MessageData[] data = {
                    new MessageData("profession", professionName),
                    new MessageData("costs.money", moneyCost),
                    new MessageData("costs.experience", expCost),
                    new MessageData("unlocked", event.getFusionPlayer().getJoinedProfessions().size()),
                    new MessageData("limit", PlayerUtil.getPermOption(player, "fusion.limit")),
                    new MessageData("bal", CodexEngine.get().getVault().getBalance(player))
            };

            CodexEngine.get().getMessageUtil().sendMessage("fusion.unlockedProfession", player, data);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        }
    }


    /**
     * Call the ProfessionLeaveEvent.
     * @param table The crafting table (profession) the player wants to leave.
     * @param player The player that wants to leave the profession.
     */
    public void leaveProfession(CraftingTable table, Player player) {
        ProfessionLeaveEvent event = new ProfessionLeaveEvent(table.getName(), player);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            event.getFusionPlayer().removeProfession(table);
            CodexEngine.get().getMessageUtil().sendMessage("fusion.forgotten",
                    player,
                    new MessageData("sender", player.getName()),
                    new MessageData("craftingTable", table));
        }
    }

    /**
     * Call the ProfessionGainXpEvent.
     * @param player The player that gains the experience.
     * @param table The crafting table (profession) the player gains experience in.
     * @param xp The amount of experience the player gains.
     */
    public void giveProfessionExp(Player player, CraftingTable table, long xp) {
        FusionPlayer fusionPlayer = FusionAPI.getPlayerManager().getPlayer(player);
        Profession   profession   = fusionPlayer.getProfession(table);
        if (profession == null) {
            Fusion.getInstance()
                    .getLogger()
                    .warning("Failed to give experience to player " + player.getName() + " for table " + table.getName()
                            + " as they do not have the profession.");
            return;
        }

        if(table.getMaxLevel() > 0 && profession.getLevel() >= table.getMaxLevel()) {
            return;
        }

        ProfessionGainXpEvent event = new ProfessionGainXpEvent(table.getName(), player, xp);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {

            int previousLevel = profession.getLevel();
            FusionAPI.getPlayerManager().getPlayer(player).addExperience(table.getName(), event.getGainedExp());
            int newLevel = profession.getLevel();
            if (newLevel != previousLevel) {
                levelUpProfession(player, table, previousLevel, newLevel);
            }
        }
    }

    /**
     * Call the ProfessionLevelUpEvent.
     * @param player The player that levels up.
     * @param table The crafting table (profession) the player levels up in.
     * @param previousLevel The previous level of the player.
     * @param newLevel The new level of the player.
     */
    public void levelUpProfession(Player player, CraftingTable table, int previousLevel, int newLevel) {
        if(newLevel < 0) {
            newLevel = 0;
        }
        ProfessionLevelUpEvent event = new ProfessionLevelUpEvent(table.getName(), player, previousLevel, newLevel);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            event.getFusionPlayer().getProfession(table).setLevel(event.getNewLevel());
            CodexEngine.get().getMessageUtil().sendMessage("fusion.levelup",
                    player,
                    new MessageData("craftingTable", table),
                    new MessageData("level", event.getNewLevel()));
        }
    }

    /**
     * Call the ProfessionMasteryEvent.
     * @param professionName The name of the profession.
     * @param player The player that masters the profession.
     * @param isMastered If the player has mastered the profession.
     */
    public void masterProfession(String professionName, Player player, boolean isMastered) {
        ProfessionMasteryEvent event = new ProfessionMasteryEvent(professionName, player, isMastered);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            event.getFusionPlayer().getProfession(professionName).setMastered(isMastered);
            if (event.isHasMastered()) {
                CodexEngine.get()
                        .getMessageUtil()
                        .sendMessage("fusion.mastered",
                                player,
                                new MessageData("sender", player),
                                new MessageData("craftingTable", event.getCraftingTable()));
            }
        }
    }
}
