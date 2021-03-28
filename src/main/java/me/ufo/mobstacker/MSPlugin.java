package me.ufo.mobstacker;

import me.ufo.mobstacker.commands.MSCommand;
import me.ufo.mobstacker.listeners.EntityListener;
import me.ufo.mobstacker.listeners.MiscListener;
import me.ufo.mobstacker.listeners.SpawnerListener;
import me.ufo.mobstacker.listeners.StackedMobListener;
import me.ufo.mobstacker.mob.StackedMob;
import me.ufo.mobstacker.mob.StackedMobDrops;
import me.ufo.mobstacker.tasks.ClearTask;
import me.ufo.mobstacker.tasks.MergeTask;
import me.ufo.shaded.it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import me.ufo.shaded.it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import me.ufo.shaded.it.unimi.dsi.fastutil.objects.ObjectIterator;
import me.ufo.shaded.org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public final class MSPlugin extends JavaPlugin {

  private static MSPlugin plugin;

  private final AtomicBoolean debug = new AtomicBoolean(false);
  private final AtomicBoolean spawning = new AtomicBoolean(false);

  //private ScheduledExecutorService service;
  private Object2LongOpenHashMap<UUID> hitTimestamps;
  private BukkitTask clearTask;
  private BukkitTask mergeTask;

  public boolean clearing = true;
  public boolean merging = true;

  public MSPlugin() {
    plugin = this;
    this.saveDefaultConfig();
  }

  @Override
  public void onEnable() {
    final BukkitScheduler scheduler = Bukkit.getScheduler();

    // clear any mobs
    scheduler.runTaskLater(this, () -> {
      this.getLogger().info("Initial clear all mobs");
      this.clearMobs(true, false, true);
    }, 20L);

    // load config
    Config.load(this);

    if (Config.HIT_DELAY != -1) {
      hitTimestamps = new Object2LongOpenHashMap<>();
    }

    // register mob drops
    StackedMobDrops.init();

    // register commands
    this.getCommand("stacker").setExecutor(new MSCommand(this));

    // register events
    final PluginManager pm = this.getServer().getPluginManager();
    pm.registerEvents(new SpawnerListener(this), this);
    pm.registerEvents(new EntityListener(this), this);
    pm.registerEvents(new StackedMobListener(this), this);
    pm.registerEvents(new MiscListener(this), this);

    final long start = 20L * 30; // 30 seconds

    // register merge task
    mergeTask = scheduler.runTaskTimer(this, new MergeTask(this), start + 100 /* after 35 seconds */, 300L /* after 15 seconds */);

    // register clear task
    clearTask = scheduler.runTaskTimer(this, new ClearTask(this), 6000L - (start + 100) /* after 4 minutes 35 seconds */, 6000L /* after 5 minutes */);

    // enable spawning
    scheduler.runTaskLater(this, () -> {
      spawning.set(true);
    }, start /* after 30 seconds */);
  }

  @Override
  public void onDisable() {
    spawning.set(false);
    merging = false;
    clearing = false;

    mergeTask.cancel();
    clearTask.cancel();

    mergeTask = null;
    clearTask = null;

    plugin = null;
  }

  // clear any mobs and then set spawning to what it was
  public void clearMobs(final boolean all) {
    if (spawning.get()) {
      this.clearMobs(true, true, false);
    } else {
      this.clearMobs(all, false, false);
    }
  }

  public void clearMobs(final boolean all, final boolean after, final boolean force) {
    spawning.set(false);

    final ObjectIterator<Object2ObjectMap.Entry<UUID, StackedMob>> mobIterator =
        StackedMob.getStackedMobs().object2ObjectEntrySet().fastIterator();

    while (mobIterator.hasNext()) {
      final StackedMob current = mobIterator.next().getValue();
      current.destroyEntity();
      mobIterator.remove();
    }

    if (all) {
      for (final World world : this.getServer().getWorlds()) {
        for (final LivingEntity entity : world.getLivingEntities()) {
          final EntityType type = entity.getType();
          if (type == EntityType.PLAYER || type == EntityType.ARMOR_STAND || type == EntityType.ARROW ||
              type == EntityType.GIANT) {
            continue;
          }

          if (entity.hasMetadata(StackedMob.METADATA_KEY)) {
            entity.removeMetadata(StackedMob.METADATA_KEY, this);
            entity.remove();
            continue;
          }

          if (entity.isDead()) {
            continue;
          }

          if (entity.hasMetadata("NPC")) {
            continue;
          }

          if (!force) {
            if (entity.getCustomName() != null && !entity.getCustomName().isEmpty()) {
              if (!StringUtils.contains(entity.getCustomName(), type.name())) {
                continue;
              }
            }
          }

          entity.remove();
        }
      }
    }

    spawning.set(after);
  }

  public Object2LongOpenHashMap<UUID> getHitTimestamps() {
    return hitTimestamps;
  }

  public boolean isSpawning() {
    return spawning.get();
  }

  public void setSpawning(final boolean bool) {
    spawning.set(bool);
  }

  public boolean toggleSpawning() {
    spawning.set(!spawning.get());

    return spawning.get();
  }

  public boolean isDebugging() {
    return debug.get();
  }

  public void setDebugging(final boolean bool) {
    debug.set(bool);
  }

  public static MSPlugin get() {
    return plugin;
  }

}
