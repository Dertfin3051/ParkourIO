package ru.dfhub.parkourio.components.spawn;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.json.JSONObject;
import ru.dfhub.parkourio.util.Config;

public class SpawnHandler implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent e) {
        e.getPlayer().teleport(getSpawnLocation());
    }

    @EventHandler
    public void onFall(PlayerMoveEvent e) {
        if (!e.getTo().getWorld().getName().equalsIgnoreCase(Config.getConfig().getJSONObject("spawn-location").getString("world"))) return;

        if (e.getTo().getBlockY() <= Config.getConfig().getJSONObject("spawn-location").getInt("fall-level")) {
            e.getPlayer().teleport(getSpawnLocation());
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        if (!e.getPlayer().hasPermission("ru.dfhub.parkourio.spawn.build")) e.setCancelled(true);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        if (!e.getPlayer().hasPermission("ru.dfhub.parkourio.spawn.build")) e.setCancelled(true);
    }

    private Location getSpawnLocation() {
        JSONObject data = Config.getConfig().getJSONObject("spawn-location");
        return new Location(
                Bukkit.getWorld(data.optString("world", "world")),
                data.optDouble("x", 0),
                data.optDouble("y", 0),
                data.optDouble("z", 0),
                data.optFloat("yaw", 0.0F),
                data.optFloat("pitch", 0.0F)
        );
    }
}
