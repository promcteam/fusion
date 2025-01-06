package studio.magemonkey.fusion.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permissible;
import studio.magemonkey.codex.legacy.item.ItemBuilder;

public final class Utils {

    public static boolean hasCraftingPermission(Permissible permissible, String recipe) {
        return permissible.hasPermission("fusion.recipe." + recipe) || permissible.hasPermission("fusion.recipes") ||
                permissible.hasPermission("fusion.recipe.*");
    }

    public static boolean hasCraftingUsePermission(Permissible permissible, String item) {
        if (item == null) {
            return permissible.hasPermission("fusion.use") || permissible.hasPermission("fusion.use.*");
        }
        return permissible.hasPermission("fusion.use." + item) || permissible.hasPermission("fusion.use.*");
    }

    public static String getFormattedTime(int seconds) {
        int hours   = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs    = seconds % 60;

        // format it properly so when hours are 0 it just show minutes and seconds. If minutes are 0 it just shows seconds
        if (hours == 0) {
            if (minutes == 0) {
                return secs > 0 ? secs + "s" : "Finished";
            }
            return minutes + "m " + secs + "s";
        }
        return hours + "h " + minutes + "m " + secs + "s";
    }

    public static String getItemName(ItemStack item) {
        if (!item.hasItemMeta()) {
            String name = item.getType().name().replace("_", " ").toLowerCase();
            return name.substring(0, 1).toUpperCase() + name.substring(1);
        } else {
            return ItemBuilder.newItem(item).getName();
        }
    }
}
