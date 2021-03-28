package me.ufo.mobstacker.listeners;

import me.ufo.mobstacker.Config;
import me.ufo.mobstacker.MSPlugin;
import me.ufo.mobstacker.events.StackedMobDeathEvent;
import me.ufo.mobstacker.mob.StackedMob;
import me.ufo.mobstacker.mob.StackedMobDeathCause;
import me.ufo.mobstacker.mob.StackedMobDrops;
import me.ufo.shaded.it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import me.ufo.shaded.org.apache.commons.math3.util.FastMath;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

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

    final EntityDamageEvent.DamageCause cause = event.getCause();

    if (cause == EntityDamageEvent.DamageCause.FALL) {
      event.setCancelled(true);

      if (entity.getType() != EntityType.IRON_GOLEM) {
        if (event.getDamage() < ((LivingEntity) entity).getHealth()) {
          return;
        }
      }

      final StackedMob sm = StackedMob.getByEntityId(entity.getUniqueId());

      if (sm == null) {
        entity.removeMetadata(StackedMob.METADATA_KEY, plugin);
        entity.remove();
        return;
      }

      final StackedMobDeathEvent out = new StackedMobDeathEvent(sm, StackedMobDeathCause.FALL,
          FastMath.min(sm.getStackedAmount(), Config.MAX_DEATHS),
          null);

//      for (final Player player : Bukkit.getOnlinePlayers()) {
//        player.sendMessage("Death of " + entity.getType() + " by FALL (" + out.getAmountDied() + ")");
//      }

      Bukkit.getPluginManager().callEvent(out);

      if (out.isCancelled()) {
        return;
      }

      sm.destroyEntity();
      StackedMob.getStackedMobs().remove(sm.getUniqueId());

      final List<ItemStack> drops = out.getDrops();

      if (drops != null && !drops.isEmpty()) {
        final Location location = out.getLocation();

        for (final ItemStack item : drops) {
          location.getWorld().dropItem(location, item);
        }
      }

    /*} else if (cause == EntityDamageEvent.DamageCause.LAVA ||
               cause == EntityDamageEvent.DamageCause.FIRE ||
               cause == EntityDamageEvent.DamageCause.FIRE_TICK) {


    } else {
      event.setCancelled(true);
    }*/
    } else if (cause != EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
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

      if (hitTimestamps != null) {
        hitTimestamps.put(player.getUniqueId(), System.currentTimeMillis() + Config.HIT_DELAY);
      }

      final StackedMobDeathEvent out = new StackedMobDeathEvent(sm, StackedMobDeathCause.PLAYER, 1, player);
      Bukkit.getPluginManager().callEvent(out);

      if (out.isCancelled()) {
        return;
      }

      final int decremented = sm.decrementAndGet();

      if (decremented <= 0) {
        sm.destroyEntity();

        StackedMob.getStackedMobs().remove(sm.getUniqueId());
        return;
      }

      sm.setCustomName();

      final List<ItemStack> drops = out.getDrops();

      if (drops != null && !drops.isEmpty()) {
        final Location location = out.getLocation();

        for (final ItemStack item : drops) {
          location.getWorld().dropItem(location, item);
        }
      }

      final int xp = out.getXp();
      if (xp <= 0) {
        return;
      }

      final Player p = out.getPlayer();
      if (player == null) {
        return;
      }

      // TODO: NOTE remove
      p.giveExp(player.hasPermission("vicious.booster.doubleexp") ? xp * 2 : xp);
    }
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onEntityDeathEvent(final EntityDeathEvent event) {
    event.getDrops().clear();

    final StackedMobDrops type = StackedMobDrops.getFromEntity(event.getEntityType());

    if (type == null) {
      return;
    }

    event.getDrops().addAll(type.getDrops());
    event.setDroppedExp(0);
  }

}
