package studio.magemonkey.fusion.cfg.hooks.divinity;

import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.modules.list.itemgenerator.ItemGeneratorManager;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.hooks.ItemGenEntry;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class DivinityService {
    public static DivinityCfg               divinityCfg;
    public static Map<String, ItemGenEntry> itemGenResults = new HashMap<>();

    public static boolean isCached(String itemId) {
        return itemGenResults.containsKey(itemId);
    }

    public static boolean cache(String itemId, Consumer<ItemGenEntry> callback) {
        ItemGeneratorManager.GeneratorItem item =
                Divinity.getInstance().getModuleCache().getTierManager().getItemById(itemId);
        if (item == null) {
            Fusion.getInstance()
                    .getLogger()
                    .warning("Failed to cache item " + itemId + " from Divinity as it does not exist.");
            return false;
        }
        itemGenResults.put(itemId, new ItemGenEntry(item));
        if (callback != null)
            callback.accept(itemGenResults.get(itemId));
        return true;
    }

    public static void init() {
        itemGenResults.clear();
        divinityCfg = new DivinityCfg();
        Fusion.getInstance().getLogger().info("Divinity Service loaded.");
    }
}
