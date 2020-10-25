package me.ufo.mobstacker;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import me.ufo.architect.util.Style;
import me.ufo.mobstacker.commands.MobStackerCommand;
import me.ufo.mobstacker.events.StackedMobDeathEvent;
import me.ufo.mobstacker.mob.StackedMob;
import me.ufo.mobstacker.mob.StackedMobDeathCause;
import me.ufo.mobstacker.mob.StackedMobDrops;
import me.ufo.mobstacker.tasks.ClearTask;
import me.ufo.mobstacker.tasks.MergeTask;
import me.ufo.shaded.it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.techcable.tacospigot.event.entity.SpawnerPreSpawnEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
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

  private final Object2LongOpenHashMap<Location> spawnedTimestamps = new Object2LongOpenHashMap<>();
  private final Object2LongOpenHashMap<UUID> hitTimestamps = new Object2LongOpenHashMap<>();

  /* Delay before SpawnerSpawnEvent */
  private int spawnerActivationDelay;
  /* Spawns per SpawnerSpawnEvent */
  private int spawnerSpawnPerMin;
  private int spawnerSpawnPerMax;
  /* Name for each stack */
  private String stackedMobName;
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

    this.saveDefaultConfig();

    final ConfigurationSection spawner = this.getConfig().getConfigurationSection("spawner");
    spawnerActivationDelay = spawner.getInt("activation-delay", 20);
    if (spawnerActivationDelay != -1) {
      spawnerActivationDelay = spawnerActivationDelay * 1000;
    }
    spawnerSpawnPerMin = spawner.getInt("min-spawn", 2);
    spawnerSpawnPerMax = spawner.getInt("max-spawn", 5);

    final ConfigurationSection mob = this.getConfig().getConfigurationSection("stacked-mob");
    stackedMobName = Style.translate(mob.getString("name", "&6&lx{amount} &e&l{mob}"));
    stackedMobMaxDeath = mob.getInt("max-death-on-fall", 100);
    stackedMobHitDelay = mob.getInt("hit-delay", 250);

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
      spawnedTimestamps.removeLong(event.getBlock().getLocation());
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onSpawnerPreSpawnEvent(final SpawnerPreSpawnEvent event) {
    if (spawnerActivationDelay != -1) {
      final Location location = event.getLocation();

      final long allowedSpawnerSpawnTimestamp = spawnedTimestamps.getOrDefault(location, -1L);
      if (allowedSpawnerSpawnTimestamp != -1L && System.currentTimeMillis() < allowedSpawnerSpawnTimestamp) {
        event.setCancelled(true);
      }
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onSpawnerSpawnEvent(final SpawnerSpawnEvent event) {
    event.setCancelled(true);

    if (spawnerActivationDelay != -1) {
      spawnedTimestamps.put(
        event.getSpawner().getLocation(),
        System.currentTimeMillis() + spawnerActivationDelay
      );
    }

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
          spawnedEntity.setCustomName(this.getStackedMobName(spawns, entity));
        });
        return;
      }

      final int spawns = ThreadLocalRandom.current().nextInt(spawnerSpawnPerMin, spawnerSpawnPerMax);
      final int stacked = stackedMob.addAndGet(spawns);
      //Bukkit.getLogger().info("Found stacked mob with new: " + stacked);

      scheduler.runTask(this, () -> {
        stackedMob.getEntity().setCustomName(this.getStackedMobName(stacked, entity));
      });
    });
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onExplosionPrimeEvent(final EntityExplodeEvent event) {
    final Entity entity = event.getEntity();
    if (entity.getType() != EntityType.CREEPER) {
      return;
    }

    if (!entity.hasMetadata("STACKED_MOB")) {
      return;
    }

    final StackedMob stackedMob = StackedMob.getByEntityId(entity.getUniqueId());

    if (stackedMob == null) {
      return;
    }

    entity.removeMetadata("STACKED_MOB", this);
    StackedMob.getStackedMobs().remove(stackedMob.getUniqueId());
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onEntityDamageEvent(final EntityDamageEvent event) {
    final Entity entity = event.getEntity();
    if (!(entity instanceof LivingEntity) || entity instanceof Player) {
      return;
    }

    if (!entity.hasMetadata("STACKED_MOB")) {
      return;
    }

    if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
      event.setCancelled(true);

      if (event.getDamage() < ((LivingEntity) entity).getHealth()) {
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

      if (stackedMobHitDelay != -1) {
        final long allowedHitTimestamp = hitTimestamps.getOrDefault(player.getUniqueId(), -1L);
        if (allowedHitTimestamp != -1 && System.currentTimeMillis() < allowedHitTimestamp) {
          return;
        }
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

      if (stackedMobHitDelay != -1) {
        hitTimestamps.put(player.getUniqueId(), System.currentTimeMillis() + stackedMobHitDelay);
      }
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onStackedMobDeathEvent(final StackedMobDeathEvent event) {
    if (event.getCause() == StackedMobDeathCause.FALL) {
      final StackedMob stackedMob = event.getStackedMob();
      final int dropAmount = Math.min(event.getAmountDied(), stackedMobMaxDeath);

      stackedMob.destroyEntity();

      for (final ItemStack item : StackedMobDrops.getDropsForEntity(event.getEntityType())) {
        final ItemStack cloned = item.clone();
        cloned.setAmount(item.getAmount() * dropAmount);
        event.getDrops().add(cloned);
      }

      StackedMob.getStackedMobs().remove(stackedMob.getUniqueId());
    } else if (event.getCause() == StackedMobDeathCause.PLAYER) {
      final StackedMob stackedMob = event.getStackedMob();
      final int decremented = stackedMob.decrementAndGet();

      if (decremented <= 0) {
        stackedMob.destroyEntity();

        final StackedMobDrops drops = StackedMobDrops.getFromEntity(stackedMob.getEntityType());

        if (drops != null) {
          for (final ItemStack item : drops.getDrops()) {
            event.getDrops().add(item);
          }

          final int maxXp = drops.getMaxXp();
          if (maxXp == 0) {
            return;
          }

          final int lowXp = drops.getLowXp();
          final int xp = ThreadLocalRandom.current().nextInt(lowXp, maxXp);

          event.getPlayer().giveExp(xp);
        }

        StackedMob.getStackedMobs().remove(stackedMob.getUniqueId());
        return;
      }

      final Entity entity = stackedMob.getEntity();
      entity.setCustomName(this.getStackedMobName(decremented, entity));

      final StackedMobDrops drops = StackedMobDrops.getFromEntity(stackedMob.getEntityType());

      if (drops != null) {
        for (final ItemStack item : drops.getDrops()) {
          event.getDrops().add(item);
        }

        final int maxXp = drops.getMaxXp();
        if (maxXp == 0) {
          return;
        }

        final int lowXp = drops.getLowXp();
        final int xp = ThreadLocalRandom.current().nextInt(lowXp, maxXp);

        event.setXp(xp);
      }
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onStackedMobDeathEventItemsDrop(final StackedMobDeathEvent event) {
    final Location location = event.getLocation();

    for (final ItemStack item : event.getDrops()) {
      location.getWorld().dropItem(location, item);
    }

    final int xp = event.getXp();
    if (xp <= 0) {
      return;
    }

    final Player player = event.getPlayer();
    if (player == null) {
      return;
    }

    player.giveExp(xp);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerQuitEvent(final PlayerQuitEvent event) {
    if (stackedMobHitDelay != -1) {
      hitTimestamps.removeLong(event.getPlayer().getUniqueId());
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onInventoryOpenEvent(final InventoryOpenEvent event) {
    if (event.getInventory().getType() == InventoryType.MERCHANT) {
      event.setCancelled(true);
    }
  }

  private String getStackedMobName(final int amount, final Entity entity) {
    String out = Style.replace(stackedMobName, "{amount}", "" + amount);
    out = Style.replace(out, "{mob}", entity.getType().name());

    return out;
  }

  public BukkitScheduler getScheduler() {
    return scheduler;
  }

}
