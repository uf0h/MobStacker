package me.ufo.mobstacker;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import me.ufo.mobstacker.commands.MobStackerCommand;
import me.ufo.mobstacker.events.StackedMobDeathEvent;
import me.ufo.mobstacker.mob.StackedMob;
import me.ufo.mobstacker.mob.StackedMobDeathCause;
import me.ufo.mobstacker.mob.StackedMobDrops;
import me.ufo.mobstacker.tasks.ClearTask;
import me.ufo.mobstacker.tasks.MergeTask;
import net.techcable.tacospigot.event.entity.SpawnerPreSpawnEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;
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
  private final Map<UUID, Long> hitTimestamps = new HashMap<>();

  /* Delay before SpawnerSpawnEvent */
  private int spawnerActivationDelay;
  /* Spawns per SpawnerSpawnEvent */
  private int spawnerSpawnPerMin;
  private int spawnerSpawnPerMax;
  /* Max item drop amount for stack on fall damage */
  private int stackedMobMaxDeath;
  /* Hit delay */
  private int stackedMobHitDelay;

  private BukkitScheduler scheduler;

  private BukkitTask mergeTask;
  private BukkitTask clearTask;

  @Override
  public void onEnable() {
    instance = this;

    spawnerActivationDelay = 20000;
    spawnerSpawnPerMin = 2;
    spawnerSpawnPerMax = 5;
    stackedMobMaxDeath = 100;
    stackedMobHitDelay = 250;

    scheduler = this.getServer().getScheduler();

    for (final World world : this.getServer().getWorlds()) {
      for (final LivingEntity entity : world.getLivingEntities()) {
        if (entity.hasMetadata("STACKED_MOB")) {
          entity.removeMetadata("STACKED_MOB", this);
          entity.remove();
        }
      }
    }

    StackedMobDrops.init();

    final PluginManager pm = this.getServer().getPluginManager();
    pm.registerEvents(this, this);

    this.getCommand("stacker").setExecutor(new MobStackerCommand());

    this.mergeTask = scheduler.runTaskTimerAsynchronously(this, new MergeTask(this), 0L, 300L);
    this.clearTask = scheduler.runTaskTimer(this, new ClearTask(), 6000L, 6000L);
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

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onBlockBreakEvent(final BlockBreakEvent event) {
    if (event.getBlock().getType() == Material.MOB_SPAWNER) {
      spawnedTimestamps.remove(event.getBlock().getLocation());
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onSpawnerPreSpawnEvent(final SpawnerPreSpawnEvent event) {
    final Location location = event.getLocation();

    final long allowedSpawnerSpawnTimestamp = spawnedTimestamps.getOrDefault(location, -1L);
    if (allowedSpawnerSpawnTimestamp != -1L && System.currentTimeMillis() < allowedSpawnerSpawnTimestamp) {
      event.setCancelled(true);
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
      final StackedMob stackedMob = StackedMob.getFirstByDistance(event.getEntity(), 8);

      if (stackedMob == null) {
        final int spawns = ThreadLocalRandom.current().nextInt(spawnerSpawnPerMin, spawnerSpawnPerMax);
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

      final int spawns = ThreadLocalRandom.current().nextInt(spawnerSpawnPerMin, spawnerSpawnPerMax);
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
      event.setCancelled(true);

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
      final Player player = (Player) event.getDamager();

      final long allowedHitTimestamp = hitTimestamps.getOrDefault(player.getUniqueId(), -1L);
      if (allowedHitTimestamp != -1 && System.currentTimeMillis() < allowedHitTimestamp) {
        return;
      }

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
          player
        )
      );

      hitTimestamps.put(player.getUniqueId(), System.currentTimeMillis() + stackedMobHitDelay);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onStackedMobDeathEvent(final StackedMobDeathEvent event) {
    if (event.getCause() == StackedMobDeathCause.FALL) {
      final StackedMob stackedMob = event.getStackedMob();
      final int dropAmount = Math.min(event.getAmountDied(), stackedMobMaxDeath);

      stackedMob.destroyEntity();

      for (final ItemStack item : StackedMobDrops.getDropsForEntity(event.getEntityType())) {
        final int amount = item.getAmount() * dropAmount;
        item.setAmount(amount);
        event.getDrops().add(item);
      }

      StackedMob.getStackedMobs().remove(stackedMob.getUniqueId());
    } else if (event.getCause() == StackedMobDeathCause.PLAYER) {
      final StackedMob stackedMob = event.getStackedMob();
      final int decremented = stackedMob.decrementAndGet();

      if (decremented <= 0) {
        stackedMob.destroyEntity();

        for (final ItemStack item : StackedMobDrops.getDropsForEntity(stackedMob.getEntityType())) {
          event.getDrops().add(item);
        }

        StackedMob.getStackedMobs().remove(stackedMob.getUniqueId());
        return;
      }

      stackedMob.getEntity().setCustomName(
        ChatColor.GOLD.toString() + ChatColor.BOLD.toString() + "x" + decremented + " " +
        ChatColor.YELLOW.toString() + ChatColor.BOLD.toString() + stackedMob.getEntityType().name()
      );

      for (final ItemStack item : StackedMobDrops.getDropsForEntity(stackedMob.getEntityType())) {
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

  @EventHandler
  public void onPlayerQuitEvent(final PlayerQuitEvent event) {
    hitTimestamps.remove(event.getPlayer().getUniqueId());
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
