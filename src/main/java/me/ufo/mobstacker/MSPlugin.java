package me.ufo.mobstacker;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import me.ufo.architect.util.Style;
import me.ufo.mobstacker.commands.MobStackerCommand;
import me.ufo.mobstacker.events.StackedMobDeathEvent;
import me.ufo.mobstacker.mob.StackedMob;
import me.ufo.mobstacker.mob.StackedMobDeathCause;
import me.ufo.mobstacker.mob.StackedMobDrops;
import me.ufo.mobstacker.tasks.ClearTask;
import me.ufo.mobstacker.tasks.MergeTask;
import me.ufo.shaded.com.google.common.util.concurrent.ThreadFactoryBuilder;
import me.ufo.shaded.it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

public final class MSPlugin extends JavaPlugin implements Listener {

  private static MSPlugin instance;

  public static MSPlugin getInstance() {
    return instance;
  }

  private ScheduledExecutorService service;
  private Object2LongOpenHashMap<UUID> hitTimestamps;
  private BukkitScheduler scheduler;
  private BukkitTask clearTask;
  private boolean loaded;
  private final AtomicBoolean debug = new AtomicBoolean(false);
  private final AtomicBoolean removing = new AtomicBoolean(false);

  /* CONFIG VALUES */
  private int SPAWN_MIN;
  private int SPAWN_MAX;
  private String MOB_NAME;
  private int MAX_DEATHS;
  private int HIT_DELAY;

  @Override
  public void onEnable() {
    instance = this;

    this.saveDefaultConfig();

    final int threads = this.getConfig().getInt("thread-count", 2);
    this.service = Executors.newScheduledThreadPool(threads, new ThreadFactoryBuilder()
      .setNameFormat("MobStacker Worker #%d").build());

    final ConfigurationSection spawner = this.getConfig().getConfigurationSection("spawner");
    SPAWN_MIN = spawner.getInt("min-spawn", 2);
    SPAWN_MAX = spawner.getInt("max-spawn", 5);

    final ConfigurationSection mob = this.getConfig().getConfigurationSection("stacked-mob");
    MOB_NAME = Style.translate(mob.getString("name", "&6&lx{amount} &e&l{mob}"));
    MAX_DEATHS = mob.getInt("max-death-on-fall", 100);

    HIT_DELAY = mob.getInt("hit-delay", 250);
    if (HIT_DELAY != -1) {
      this.hitTimestamps = new Object2LongOpenHashMap<>();
    }

    scheduler = this.getServer().getScheduler();

    for (final World world : this.getServer().getWorlds()) {
      for (final LivingEntity entity : world.getLivingEntities()) {
        if (entity instanceof Player || entity instanceof ArmorStand || entity.hasMetadata("NPC")) {
          continue;
        }

        if (entity.hasMetadata(StackedMob.METADATA_KEY)) {
          entity.removeMetadata(StackedMob.METADATA_KEY, this);
        }

        entity.remove();
      }
    }

    StackedMobDrops.init();

    final PluginManager pm = this.getServer().getPluginManager();
    pm.registerEvents(this, this);

    this.getCommand("stacker").setExecutor(new MobStackerCommand());

    //this.mergeTask = scheduler.runTaskTimerAsynchronously(this, new MergeTask(this), 0L, 300L);
    this.clearTask = scheduler.runTaskTimer(this, new ClearTask(this), 6000L, 6000L);

    service.scheduleAtFixedRate(new MergeTask(this), 15, 15, TimeUnit.SECONDS);

    scheduler.runTaskLater(this, () -> {
      loaded = true;
    }, 600L);
  }

  @Override
  public void onDisable() {
    final Iterator<Map.Entry<UUID, StackedMob>> iterator =
      StackedMob.getStackedMobs().entrySet().iterator();

    while (iterator.hasNext()) {
      final Map.Entry<UUID, StackedMob> entry = iterator.next();

      entry.getValue().destroyEntity();
      iterator.remove();
    }

    this.service.shutdown();

    try {
      this.service.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    } catch (final InterruptedException ignored) {
    }


    this.clearTask.cancel();

    for (final World world : this.getServer().getWorlds()) {
      for (final LivingEntity entity : world.getLivingEntities()) {
        if (entity instanceof Player || entity instanceof ArmorStand || entity.hasMetadata("NPC")) {
          continue;
        }

        if (entity.hasMetadata(StackedMob.METADATA_KEY)) {
          entity.removeMetadata(StackedMob.METADATA_KEY, this);
        }

        entity.remove();
      }
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onCreatureSpawnEvent(final CreatureSpawnEvent event) {
    if (!loaded || removing.get()) {
      event.setCancelled(true);
      return;
    }

    final CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();
    if (reason == CreatureSpawnEvent.SpawnReason.MOUNT || reason == CreatureSpawnEvent.SpawnReason.JOCKEY) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onSpawnerSpawnEvent(final SpawnerSpawnEvent event) {
    if (!loaded || removing.get()) {
      event.setCancelled(true);
      return;
    }

    event.setCancelled(true);

    final Entity entity = event.getEntity();

    if (entity.getPassenger() != null) {
      entity.remove();
      entity.getPassenger().remove();
      return;
    }

    final Location location = event.getLocation();

    service.execute(() -> {
      try {
        //final long now = System.nanoTime();
        final StackedMob stackedMob = StackedMob.getFirstByDistance(entity, 16);
        //final long complete = System.nanoTime();

        //instance.getLogger().info("StackedMob#getFirstByDistance took " + ((double) (complete - now) /
        // 1_000_000_000) + " seconds.");

        final int spawns = ThreadLocalRandom.current().nextInt(SPAWN_MIN, SPAWN_MAX);

        if (stackedMob == null || stackedMob.getEntity() == null || stackedMob.getEntity().isDead()) {
          scheduler.runTask(this, () -> {
            //Bukkit.getLogger().info("No stack found, creating stack: ");
            final Entity spawnedEntity = location.getWorld().spawnEntity(location, entity.getType());

            final UUID uniqueId = spawnedEntity.getUniqueId();

            StackedMob.getStackedMobs().put(uniqueId, new StackedMob(spawnedEntity, spawns));
          });
          return;
        }

        stackedMob.addSetAndGet(spawns);
      } catch (final Throwable throwable) {
        if (instance.isDebug()) {
          instance.getLogger()
            .info("NON-FATAL ERROR (IGNORE): [MSPlugin L243] " + throwable.getClass().getSimpleName());
          //throwable.printStackTrace();
        }
      }
    });
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

    final StackedMob stackedMob = StackedMob.getByEntityId(entity.getUniqueId());

    if (stackedMob == null) {
      return;
    }

    entity.removeMetadata(StackedMob.METADATA_KEY, this);
    StackedMob.getStackedMobs().remove(stackedMob.getUniqueId());
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

      final StackedMob stackedMob = StackedMob.getByEntityId(entity.getUniqueId());

      if (stackedMob == null) {
        entity.removeMetadata(StackedMob.METADATA_KEY, this);
        entity.remove();
        return;
      }

      Bukkit.getPluginManager().callEvent(
        new StackedMobDeathEvent(
          stackedMob,
          Math.min(stackedMob.getStackedAmount(), MAX_DEATHS),
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

      if (HIT_DELAY != -1) {
        final long allowedHitTimestamp = hitTimestamps.getOrDefault(player.getUniqueId(), -1L);
        if (allowedHitTimestamp != -1 && System.currentTimeMillis() < allowedHitTimestamp) {
          return;
        }
      }

      final StackedMob stackedMob = StackedMob.getByEntityId(entity.getUniqueId());

      if (stackedMob == null) {
        entity.removeMetadata(StackedMob.METADATA_KEY, this);
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

      if (HIT_DELAY != -1) {
        hitTimestamps.put(player.getUniqueId(), System.currentTimeMillis() + HIT_DELAY);
      }
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onStackedMobDeathEvent(final StackedMobDeathEvent event) {
    if (event.getCause() == StackedMobDeathCause.FALL) {
      final StackedMob stackedMob = event.getStackedMob();
      final int dropAmount = Math.min(event.getAmountDied(), MAX_DEATHS);

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

          final int xp;
          if (maxXp != lowXp) {
            xp = ThreadLocalRandom.current().nextInt(lowXp, maxXp);
          } else {
            xp = maxXp;
          }

          event.getPlayer().giveExp(xp);
        }

        StackedMob.getStackedMobs().remove(stackedMob.getUniqueId());
        return;
      }

      stackedMob.setCustomName();

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

        final int xp;
        if (maxXp != lowXp) {
          xp = ThreadLocalRandom.current().nextInt(lowXp, maxXp);
        } else {
          xp = maxXp;
        }

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
    if (HIT_DELAY != -1) {
      hitTimestamps.removeLong(event.getPlayer().getUniqueId());
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onInventoryOpenEvent(final InventoryOpenEvent event) {
    if (event.getInventory().getType() == InventoryType.MERCHANT) {
      event.setCancelled(true);
    }
  }

  public String getStackedMobName(final int amount, final Entity entity) {
    String out = Style.replace(MOB_NAME, "{amount}", "" + amount);
    out = Style.replace(out, "{mob}", entity.getType().name());

    return out;
  }

  public AtomicBoolean getRemoving() {
    return removing;
  }

  public boolean isDebug() {
    return debug.get();
  }

  public void setDebug(final boolean debug) {
    this.debug.set(debug);
  }

  public BukkitScheduler getScheduler() {
    return scheduler;
  }

}
