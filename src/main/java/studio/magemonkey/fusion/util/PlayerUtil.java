package studio.magemonkey.fusion.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachmentInfo;
import studio.magemonkey.codex.util.ItemUtils;
import studio.magemonkey.fusion.Fusion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PlayerUtil {

    /**
     * Attempts to get the numeric value of the permission starting with 'permission' <br/>
     * For example, if the player has 'perm.test.5' and 'perm.test' is passed in, 5 will be returned.
     * If the permission is not set, or the last part is non-numeric, 0 will be returned.
     *
     * @param player     The player to test
     * @param permission The permission to test
     * @return Number representing permission
     */
    public static int getPermOption(Player player, String permission) {
        int ret = 0;
        for (PermissionAttachmentInfo perm : player.getEffectivePermissions()) {
            String ps = perm.getPermission();
            if (ps.startsWith(permission.toLowerCase())) {
                String end = ps.substring(ps.lastIndexOf('.') + 1);
                try {
                    ret = Math.max(ret, Integer.parseInt(end));
                } catch (NumberFormatException e) {
                    Fusion.getInstance().log.warning("Could not get numeric permission value from '" + end + "'");
                    continue;
                }
            }
        }

        return ret;
    }

    public static double getProfessionExpBonusThroughPermissions(Player player, String profession) {
        int chance = 0;
        for (PermissionAttachmentInfo perm : player.getEffectivePermissions()) {
            String ps = perm.getPermission();
            if (ps.startsWith("fusion.experience." + profession.toLowerCase() + ".boost")) {
                String end = ps.substring(ps.lastIndexOf('.') + 1);
                try {
                    chance += Integer.parseInt(end);
                } catch (NumberFormatException e) {
                    Fusion.getInstance().log.warning("Could not get numeric permission value from '" + end + "'");
                }
            }
        }
        return (double) chance / 100;
    }

    public static List<String> getPlayerNames() {
        List<String> entries = new ArrayList<>();
        Bukkit.getOnlinePlayers().forEach(player -> entries.add(player.getName()));
        return entries;
    }

    public static Collection<ItemStack> getPlayerItems(InventoryHolder player) {
        ItemStack[]     contents = ItemUtils.compact(false, player.getInventory().getContents());
        List<ItemStack> result   = new ArrayList<>(contents.length);
        for (ItemStack content : contents) {
            if (content == null) {
                continue;
            }
            result.add(content);
        }
        return result;
    }
}
