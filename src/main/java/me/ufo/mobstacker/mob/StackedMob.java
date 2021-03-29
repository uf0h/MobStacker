package me.ufo.mobstacker.mob;

import me.ufo.mobstacker.Config;
import me.ufo.mobstacker.MSPlugin;
import me.ufo.shaded.com.google.common.base.MoreObjects;
import me.ufo.shaded.it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.ufo.shaded.it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import me.ufo.shaded.org.apache.commons.math3.util.FastMath;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.CraftChunk;
import org.bukkit.entity.*;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public final class StackedMob {

  private static final MSPlugin plugin = MSPlugin.get();

  private static final Object2ObjectOpenHashMap<UUID, StackedMob> STACKED_MOBS = new Object2ObjectOpenHashMap<>(1000);

  public static final String METADATA_KEY = "STACKED_MOB";
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

    final LivingEntity livingEntity = (LivingEntity) this.entity;
    livingEntity.setHealth(1.0D);
    livingEntity.setMaxHealth(1.0D);
    livingEntity.setRemoveWhenFarAway(false);
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
    this.entity.setCustomName(Config.getStackedMobName(stacked.get(), entity));
  }

  public void destroyEntity() {
    entity.removeMetadata(METADATA_KEY, plugin);
    entity.remove();
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("entity", entity)
        .add("type", type)
        .add("uniqueId", uniqueId)
        .add("stacked", stacked)
        .toString();
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

    return type == that.type && uniqueId.equals(that.uniqueId) && entity.equals(that.entity);
  }

  @Override
  public int hashCode() {
    return Objects.hash(entity, type, uniqueId);
  }

  public static ObjectOpenHashSet<StackedMob> getAllByChunk(final Chunk chunk) {
    final ObjectOpenHashSet<StackedMob> entities = new ObjectOpenHashSet<>();

    final net.minecraft.server.v1_8_R3.Chunk nmsChunk = ((CraftChunk) chunk).getHandle();
    final List<net.minecraft.server.v1_8_R3.Entity>[] entitySlices = nmsChunk.entitySlices;

    for (final List<net.minecraft.server.v1_8_R3.Entity> slice : entitySlices) {
      for (final net.minecraft.server.v1_8_R3.Entity nmsEntity : slice) {
        final Entity entity = nmsEntity.getBukkitEntity();

        if (entity instanceof LivingEntity) {
          if (entity instanceof Player || entity instanceof ArmorStand || entity.hasMetadata("NPC")) {
            continue;
          }

          if (entity.isDead()) {
            continue;
          }

          final StackedMob sm = STACKED_MOBS.get(entity.getUniqueId());

          if (sm != null) {
            if (sm.getEntity().isDead()) {
              sm.destroyEntity();
              STACKED_MOBS.remove(entity.getUniqueId());
              continue;
            }

            entities.add(sm);
          }
        }
      }
    }

    return entities;
  }

  /* TODO: decide whether to use NMS or World#nearbyEntities */

  private static final double RADIUS_XZ = 16;
  private static final double RADIUS_Y = 32;

  /*public static StackedMob getFirstByRadius(final EntityType entityType, final Location location) {
    return getFirstByRadius(entityType, location, RADIUS_XZ, RADIUS_Y, RADIUS_XZ);
  }

  public static StackedMob getFirstByRadius(final EntityType entityType, final Location location,
                                            final double x, final double y, final double z) {

    for (final Entity entity : location.getWorld().getNearbyEntities(location, x, y, z)) {
      if (entity.getType() != entityType) {
        continue;
      }

      if (entity.hasMetadata("NPC")) {
        continue;
      }

      final StackedMob sm = STACKED_MOBS.get(entity.getUniqueId());

      if (sm != null) {
        return sm;
      }
    }

    return null;
  }*/

  public static StackedMob getFirstByDistance(final EntityType entityType, final Location location) {
    return _getFirstByDistance(entityType, location, RADIUS_XZ, RADIUS_Y);
  }

  public static StackedMob _getFirstByDistance(final EntityType entityType, final Location location,
                                               final double horizontalRadius, final double verticalRadius) {

    final double locX = location.getX();
    final double locZ = location.getZ();

    final int minX = ((int) FastMath.floor(locX - horizontalRadius) >> 4);
    final int maxX = ((int) FastMath.floor(locX + horizontalRadius) >> 4);
    final int minZ = ((int) FastMath.floor(locZ - horizontalRadius) >> 4);
    final int maxZ = ((int) FastMath.floor(locZ + horizontalRadius) >> 4);

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
            final Entity entity = nmsEntity.getBukkitEntity();

            if (entity.getType() != entityType || entity.isDead()) {
              continue;
            }

            final StackedMob sm = STACKED_MOBS.get(entity.getUniqueId());

            if (sm != null) {
//              if (FastMath.abs(entity.getLocation().getY() - verticalRadius) <= verticalRadius) {
//                return sm;
//              }
              if (sm.getLocation().distanceSquared(location) <= (verticalRadius * 2)) {
                return sm;
              }
            }
          }
        }
      }
    }

    return null;
  }

  public static ObjectOpenHashSet<StackedMob> getAllByRadius(final StackedMob sm) {
    return getAllByRadius(sm, RADIUS_XZ, RADIUS_Y);
  }

  public static ObjectOpenHashSet<StackedMob> getAllByRadius(final StackedMob sm, final double horizontalRadius,
                                                             final double verticalRadius) {

    final ObjectOpenHashSet<StackedMob> entities = new ObjectOpenHashSet<>();

    final Location location = sm.getLocation();
    final double locX = location.getX();
    final double locZ = location.getZ();

    final int minX = ((int) FastMath.floor(locX - horizontalRadius) >> 4);
    final int maxX = ((int) FastMath.floor(locX + horizontalRadius) >> 4);
    final int minZ = ((int) FastMath.floor(locZ - horizontalRadius) >> 4);
    final int maxZ = ((int) FastMath.floor(locZ + horizontalRadius) >> 4);

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
              if (compareToEntity instanceof Player ||
                  compareToEntity instanceof ArmorStand || compareToEntity.hasMetadata("NPC")) {
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
                if (compareToStackedMob.getLocation().distanceSquared(location) <= (verticalRadius * 2)) {
                  entities.add(compareToStackedMob);
                }
//                final Location smLocation = sm.getLocation();
//
//                if (FastMath.abs(smLocation.getY() - verticalRadius) <= verticalRadius &&
//                    FastMath.abs(smLocation.getX() - horizontalRadius) <= horizontalRadius &&
//                    FastMath.abs(smLocation.getZ() - horizontalRadius) <= horizontalRadius) {
//                  entities.add(compareToStackedMob);
//                }
              }
            }
          }
        }
      }
    }

    return entities;
  }

  /*public static StackedMob getFirstByDistance(final EntityType entityType, final Location location, final double radius) {
      final double locX = location.getX();
      final double locZ = location.getZ();

      final int minX = ((int) FastMath.floor(locX - radius) >> 4);
      final int maxX = ((int) FastMath.floor(locX + radius) >> 4);
      final int minZ = ((int) FastMath.floor(locZ - radius) >> 4);
      final int maxZ = ((int) FastMath.floor(locZ + radius) >> 4);

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
              final Entity entity = nmsEntity.getBukkitEntity();

              if (entity instanceof LivingEntity) {
                if (entity instanceof Player || entity instanceof ArmorStand || entity.hasMetadata("NPC")) {
                  continue;
                }

                if (entity.isDead()) {
                  continue;
                }

                if (entityType != entity.getType()) {
                  continue;
                }

                final StackedMob sm = STACKED_MOBS.get(entity.getUniqueId());

                if (sm != null) {
                  if (sm.getEntity().isDead()) {
                    sm.destroyEntity();
                    STACKED_MOBS.remove(entity.getUniqueId());
                    continue;
                  }

                  if (sm.getLocation().distanceSquared(location) <= radius) {
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

    public static StackedMob getFirstByDistance(final Entity entity, final double radius) {
      final Location location = entity.getLocation();
      final double locX = location.getX();
      final double locZ = location.getZ();

      final int minX = ((int) FastMath.floor(locX - radius) >> 4);
      final int maxX = ((int) FastMath.floor(locX + radius) >> 4);
      final int minZ = ((int) FastMath.floor(locZ - radius) >> 4);
      final int maxZ = ((int) FastMath.floor(locZ + radius) >> 4);

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
                if (other instanceof Player || other instanceof ArmorStand ||
                        other.hasMetadata("NPC")) {
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
                    sm.destroyEntity();
                    STACKED_MOBS.remove(other.getUniqueId());
                    continue;
                  }

                  if (sm.getLocation().distanceSquared(location) <= radius) {
                    return sm;
                  }
                }
              }
            }
          }
        }
      }

      return null;
    }*/

  public static Object2ObjectOpenHashMap<UUID, StackedMob> getStackedMobs() {
    return STACKED_MOBS;
  }

  public static StackedMob getByEntityId(final UUID uuid) {
    return STACKED_MOBS.get(uuid);
  }

}
