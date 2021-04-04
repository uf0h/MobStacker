package me.ufo.mobstacker.listeners;

import me.ufo.mobstacker.Config;
import me.ufo.mobstacker.MSPlugin;
import me.ufo.mobstacker.mob.StackedMob;
import me.ufo.shaded.it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.server.v1_8_R3.EntityInsentient;
import net.techcable.tacospigot.event.entity.SpawnerPreSpawnEvent;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

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

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onSpawnerSpawnEvent(final SpawnerPreSpawnEvent event) {
    event.setCancelled(true);

    if (!plugin.isSpawning()) {
      return;
    }

    final Location location = event.getLocation();
    final EntityType entityType = event.getSpawnedType();
    final StackedMob sm = StackedMob.getFirstByDistance(entityType, location);
    int spawns = Config.getRandomSpawnAmount();

    if (entityType == EntityType.SILVERFISH || entityType == EntityType.ENDERMITE) {
      spawns = spawns + 10;
    }

    if (sm == null || sm.getEntity() == null || sm.getEntity().isDead()) {
      //this.plugin.getLogger().info("Spawning " + spawns + " " + entityType);
      this.createStackedMobEntity(location, entityType, spawns);
      return;
    }

    //this.plugin.getLogger().info("Merging " + spawns + " " + entityType + " making it " + sm.addSetAndGet(spawns));
    sm.addSetAndGet(spawns);
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onChunkUnloadEvent(final ChunkUnloadEvent event) {
    //this.plugin.getLogger().info("ChunkUnloadEvent");
    final ObjectOpenHashSet<StackedMob> entities = StackedMob.getAllByChunk(event.getChunk());

    for (final StackedMob sm : entities) {
      //this.plugin.getLogger().info("Destroying " + sm.getType());
      sm.destroyEntity();
      StackedMob.getStackedMobs().remove(sm.getUniqueId());
    }
  }

  private void createStackedMobEntity(final Location spawnerLocation, final EntityType entityType, final int spawns) {
    final double x = spawnerLocation.getX() + (Config.RANDOM.nextInt(2) * 1.0D - 0.5D) * 4.0D + 0.5D;
    final double y = spawnerLocation.getY() + Config.RANDOM.nextInt(3) - 1.0D;
    final double z = spawnerLocation.getZ() + (Config.RANDOM.nextInt(2) * 1.0D - 0.5D) * 4.0D + 0.5D;

    final World world = spawnerLocation.getWorld();
    final Entity entity = world.spawnEntity(new Location(world, x, y, z), entityType);

    // check if entity can be spawned x2
    final net.minecraft.server.v1_8_R3.Entity _entity = ((CraftEntity) entity).getHandle();

    if (_entity instanceof EntityInsentient) {
      final EntityInsentient entityInsentient = (EntityInsentient) _entity;

      if (!entityInsentient.canSpawn()) {
        entityInsentient.setPosition(x, y + 1.0D, z);

        if (!entityInsentient.canSpawn()) {
          //this.plugin.getLogger().info("Failed to spawn " + spawns + " " + entityType);
          entity.remove();
          return;
        }
      }
    }

    StackedMob.getStackedMobs().put(entity.getUniqueId(), new StackedMob(entity, spawns));
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
