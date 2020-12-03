package me.ufo.mobstacker.listeners;

import java.util.UUID;
import me.ufo.mobstacker.Config;
import me.ufo.mobstacker.MSPlugin;
import me.ufo.mobstacker.events.StackedMobDeathEvent;
import me.ufo.mobstacker.mob.StackedMob;
import me.ufo.mobstacker.mob.StackedMobDeathCause;
import me.ufo.shaded.it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import me.ufo.shaded.org.apache.commons.math3.util.FastMath;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

public final class EntityListener implements Listener {

  private final MSPlugin plugin;

  public EntityListener(final MSPlugin plugin) {
    this.plugin = plugin;
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onExplosionPrimeEvent(final EntityExplodeEvent event) {
    final Entity entity = event.getEntity();
    if (entity.getType() != EntityType.CREEPER) {
      return;
    }

    if (!entity.hasMetadata(StackedMob.METADATA_KEY)) {
      return;
    }

    final StackedMob sm = StackedMob.getByEntityId(entity.getUniqueId());

    if (sm == null) {
      return;
    }

    entity.removeMetadata(StackedMob.METADATA_KEY, plugin);
    StackedMob.getStackedMobs().remove(sm.getUniqueId());
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onEntityDamageEvent(final EntityDamageEvent event) {
    final Entity entity = event.getEntity();
    if (!(entity instanceof LivingEntity) || entity instanceof Player) {
      return;
    }

    if (!entity.hasMetadata(StackedMob.METADATA_KEY)) {
      return;
    }

    if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
      event.setCancelled(true);

      if (event.getDamage() < ((LivingEntity) entity).getHealth()) {
        return;
      }

      final StackedMob sm = StackedMob.getByEntityId(entity.getUniqueId());

      if (sm == null) {
        entity.removeMetadata(StackedMob.METADATA_KEY, plugin);
        entity.remove();
        return;
      }

      Bukkit.getPluginManager().callEvent(
        new StackedMobDeathEvent(
          sm,
          FastMath.min(sm.getStackedAmount(), Config.MAX_DEATHS),
          StackedMobDeathCause.FALL
        )
      );
    } else if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onEntityDamageByEntityEvent(final EntityDamageByEntityEvent event) {
    final Entity entity = event.getEntity();
    if (entity instanceof Player) {
      return;
    }

    if (!entity.hasMetadata(StackedMob.METADATA_KEY)) {
      return;
    }

    //Bukkit.getServer().getLogger().info("hit");

    event.setCancelled(true);

    if (event.getDamager() instanceof Player) {
      final Player player = (Player) event.getDamager();

      final Object2LongOpenHashMap<UUID> hitTimestamps = plugin.getHitTimestamps();

      if (hitTimestamps != null) {
        final long allowedHitTimestamp = hitTimestamps.getOrDefault(player.getUniqueId(), -1L);
        if (allowedHitTimestamp != -1 && System.currentTimeMillis() < allowedHitTimestamp) {
          return;
        }
      }

      final StackedMob sm = StackedMob.getByEntityId(entity.getUniqueId());

      if (sm == null) {
        entity.removeMetadata(StackedMob.METADATA_KEY, plugin);
        entity.remove();
        return;
      }

      Bukkit.getPluginManager().callEvent(
        new StackedMobDeathEvent(
          sm,
          StackedMobDeathCause.PLAYER,
          player
        )
      );

      if (hitTimestamps != null) {
        hitTimestamps.put(player.getUniqueId(), System.currentTimeMillis() + Config.HIT_DELAY);
      }
    }
  }

}
