package me.ufo.mobstacker.mob;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import me.ufo.mobstacker.Config;
import me.ufo.mobstacker.MSPlugin;
import me.ufo.shaded.it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.CraftChunk;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

public final class StackedMob {

  public static final String METADATA_KEY = "STACKED_MOB";
  private static final MSPlugin plugin = MSPlugin.get();
  private static final Map<UUID, StackedMob> STACKED_MOBS = new ConcurrentHashMap<>(1000);
  private static final FixedMetadataValue METADATA_VALUE = new FixedMetadataValue(plugin, true);

  private final Entity entity;
  private final EntityType type;
  private final UUID uniqueId;
  private final AtomicInteger stacked;

  public StackedMob(final Entity entity, final UUID uniqueId, final int stacked) {
    this.entity = entity;
    this.type = entity.getType();
    this.uniqueId = uniqueId;
    this.stacked = new AtomicInteger(stacked);

    this.entity.setMetadata(METADATA_KEY, METADATA_VALUE);
    this.setCustomName();
  }

  public StackedMob(final Entity entity) {
    this(entity, 1);
  }

  public StackedMob(final Entity entity, final int stacked) {
    this(entity, entity.getUniqueId(), stacked);
  }

  public Entity getEntity() {
    return entity;
  }

  public EntityType getType() {
    return type;
  }

  public Location getLocation() {
    return entity.getLocation();
  }

  public UUID getUniqueId() {
    return uniqueId;
  }

  public int getStackedAmount() {
    return stacked.get();
  }

  public int addAndGet(final int increment) {
    stacked.addAndGet(increment);
    return stacked.get();
  }

  public int addSetAndGet(final int increment) {
    stacked.addAndGet(increment);
    this.setCustomName();
    return stacked.get();
  }

  public int decrementAndGet() {
    stacked.decrementAndGet();
    return stacked.get();
  }

  public int decrementSetAndGet() {
    stacked.decrementAndGet();
    this.setCustomName();
    return stacked.get();
  }

  public void setCustomName() {
    if (!Bukkit.isPrimaryThread()) {
      plugin.syncTask(() -> {
        this.entity.setCustomName(Config.getStackedMobName(stacked.get(), entity));
      });
    } else {
      this.entity.setCustomName(Config.getStackedMobName(stacked.get(), entity));
    }
  }

  public StackedMob destroyEntity() {
    entity.removeMetadata(METADATA_KEY, plugin);
    entity.remove();
    return this;
  }

  /*public static StackedMob getFirstByDistance(final Entity entity, final int distance) {
    final ObjectIterator<Object2ObjectMap.Entry<UUID, StackedMob>> iterator =
      STACKED_MOBS.object2ObjectEntrySet().iterator();

    while (iterator.hasNext()) {
      final Object2ObjectMap.Entry<UUID, StackedMob> entry = iterator.next();

      if (entry == null) {
        continue;
      }

      final StackedMob stackedMob = entry.getValue();

      if (stackedMob == null) {
        continue;
      }

      if (stackedMob.getEntity().isDead()) {
        continue;
      }

      if (entity.getUniqueId().equals(stackedMob.getUniqueId())) {
        continue;
      }

      if (entity.getType() != stackedMob.getEntityType()) {
        continue;
      }

      if (entity.getLocation().getWorld() != stackedMob.getLocation().getWorld()) {
        continue;
      }

      if (entity.getLocation().distance(stackedMob.getLocation()) <= distance) {
        return stackedMob;
      }

    }

    return null;
  }*/

  /*public static ObjectOpenHashSet<StackedMob> getAllByDistance(final StackedMob stackedMob,
                                                               final int distance) {

    final ObjectOpenHashSet<StackedMob> out = new ObjectOpenHashSet<>();

    final ObjectIterator<Object2ObjectMap.Entry<UUID, StackedMob>> iterator =
      STACKED_MOBS.object2ObjectEntrySet().iterator();

    while (iterator.hasNext()) {
      final Object2ObjectMap.Entry<UUID, StackedMob> entry = iterator.next();

      if (entry == null) {
        continue;
      }

      final StackedMob other = entry.getValue();

      if (other.getUniqueId().equals(stackedMob.getUniqueId())) {
        continue;
      }

      if (other.getEntityType() != stackedMob.getEntityType()) {
        continue;
      }

      if (other.getLocation().getWorld() != stackedMob.getLocation().getWorld()) {
        continue;
      }

      if (other.getLocation().distance(stackedMob.getLocation()) <= distance) {
        out.add(other);
      }
    }

    return out;
  }*/

  /*public static StackedMob getFirstByDistance(final Entity entity, final double radius) {
    final Location location = entity.getLocation();
    final World world = location.getWorld();
    final double locX = location.getX();
    final double locZ = location.getZ();

    final int minX = ((int) Math.floor(locX - radius) >> 4);
    final int maxX = ((int) Math.floor(locX + radius) >> 4);
    final int minZ = ((int) Math.floor(locZ - radius) >> 4);
    final int maxZ = ((int) Math.floor(locZ + radius) >> 4);

    for (int x = minX; x <= maxX; x++) {
      for (int z = minZ; z <= maxZ; z++) {
        if (world.isChunkLoaded(x, z)) {
          try {
            for (final Entity other : world.getChunkAt(x, z).getEntities()) {
              if (other instanceof LivingEntity) {
                if (other instanceof Player || other instanceof ArmorStand || other.hasMetadata("NPC")) {
                  continue;
                }

                if (other.isDead()) {
                  continue;
                }

                if (entity.getUniqueId().equals(other.getUniqueId())) {
                  continue;
                }

                if (entity.getType() != other.getType()) {
                  continue;
                }

                final StackedMob stackedMob = STACKED_MOBS.get(other.getUniqueId());

                if (stackedMob != null) {
                  if (stackedMob.getEntity().isDead()) {
                    continue;
                  }

                  if (stackedMob.getLocation().distance(location) <= radius) {
                    return stackedMob;
                  }
                }
              }
            }
          } catch (final Throwable throwable) {
            if (INSTANCE.isDebug()) {
              INSTANCE.getLogger()
                .info("NON-FATAL ERROR (IGNORE): [StackedMob L208] " + throwable.getClass().getSimpleName());
            }
          }
        }
      }
    }

    return null;
  }*/

  /*public static ObjectOpenHashSet<StackedMob> getAllByDistance(final StackedMob stackedMob,
                                                               final double radius) {

    final ObjectOpenHashSet<StackedMob> entities = new ObjectOpenHashSet<>();

    final Location location = stackedMob.getLocation();
    final World world = location.getWorld();
    final double locX = location.getX();
    final double locZ = location.getZ();

    final int minX = ((int) Math.floor(locX - radius) >> 4);
    final int maxX = ((int) Math.floor(locX + radius) >> 4);
    final int minZ = ((int) Math.floor(locZ - radius) >> 4);
    final int maxZ = ((int) Math.floor(locZ + radius) >> 4);

    for (int x = minX; x <= maxX; x++) {
      for (int z = minZ; z <= maxZ; z++) {
        if (world.isChunkLoaded(x, z)) {
          try {
            for (final Entity entity : world.getChunkAt(x, z).getEntities()) {
              if (entity instanceof LivingEntity) {
                if (entity instanceof Player || entity instanceof ArmorStand || entity
                  .hasMetadata("NPC")) {
                  continue;
                }

                if (entity.isDead()) {
                  continue;
                }

                if (stackedMob.getUniqueId().equals(entity.getUniqueId())) {
                  continue;
                }

                if (stackedMob.getEntityType() != entity.getType()) {
                  continue;
                }

                final StackedMob other = STACKED_MOBS.get(entity.getUniqueId());

                if (other == null) {
                  continue;
                }

                entities.add(other);
              }
            }
          } catch (final Throwable throwable) {
            if (INSTANCE.isDebug()) {
              INSTANCE.getLogger()
                .info(
                  "NON-FATAL ERROR (IGNORE): [StackedMob L269] " + throwable.getClass()
                    .getSimpleName());
            }
          }
        }
      }
    }

    final ObjectIterator<StackedMob> entityIterator = entities.iterator();
    while (entityIterator.hasNext()) {
      if (entityIterator.next().getLocation().distance(location) > radius) {
        entityIterator.remove();
      }
    }

    return entities;
  }*/

  @Override
  public String toString() {
    return "StackedMob{" +
           "entityIsDead=" + (entity.isDead() ? ChatColor.RED.toString() + "YES" :
                              ChatColor.GREEN.toString() + "NO") + ChatColor.WHITE.toString() +
           ", location=" + entity.getLocation().toVector().toString() +
           ", type=" + type +
           ", uniqueId=" + uniqueId +
           ", stacked=" + stacked +
           '}';
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final StackedMob that = (StackedMob) o;

    return entity.equals(that.entity) && type == that.type && uniqueId.equals(that.uniqueId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(entity, type, uniqueId);
  }

  public static StackedMob getFirstByDistance(final Entity entity, final double radius) {
    final Location location = entity.getLocation();
    final double locX = location.getX();
    final double locZ = location.getZ();

    final int minX = ((int) Math.floor(locX - radius) >> 4);
    final int maxX = ((int) Math.floor(locX + radius) >> 4);
    final int minZ = ((int) Math.floor(locZ - radius) >> 4);
    final int maxZ = ((int) Math.floor(locZ + radius) >> 4);

    final World world = location.getWorld();

    for (int x = minX; x <= maxX; x++) {
      for (int z = minZ; z <= maxZ; z++) {
        if (!world.isChunkLoaded(x, z)) {
          continue;
        }

        final Chunk chunk = world.getChunkAt(x, z);

        final net.minecraft.server.v1_8_R3.Chunk nmsChunk = ((CraftChunk) chunk).getHandle();
        final List<net.minecraft.server.v1_8_R3.Entity>[] entitySlices = nmsChunk.entitySlices;

        for (final List<net.minecraft.server.v1_8_R3.Entity> slice : entitySlices) {
          for (final net.minecraft.server.v1_8_R3.Entity nmsEntity : slice) {
            final Entity other = nmsEntity.getBukkitEntity();

            if (other instanceof LivingEntity) {
              if (other instanceof Player || other instanceof ArmorStand || other.hasMetadata("NPC")) {
                continue;
              }

              if (other.isDead()) {
                continue;
              }

              if (entity.getType() != other.getType()) {
                continue;
              }

              if (entity.getUniqueId().equals(other.getUniqueId())) {
                continue;
              }

              final StackedMob sm = STACKED_MOBS.get(other.getUniqueId());

              if (sm != null) {
                if (sm.getEntity().isDead()) {
                  continue;
                }

                if (sm.getLocation().distance(location) <= radius) {
                  return sm;
                }
              }
            }
          }
        }
      }
    }

    return null;
  }

  public static ObjectOpenHashSet<StackedMob> getAllByDistance(final StackedMob sm, final double radius) {

    final ObjectOpenHashSet<StackedMob> entities = new ObjectOpenHashSet<>();

    final Location location = sm.getLocation();
    final double locX = location.getX();
    final double locZ = location.getZ();

    final int minX = ((int) Math.floor(locX - radius) >> 4);
    final int maxX = ((int) Math.floor(locX + radius) >> 4);
    final int minZ = ((int) Math.floor(locZ - radius) >> 4);
    final int maxZ = ((int) Math.floor(locZ + radius) >> 4);

    final World world = location.getWorld();

    for (int x = minX; x <= maxX; x++) {
      for (int z = minZ; z <= maxZ; z++) {
        if (!world.isChunkLoaded(x, z)) {
          continue;
        }

        final Chunk chunk = world.getChunkAt(x, z);

        final net.minecraft.server.v1_8_R3.Chunk nmsChunk = ((CraftChunk) chunk).getHandle();
        final List<net.minecraft.server.v1_8_R3.Entity>[] entitySlices = nmsChunk.entitySlices;

        for (final List<net.minecraft.server.v1_8_R3.Entity> slice : entitySlices) {
          for (final net.minecraft.server.v1_8_R3.Entity nmsEntity : slice) {
            final Entity compareToEntity = nmsEntity.getBukkitEntity();

            if (compareToEntity instanceof LivingEntity) {
              if (compareToEntity instanceof Player || compareToEntity instanceof ArmorStand || compareToEntity
                .hasMetadata("NPC")) {
                continue;
              }

              if (compareToEntity.isDead()) {
                continue;
              }

              if (sm.getType() != compareToEntity.getType()) {
                continue;
              }

              if (sm.getUniqueId().equals(compareToEntity.getUniqueId())) {
                continue;
              }

              final StackedMob compareToStackedMob = STACKED_MOBS.get(compareToEntity.getUniqueId());

              if (compareToStackedMob != null) {
                if (compareToStackedMob.getEntity().isDead()) {
                  continue;
                }

                if (compareToStackedMob.getLocation().distance(location) <= radius) {
                  entities.add(compareToStackedMob);
                }
              }
            }
          }
        }
      }
    }

    return entities;
  }

  public static Map<UUID, StackedMob> getStackedMobs() {
    return STACKED_MOBS;
  }

  public static StackedMob getByEntityId(final UUID uuid) {
    return STACKED_MOBS.get(uuid);
  }

}
