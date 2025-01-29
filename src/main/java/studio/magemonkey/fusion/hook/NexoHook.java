package studio.magemonkey.fusion.hook;

import com.nexomc.nexo.api.events.NexoItemsLoadedEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.Consumer;

public class NexoHook implements Listener {
    private final Consumer<Void> postLoadHook;

    public NexoHook(JavaPlugin plugin, Consumer<Void> postLoadHook) {
        this.postLoadHook = postLoadHook;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void nexoLoad(NexoItemsLoadedEvent event) {
        postLoadHook.accept(null);
        HandlerList.unregisterAll(this);
    }
}