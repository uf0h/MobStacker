package me.ufo.mobstacker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import me.ufo.mobstacker.commands.MobStackerCommand;
import me.ufo.mobstacker.events.StackedMobDeathEvent;
import me.ufo.mobstacker.mob.StackedMob;
import me.ufo.mobstacker.mob.StackedMobDeathCause;
import me.ufo.mobstacker.tasks.ClearTask;
import me.ufo.mobstacker.tasks.MergeTask;
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
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

public final class MSPlugin extends JavaPlugin implements Listener {

  private static MSPlugin instance;

  public static MSPlugin getInstance() {
    return instance;
  }

  private final Map<Location, Long> spawnedTimestamps = new HashMap<>();

  private int spawnerActivationDelay;
  private int spawnerSpawnPer;
  private int stackedMobMaxDeath;

  private BukkitScheduler scheduler;

  private BukkitTask mergeTask;
  private BukkitTask clearTask;

  @Override
  public void onEnable() {
    instance = this;

    spawnerActivationDelay = 40000;
    spawnerSpawnPer = 4;
    stackedMobMaxDeath = 100;

    scheduler = this.getServer().getScheduler();

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

    this.getCommand("stacker").setExecutor(new MobStackerCommand());

    this.mergeTask = scheduler.runTaskTimerAsynchronously(this, new MergeTask(this), 0L, 100L);
    this.clearTask = scheduler.runTaskTimer(this, new ClearTask(this), 6000L, 6000L);
  }

  @Override
  public void onDisable() {
    this.mergeTask.cancel();
    this.clearTask.cancel();

    for (final World world : this.getServer().getWorlds()) {
      for (final LivingEntity entity : world.getLivingEntities()) {
        if (entity.hasMetadata("STACKED_MOB")) {
          entity.removeMetadata("STACKED_MOB", this);
          entity.remove();
        }
      }
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onSpawnerPreSpawnEvent(final SpawnerPreSpawnEvent event) {
    final Location location = event.getLocation();

    if (spawnedTimestamps.containsKey(location)) {
      final long allowedSpawnerSpawnTimestamp = spawnedTimestamps.get(location);
      if (System.currentTimeMillis() < allowedSpawnerSpawnTimestamp) {
        event.setCancelled(true);
      }
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onSpawnerSpawnEvent(final SpawnerSpawnEvent event) {
    event.setCancelled(true);

    spawnedTimestamps.put(
      event.getSpawner().getLocation(),
      System.currentTimeMillis() + spawnerActivationDelay
    );

    final Entity entity = event.getEntity();
    final Location location = event.getLocation();

    scheduler.runTaskAsynchronously(this, () -> {
      final StackedMob stackedMob = StackedMob.getFirstByDistance(event.getEntity(), 4);

      if (stackedMob == null) {
        final int spawns = ThreadLocalRandom.current().nextInt(1, spawnerSpawnPer);
        Bukkit.getScheduler().runTask(this, () -> {
          //Bukkit.getLogger().info("No stack found, creating stack: ");

          final Entity spawnedEntity = location.getWorld().spawnEntity(location, entity.getType());

          StackedMob.getStackedMobs().put(spawnedEntity.getUniqueId(), new StackedMob(spawnedEntity, spawns));

          spawnedEntity.setMetadata("STACKED_MOB", new FixedMetadataValue(this, ""));
          spawnedEntity.setCustomName(
            ChatColor.GOLD.toString() + ChatColor.BOLD.toString() + "x" + spawns + " " +
            ChatColor.YELLOW.toString() + ChatColor.BOLD.toString() + entity.getType().name()
          );
        });
        return;
      }

      final int spawns = ThreadLocalRandom.current().nextInt(1, spawnerSpawnPer);
      final int stacked = stackedMob.addAndGet(spawns);
      //Bukkit.getLogger().info("Found stacked mob with new: " + stacked);

      scheduler.runTask(this, () -> {
        stackedMob.getEntity().setCustomName(
          ChatColor.GOLD.toString() + ChatColor.BOLD.toString() + "x" + stacked + " " +
          ChatColor.YELLOW.toString() + ChatColor.BOLD.toString() + entity.getType().name()
        );
      });
    });
  }

  /*@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onEntityDeathEvent(final EntityDeathEvent event) {
    final Entity entity = event.getEntity();
    if (!entity.hasMetadata("STACKED_MOB")) {
      return;
    }

    final StackedMob stackedMob = StackedMob.getByEntityId(entity.getUniqueId());

    // if entity has metadata but it's not in collection
    if (stackedMob == null) {
      for (final ItemStack drop : event.getDrops()) {
        entity.getWorld().dropItem(entity.getLocation(), drop);
      }

      // kill entities have meta and are not found in Set<StackedMob>
      entity.removeMetadata("STACKED_MOB", this);
      entity.remove();
      return;
    }

    if (entity.getLastDamageCause() != null) {
      if (entity.getLastDamageCause().getCause() == EntityDamageEvent.DamageCause.FALL) {
        Bukkit.getLogger().info("fall damage");
        if (event.getDrops() != null) {
          event.getDrops().clear();
        }

        final int stacked = stackedMob.getStackedAmount();
        final int dropAmount = Math.min(stacked, 100);

        event.getDrops().add(new ItemStack(Material.EMERALD, dropAmount));
        event.getDrops().add(new ItemStack(Material.DIAMOND, dropAmount));

        stackedMob.destroyEntity();

        StackedMob.getStackedMobs().remove(entity.getUniqueId());
        return;
      }
    }

    final int decremented = stackedMob.decrementAndGet();
    if (decremented <= 0) {
      for (final ItemStack drop : event.getDrops()) {
        entity.getWorld().dropItem(entity.getLocation(), drop);
      }

      stackedMob.destroyEntity();

      StackedMob.getStackedMobs().remove(entity.getUniqueId());
      return;
    }

    stackedMob.getEntity().setCustomName(
      ChatColor.GOLD.toString() + ChatColor.BOLD.toString + "x" + decremented + " " + ChatColor.YELLOW.toString() + entity
        .getType()
        .name()
    );

    for (final ItemStack drop : event.getDrops()) {
      entity.getWorld().dropItem(entity.getLocation(), drop);
    }
  }*/

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onEntityDamageEvent(final EntityDamageEvent event) {
    final Entity entity = event.getEntity();
    if (entity instanceof Player) {
      return;
    }

    if (!entity.hasMetadata("STACKED_MOB")) {
      return;
    }

    if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
      final StackedMob stackedMob = StackedMob.getByEntityId(entity.getUniqueId());

      if (stackedMob == null) {
        entity.removeMetadata("STACKED_MOB", this);
        entity.remove();
        return;
      }

      Bukkit.getPluginManager().callEvent(
        new StackedMobDeathEvent(
          stackedMob,
          Math.min(stackedMob.getStackedAmount(), stackedMobMaxDeath),
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

    if (!entity.hasMetadata("STACKED_MOB")) {
      return;
    }

    //Bukkit.getServer().getLogger().info("hit");

    event.setCancelled(true);

    if (event.getDamager() instanceof Player) {
      final StackedMob stackedMob = StackedMob.getByEntityId(entity.getUniqueId());

      if (stackedMob == null) {
        entity.removeMetadata("STACKED_MOB", this);
        entity.remove();
        return;
      }

      Bukkit.getPluginManager().callEvent(
        new StackedMobDeathEvent(
          stackedMob,
          StackedMobDeathCause.PLAYER,
          (Player) event.getDamager()
        )
      );
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onStackedMobDeathEvent(final StackedMobDeathEvent event) {
    if (event.getCause() == StackedMobDeathCause.FALL) {
      final StackedMob stackedMob = event.getStackedMob();
      final int dropAmount = Math.min(event.getAmountDied(), stackedMobMaxDeath);

      stackedMob.destroyEntity();
      for (final ItemStack item : getDropsForEntity(event.getEntityType())) {
        item.setAmount(item.getAmount() * dropAmount);
        event.getDrops().add(item);
      }
      StackedMob.getStackedMobs().remove(stackedMob.getUniqueId());
    } else if (event.getCause() == StackedMobDeathCause.PLAYER) {
      final StackedMob stackedMob = event.getStackedMob();
      final int decremented = stackedMob.decrementAndGet();

      if (decremented <= 0) {
        stackedMob.destroyEntity();
        for (final ItemStack item : getDropsForEntity(stackedMob.getEntityType())) {
          event.getDrops().add(item);
        }
        StackedMob.getStackedMobs().remove(stackedMob.getUniqueId());
        return;
      }

      stackedMob.getEntity().setCustomName(
        ChatColor.GOLD.toString() + ChatColor.BOLD.toString() + "x" + decremented + " " +
        ChatColor.YELLOW.toString() + ChatColor.BOLD.toString()  + stackedMob.getEntityType().name()
      );

      for (final ItemStack item : getDropsForEntity(stackedMob.getEntityType())) {
        event.getDrops().add(item);
      }
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onStackedMobDeathEventItemsDrop(final StackedMobDeathEvent event) {
    final Location location = event.getLocation();

    for (final ItemStack item : event.getDrops()) {
      location.getWorld().dropItem(location, item);
    }
  }

  private static List<ItemStack> getDropsForEntity(final EntityType type) {
    switch (type) {
      default:
        return new ArrayList<>(0);

      case ZOMBIE: {
        return Arrays.asList(
          new ItemStack(Material.ROTTEN_FLESH, ThreadLocalRandom.current().nextInt(1, 10))
        );
      }

      case SKELETON: {
        return Arrays.asList(
          new ItemStack(Material.BONE, ThreadLocalRandom.current().nextInt(1, 10))
        );
      }

      case CREEPER: {
        return Arrays.asList(new ItemStack(Material.TNT, 1));
      }

      case PIG_ZOMBIE: {
        return Arrays.asList(
          new ItemStack(Material.GOLD_INGOT, ThreadLocalRandom.current().nextInt(1, 5))
        );
      }

      case BLAZE: {
        return Arrays.asList(
          new ItemStack(Material.BLAZE_ROD, ThreadLocalRandom.current().nextInt(1, 5))
        );
      }

      case IRON_GOLEM: {
        return Arrays.asList(new ItemStack(Material.IRON_INGOT, 1));
      }

      case VILLAGER: {
        return Arrays.asList(new ItemStack(Material.EMERALD, 1));
      }

      case ENDERMAN: {
        return Arrays.asList(new ItemStack(Material.ENDER_PEARL));
      }
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onInventoryOpenEvent(final InventoryOpenEvent event) {
    if (event.getInventory().getType() == InventoryType.MERCHANT) {
      event.setCancelled(true);
    }
  }

  public BukkitScheduler getScheduler() {
    return scheduler;
  }

}
