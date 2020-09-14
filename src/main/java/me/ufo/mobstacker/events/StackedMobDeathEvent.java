package me.ufo.mobstacker.events;

import java.util.ArrayList;
import java.util.List;
import me.ufo.mobstacker.mob.StackedMob;
import me.ufo.mobstacker.mob.StackedMobDeathCause;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

public final class StackedMobDeathEvent extends Event implements Cancellable {

  private static final HandlerList handlers = new HandlerList();

  private final StackedMob stackedMob;
  private final EntityType entityType;
  private final Location location;
  private final int died;

  private final StackedMobDeathCause cause;
  private final Player player;
  private final List<ItemStack> drops;

  private boolean cancelled;

  public StackedMobDeathEvent(final StackedMob stackedMob, final StackedMobDeathCause cause) {
    this.stackedMob = stackedMob;
    this.entityType = stackedMob.getEntityType();
    this.location = stackedMob.getLocation();
    this.cause = cause;
    this.died = 1;
    this.player = null;
    this.drops = new ArrayList<>();
  }

  public StackedMobDeathEvent(final StackedMob stackedMob, final StackedMobDeathCause cause, final Player player) {
    this.stackedMob = stackedMob;
    this.entityType = stackedMob.getEntityType();
    this.location = stackedMob.getLocation();
    this.cause = cause;
    this.player = player;
    this.died = 1;
    this.drops = new ArrayList<>();
  }

  public StackedMobDeathEvent(final StackedMob stackedMob, final int died, final StackedMobDeathCause cause) {
    this.stackedMob = stackedMob;
    this.entityType = stackedMob.getEntityType();
    this.location = stackedMob.getLocation();
    this.died = died;
    this.cause = cause;
    this.player = null;
    this.drops = new ArrayList<>();
  }

  public StackedMob getStackedMob() {
    return stackedMob;
  }

  public int getAmountDied() {
    return died;
  }

  public EntityType getEntityType() {
    return entityType;
  }

  public StackedMobDeathCause getCause() {
    return cause;
  }

  public Player getPlayer() {
    return player;
  }

  public Location getLocation() {
    return location;
  }

  public List<ItemStack> getDrops() {
    return drops;
  }

  @Override
  public boolean isCancelled() {
    return cancelled;
  }

  @Override
  public void setCancelled(final boolean cancelled) {
    this.cancelled = cancelled;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }

}
