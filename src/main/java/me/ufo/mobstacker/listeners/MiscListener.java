package me.ufo.mobstacker.listeners;

import java.util.UUID;
import me.ufo.mobstacker.MSPlugin;
import me.ufo.shaded.it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;

public final class MiscListener implements Listener {

  private final MSPlugin plugin;

  public MiscListener(final MSPlugin plugin) {
    this.plugin = plugin;
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerQuitEvent(final PlayerQuitEvent event) {
    final Object2LongOpenHashMap<UUID> hitTimestamps = plugin.getHitTimestamps();

    if (hitTimestamps != null) {
      hitTimestamps.removeLong(event.getPlayer().getUniqueId());
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onVillagerInventoryOpenEvent(final InventoryOpenEvent event) {
    if (event.getInventory().getType() == InventoryType.MERCHANT) {
      event.setCancelled(true);
    }
  }

}
