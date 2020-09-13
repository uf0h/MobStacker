package me.ufo.mobstacker;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

public final class StackedMob {

  private final LivingEntity entity;
  private final UUID uuid;
  private final AtomicInteger stacked;

  public StackedMob(final LivingEntity entity) {
    this.entity = entity;
    this.uuid = entity.getUniqueId();
    this.stacked = new AtomicInteger(1);
  }

  public StackedMob(final LivingEntity entity, final int stacked) {
    this.entity = entity;
    this.uuid = entity.getUniqueId();
    this.stacked = new AtomicInteger(stacked);
  }

  public LivingEntity getEntity() {
    return entity;
  }

  public EntityType getType() {
    return entity.getType();
  }

  public Location getLocation() {
    return entity.getLocation();
  }

  public UUID getUUID() {
    return uuid;
  }

  public int getStackedAmount() {
    return stacked.get();
  }

  public int getAndIncrement() {
    return stacked.getAndIncrement();
  }

  public int incrementAndGet() {
    return stacked.incrementAndGet();
  }

  public int getAndAdd(final int increment) {
    return stacked.getAndAdd(increment);
  }

  public int addAndGet(final int increment) {
    return stacked.addAndGet(increment);
  }

  public int getAndDecrement() {
    return stacked.getAndDecrement();
  }

  public int decrementAndGet() {
    return stacked.decrementAndGet();
  }

  public StackedMob destroyEntity() {
    this.entity.removeMetadata("STACKED_MOBS", MobStacker.getInstance());
    this.entity.remove();
    return this;
  }

}
