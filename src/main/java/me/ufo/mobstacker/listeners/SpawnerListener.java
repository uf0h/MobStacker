package me.ufo.mobstacker.listeners;

import java.util.UUID;
import me.ufo.mobstacker.Config;
import me.ufo.mobstacker.MSPlugin;
import me.ufo.mobstacker.mob.StackedMob;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;

public final class SpawnerListener implements Listener {

  private final MSPlugin plugin;

  public SpawnerListener(final MSPlugin plugin) {
    this.plugin = plugin;
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onCreatureSpawnEvent(final CreatureSpawnEvent event) {
    final CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();

    if (!plugin.isSpawning()) {
      if (reason != CreatureSpawnEvent.SpawnReason.SPAWNER_EGG && reason != CreatureSpawnEvent.SpawnReason.CUSTOM) {
        event.setCancelled(true);
      }
      return;
    }

    if (reason == CreatureSpawnEvent.SpawnReason.MOUNT || reason == CreatureSpawnEvent.SpawnReason.JOCKEY) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onSpawnerSpawnEvent(final SpawnerSpawnEvent event) {
    if (!plugin.isSpawning()) {
      event.setCancelled(true);
      return;
    }

    event.setCancelled(true);

    final Entity entity = event.getEntity();

    if (entity.getPassenger() != null) {
      entity.remove();
      entity.getPassenger().remove();
      return;
    }

    final Location location = event.getLocation();

    //plugin.syncTask(() -> {
    //try {
    final StackedMob sm = StackedMob.getFirstByDistance(entity, 8);
    final int spawns = Config.getRandomSpawnAmount();

    if (sm == null || sm.getEntity() == null || sm.getEntity().isDead()) {
      //plugin.syncTask(() -> {
      //Bukkit.getLogger().info("No stack found, creating stack: ");
      final Entity spawnedEntity = location.getWorld().spawnEntity(location, entity.getType());
      final UUID uniqueId = spawnedEntity.getUniqueId();

      StackedMob.getStackedMobs().put(uniqueId, new StackedMob(spawnedEntity, spawns));
      //});
      return;
    }

    sm.addSetAndGet(spawns);
      /*} catch (final Throwable throwable) {
        if (plugin.isDebugging()) {
          plugin.getLogger().info("NON-FATAL ERROR (IGNORE): [MSPlugin L243] " +
                                  throwable.getClass().getSimpleName());
          //throwable.printStackTrace();
        }
      }*/
    //});
  }

}
