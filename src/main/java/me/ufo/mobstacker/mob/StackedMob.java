package me.ufo.mobstacker.mob;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import me.ufo.mobstacker.MSPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

public final class StackedMob {

  private final static Map<UUID, StackedMob> STACKED_MOBS = new ConcurrentHashMap<>();

  private final Entity entity;
  private final EntityType entityType;
  private final UUID uniqueId;
  private final AtomicInteger stacked;

  public StackedMob(final Entity entity) {
    this(entity, 1);
  }

  public StackedMob(final Entity entity, final int stacked) {
    this.entity = entity;
    this.entityType = entity.getType();
    this.uniqueId = entity.getUniqueId();
    this.stacked = new AtomicInteger(stacked);
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
    return stacked.addAndGet(increment);
  }

  public int decrementAndGet() {
    return stacked.decrementAndGet();
  }

  public StackedMob destroyEntity() {
    if (entity != null && !entity.isDead()) {
      this.entity.removeMetadata("STACKED_MOBS", MSPlugin.getInstance());
      this.entity.remove();
    }
    return this;
  }

  public static Map<UUID, StackedMob> getStackedMobs() {
    return STACKED_MOBS;
  }

  public static StackedMob getByEntityId(final UUID uuid) {
    return STACKED_MOBS.get(uuid);
  }

  public static StackedMob getFirstByDistance(final Entity entity, final int distance) {
    for (final Map.Entry<UUID, StackedMob> entry : STACKED_MOBS.entrySet()) {
      final StackedMob stackedMob = entry.getValue();

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
  }

  public static StackedMob getFirstByDistance(final StackedMob stackedMob, final int distance) {
    for (final Map.Entry<UUID, StackedMob> entry : STACKED_MOBS.entrySet()) {
      final StackedMob other = entry.getValue();

      if (stackedMob.getUniqueId().equals(other.getUniqueId())) {
        continue;
      }

      if (other.getEntityType() != stackedMob.getEntityType()) {
        continue;
      }

      if (other.getLocation().getWorld() != stackedMob.getLocation().getWorld()) {
        continue;
      }

      if (other.getLocation().distance(stackedMob.getLocation()) <= distance) {
        return other;
      }
    }

    return null;
  }

  public static List<StackedMob> getAllByDistance(final StackedMob stackedMob, final int distance) {
    final List<StackedMob> out = new ArrayList<>();
    for (final Map.Entry<UUID, StackedMob> entry : STACKED_MOBS.entrySet()) {
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
  }

}
