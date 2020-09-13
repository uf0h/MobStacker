package me.ufo.mobstacker;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import net.techcable.tacospigot.event.entity.SpawnerPreSpawnEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
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
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class MobStacker extends JavaPlugin implements Listener {

  private static MobStacker instance;

  public static MobStacker getInstance() {
    return instance;
  }

  /* TODO: FASTUTIL */
  private final static Set<StackedMob> STACKED_MOBS = new HashSet<>();
  private final Map<Location, Long> spawnedTimestamps = new HashMap<>();

  private int SPAWNER_ACTIVATION_DELAY_MS;
  private int SPAWNER_SPAWN_PER;

  @Override
  public void onEnable() {
    instance = this;

    SPAWNER_ACTIVATION_DELAY_MS = 10000;
    SPAWNER_SPAWN_PER = 4;

    for (final World world : this.getServer().getWorlds()) {
      for (final LivingEntity entity : world.getLivingEntities()) {
        if (entity.hasMetadata("STACKED_MOB")) {
          entity.removeMetadata("STACKED_MOB", this);
          entity.remove();
        }
      }
    }

    final PluginManager pm = this.getServer().getPluginManager();
    pm.registerEvents(this, this);

    this.getCommand("ms").setExecutor((sender, label, s, args) -> {

      if ("debug".equalsIgnoreCase(args[0])) {
        spawnedTimestamps.forEach((location, time) -> {
          sender.sendMessage(location.toString() + ": " + location.getBlock().getType().name());
        });
      }

      return false;
    });

    /* TODO: TEST MORE */
    Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
      Bukkit.getLogger().info("MERGING STACKED MOBS (" + STACKED_MOBS.size() + ")");
      final Iterator<StackedMob> mobIterator = STACKED_MOBS.iterator();

      StackedMob compareTo = null;
      while (mobIterator.hasNext()) {
        if (compareTo == null) {
          compareTo = mobIterator.next();
          continue;
        }

        final StackedMob compare = mobIterator.next();

        if (compare.getLocation().distance(compareTo.getLocation()) <= 8) {
          Bukkit.getLogger().info("MERGED");

          final int stacked = compareTo.addAndGet(compare.getStackedAmount());

          final Entity entity = compareTo.getEntity();
          Bukkit.getScheduler().runTask(this, () -> {
            entity.setCustomName(
              ChatColor.GREEN.toString() + "x" + stacked + " " + ChatColor.YELLOW.toString() + entity
                .getType().name()
            );
          });
          compare.destroyEntity();

          mobIterator.remove();
        } else {
          Bukkit.getLogger().info("NOT MERGED");
          compareTo = compare;
        }
      }
    }, 40L, 40L);
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onSpawnerPreSpawnEvent(final SpawnerPreSpawnEvent event) {
    Bukkit.getServer().getLogger().info("SpawnerPre: " + event.getSpawnedType());

    final Location location = event.getLocation();

    if (spawnedTimestamps.containsKey(location)) {
      final long allowedSpawnerSpawnTimestamp = spawnedTimestamps.get(location);
      if (System.currentTimeMillis() < allowedSpawnerSpawnTimestamp) {
        Bukkit.getLogger().info("NOT SPAWNING YET");
        event.setCancelled(true);
      }
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onSpawnerSpawnEvent(final SpawnerSpawnEvent event) {
    event.setCancelled(true);

    Bukkit.getServer().getLogger().info("SpawnerSpawn: " + event.getEntity().getType());

    spawnedTimestamps.put(
      event.getSpawner().getLocation(),
      System.currentTimeMillis() + SPAWNER_ACTIVATION_DELAY_MS
    );

    Bukkit.getServer().getLogger().info("SPAWNING NOW");

    final EntityType type = event.getEntityType();
    final Location location = event.getLocation();
    final Collection<Entity> entities = location.getWorld().getNearbyEntities(location, 8, 4, 8);

    Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
      final int spawns = ThreadLocalRandom.current().nextInt(1, SPAWNER_SPAWN_PER);

      for (final Entity nearbyEntity : entities) {
        if (nearbyEntity == null) {
          continue;
        }

        if (nearbyEntity.isDead()) {
          continue;
        }

        if (nearbyEntity instanceof Player) {
          continue;
        }

        if (!nearbyEntity.hasMetadata("STACKED_MOB")) {
          continue;
        }

        final StackedMob stackedMob = this.getByNearLocation(type, nearbyEntity.getLocation());

        if (stackedMob == null) {
          break;
        }

        final int stacked = stackedMob.addAndGet(spawns);
        Bukkit.getLogger().info("Found stacked mob with new: " + stacked);

        Bukkit.getScheduler().runTask(this, () -> {
          stackedMob.getEntity().setCustomName(
            ChatColor.GREEN.toString() + "x" + stacked + " " + ChatColor.YELLOW.toString() + type.name()
          );
        });
        return;
      }

      Bukkit.getScheduler().runTask(this, () -> {
        Bukkit.getLogger().info("No stack found, creating stack: ");
        final LivingEntity entity = (LivingEntity) location.getWorld().spawnEntity(location, type);
        STACKED_MOBS.add(new StackedMob(entity, spawns));

        entity.setMetadata("STACKED_MOB", new FixedMetadataValue(this, ""));
        entity.setCustomName(
          ChatColor.GREEN.toString() + "x" + spawns + " " + ChatColor.YELLOW.toString() + type.name()
        );
      });
    });
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onEntityDeathEvent(final EntityDeathEvent event) {
    final Entity entity = event.getEntity();
    if (!entity.hasMetadata("STACKED_MOB")) {
      return;
    }

    Bukkit.getLogger()
      .info("Drops before: " + event.getDrops().stream().map(drop -> drop.getType().toString()).collect(
        Collectors.joining()));


    final Iterator<StackedMob> mobIterator = STACKED_MOBS.iterator();
    while (mobIterator.hasNext()) {
      final StackedMob stackedMob = mobIterator.next();
      if (entity.getWorld() != stackedMob.getLocation().getWorld()) {
        continue;
      }

      if (event.getEntityType() != stackedMob.getType()) {
        continue;
      }

      if (entity.getLocation().equals(stackedMob.getLocation())) {
        Bukkit.getLogger().info("is stacked mob");

        if (entity.getLastDamageCause() != null) {
          if (entity.getLastDamageCause().getCause() == EntityDamageEvent.DamageCause.FALL) {
            Bukkit.getLogger().info("fall damage");
            if (event.getDrops() != null) {
              event.getDrops().clear();
            }

            final int stacked = stackedMob.getStackedAmount();
            final int dropAmount = Math.min(stacked, 20);

            event.getDrops().add(new ItemStack(Material.EMERALD, dropAmount));
            event.getDrops().add(new ItemStack(Material.DIAMOND, dropAmount));

            stackedMob.destroyEntity();
            mobIterator.remove();
            return;
          }
        }

        final int decremented = stackedMob.decrementAndGet();
        if (decremented <= 0) {
          for (final ItemStack drop : event.getDrops()) {
            entity.getWorld().dropItemNaturally(entity.getLocation(), drop);
          }

          stackedMob.destroyEntity();
          mobIterator.remove();
          return;
        }

        stackedMob.getEntity().setCustomName(
          ChatColor.GREEN.toString() + "x" + decremented + " " + ChatColor.YELLOW.toString() + entity
            .getType()
            .name()
        );

        for (final ItemStack drop : event.getDrops()) {
          entity.getWorld().dropItemNaturally(entity.getLocation(), drop);
        }
        return;
      }
    }

    for (final ItemStack drop : event.getDrops()) {
      entity.getWorld().dropItemNaturally(entity.getLocation(), drop);
    }

    // kill entities have meta and are not found in Set<StackedMob>
    entity.removeMetadata("STACKED_MOB", this);
    entity.remove();
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onEntityDamageByEntityEvent(final EntityDamageByEntityEvent event) {
    final Entity entity = event.getEntity();
    if (entity instanceof Player) {
      return;
    }

    if (!entity.hasMetadata("STACKED_MOB")) {
      return;
    }

    Bukkit.getServer().getLogger().info("hit");

    event.setCancelled(true);

    if (event.getDamager() instanceof Player) {
      ((Player) event.getDamager()).damage(1.0D, entity);
    }

    Bukkit.getPluginManager()
      .callEvent(new EntityDeathEvent((LivingEntity) entity, Arrays.asList(new ItemStack(Material.EMERALD))));
  }

  public StackedMob getByLocation(final Location location) {
    for (final StackedMob stackedMob : STACKED_MOBS) {
      if (location.equals(stackedMob.getLocation())) {
        return stackedMob;
      }
    }

    return null;
  }

  public StackedMob getByEntity(final LivingEntity entity) {
    for (final StackedMob stackedMob : STACKED_MOBS) {
      if (entity.equals(stackedMob.getEntity())) {
        return stackedMob;
      }
    }

    return null;
  }

  public StackedMob getByNearLocation(final EntityType type, final Location location) {
    final Iterator<StackedMob> mobIterator = STACKED_MOBS.iterator();

    while (mobIterator.hasNext()) {
      final StackedMob stackedMob = mobIterator.next();

      if (type != stackedMob.getType()) {
        continue;
      }

      if (location.getWorld() != stackedMob.getLocation().getWorld()) {
        continue;
      }

      if (location.distance(stackedMob.getLocation()) <= 8) {
        return stackedMob;
      }
    }

    return null;
  }

}
