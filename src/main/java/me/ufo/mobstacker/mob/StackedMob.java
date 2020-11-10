package me.ufo.mobstacker.mob;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import me.ufo.mobstacker.MSPlugin;
import me.ufo.shaded.it.unimi.dsi.fastutil.objects.ObjectIterator;
import me.ufo.shaded.it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

public final class StackedMob {

  private static final Map<UUID, StackedMob> STACKED_MOBS = new ConcurrentHashMap<>(1000);

  public static final String METADATA_KEY = "STACKED_MOB";
  private static final FixedMetadataValue METADATA_VALUE =
    new FixedMetadataValue(MSPlugin.getInstance(), true);

  private final Entity entity;
  private final EntityType entityType;
  private final UUID uniqueId;
  private final AtomicInteger stacked;

  public StackedMob(final Entity entity) {
    this(entity, 1);
  }

  public StackedMob(final Entity entity, final UUID uniqueId, final int stacked) {
    this.entity = entity;
    this.entityType = entity.getType();
    this.uniqueId = uniqueId;
    this.stacked = new AtomicInteger(stacked);

    this.entity.setMetadata(METADATA_KEY, METADATA_VALUE);
    this.setCustomName();
  }

  public StackedMob(final Entity entity, final int stacked) {
    this(entity, entity.getUniqueId(), stacked);
  }

  public Entity getEntity() {
    return entity;
  }

  public EntityType getEntityType() {
    return entityType;
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
      Bukkit.getScheduler().runTask(MSPlugin.getInstance(), () -> {
        this.entity.setCustomName(MSPlugin.getInstance().getStackedMobName(stacked.get(), entity));
      });
    } else {
      this.entity.setCustomName(MSPlugin.getInstance().getStackedMobName(stacked.get(), entity));
    }
  }

  public StackedMob destroyEntity() {
    if (!entity.isDead()) {
      this.entity.removeMetadata(METADATA_KEY, MSPlugin.getInstance());
      this.entity.remove();
    }
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

  public static StackedMob getFirstByDistance(final Entity entity, final double radius) {
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
                if (entity instanceof Player || entity instanceof ArmorStand || entity.hasMetadata("NPC")) {
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
            if (MSPlugin.getInstance().isDebug()) {
              MSPlugin.getInstance().getLogger()
                .info("NON-FATAL ERROR (IGNORE): [StackedMob L208] " + throwable.getClass().getSimpleName());
            }
          }
        }
      }
    }

    return null;
  }

  public static ObjectOpenHashSet<StackedMob> getAllByDistance(final StackedMob stackedMob,
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
                if (entity instanceof Player || entity instanceof ArmorStand || entity.hasMetadata("NPC")) {
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
            if (MSPlugin.getInstance().isDebug()) {
              MSPlugin.getInstance().getLogger()
                .info("NON-FATAL ERROR (IGNORE): [StackedMob L269] " + throwable.getClass().getSimpleName());
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
  }

  public static Map<UUID, StackedMob> getStackedMobs() {
    return STACKED_MOBS;
  }

  public static StackedMob getByEntityId(final UUID uuid) {
    return STACKED_MOBS.get(uuid);
  }

  @Override
  public String toString() {
    return ChatColor.YELLOW.toString() + "StackedMob{" +
           "entityIsDead=" + (entity.isDead()) +
           ", location=" + entity.getLocation() +
           ", entityType=" + entityType +
           ", uniqueId=" + uniqueId +
           ", stacked=" + stacked +
           '}';
  }

}
