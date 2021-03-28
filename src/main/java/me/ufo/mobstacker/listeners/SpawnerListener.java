package me.ufo.mobstacker.listeners;

import java.util.UUID;
import me.ufo.mobstacker.Config;
import me.ufo.mobstacker.MSPlugin;
import me.ufo.mobstacker.mob.StackedMob;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
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
    final StackedMob sm = StackedMob.getFirstByDistance(entity, 64);
    final int spawns = Config.getRandomSpawnAmount();

    if (sm == null || sm.getEntity() == null || sm.getEntity().isDead()) {
      final Entity spawnedEntity = location.getWorld().spawnEntity(location, entity.getType());
      final UUID uniqueId = spawnedEntity.getUniqueId();

      /*if (entity.getType() == EntityType.SILVERFISH) {
        spawns = spawns + 10;
      } else if (entity.getType() == EntityType.ENDERMITE) {
        spawns = spawns + 10;
      }*/

      StackedMob.getStackedMobs().put(uniqueId, new StackedMob(spawnedEntity, spawns));
      return;
    }

    /*if (entity.getType() == EntityType.SILVERFISH) {
      spawns = spawns + 10;
    } else if (entity.getType() == EntityType.ENDERMITE) {
      spawns = spawns + 10;
    }*/

    sm.addSetAndGet(spawns);
  }

  /*
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
    final StackedMob sm = StackedMob.getFirstByDistance(entity, 8);
    int spawns = Config.getRandomSpawnAmount();

    if (sm == null || sm.getEntity() == null || sm.getEntity().isDead()) {
      final Entity spawnedEntity = location.getWorld().spawnEntity(location, entity.getType());
      final UUID uniqueId = spawnedEntity.getUniqueId();

      if (entity.getType() == EntityType.SILVERFISH) {
        spawns = spawns + 10;
      } else if (entity.getType() == EntityType.ENDERMITE) {
        spawns = spawns + 10;
      }

      StackedMob.getStackedMobs().put(uniqueId, new StackedMob(spawnedEntity, spawns));

      return;
    }

    if (entity.getType() == EntityType.SILVERFISH) {
      spawns = spawns + 10;
    } else if (entity.getType() == EntityType.ENDERMITE) {
      spawns = spawns + 10;
    }

    sm.addSetAndGet(spawns);
  }
   */

}
