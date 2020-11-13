package me.ufo.mobstacker;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import me.ufo.mobstacker.commands.MSCommand;
import me.ufo.mobstacker.listeners.EntityListener;
import me.ufo.mobstacker.listeners.MiscListener;
import me.ufo.mobstacker.listeners.SpawnerListener;
import me.ufo.mobstacker.listeners.StackedMobListener;
import me.ufo.mobstacker.mob.StackedMob;
import me.ufo.mobstacker.mob.StackedMobDrops;
import me.ufo.mobstacker.tasks.ClearTask;
import me.ufo.mobstacker.tasks.MergeTask;
import me.ufo.shaded.com.google.common.util.concurrent.ThreadFactoryBuilder;
import me.ufo.shaded.it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

public final class MSPlugin extends JavaPlugin implements Listener {

  private static MSPlugin PLUGIN;

  private final AtomicBoolean debug = new AtomicBoolean(false);
  private final AtomicBoolean spawning = new AtomicBoolean(false);

  private final BukkitScheduler scheduler = Bukkit.getScheduler();

  private ScheduledExecutorService service;
  private Object2LongOpenHashMap<UUID> hitTimestamps;
  private BukkitTask clearTask;

  public MSPlugin() {
    PLUGIN = this;
    this.saveDefaultConfig();
  }

  @Override
  public void onEnable() {
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

    // register thread pool
    service = Executors.newScheduledThreadPool(
      this.getConfig().getInt("thread-count", 2),
      new ThreadFactoryBuilder().setNameFormat("MobStacker Worker #%d").build());

    // register merge task
    service.scheduleAtFixedRate(new MergeTask(this), 15, 15, TimeUnit.SECONDS);

    // register clear task
    clearTask = scheduler.runTaskTimer(this, new ClearTask(this), 0L, 6000L);

    // clear any mobs and enable spawning
    scheduler.runTaskLater(this, () -> {
      this.clearMobs(true, true);
    }, 600L /* after 30 seconds */);
  }

  @Override
  public void onDisable() {
    clearTask.cancel();
    this.clearMobs(true, false);

    service.shutdown();

    try {
      service.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    } catch (final InterruptedException ignored) {
      // do nothing
    }
  }

  // clear any mobs and then set spawning to what it was
  public void clearMobs(final boolean all) {
    if (spawning.get()) {
      this.clearMobs(true, true);
    } else {
      this.clearMobs(all, false);
    }
  }

  public void clearMobs(final boolean all, final boolean after) {
    spawning.set(false);

    final Iterator<Map.Entry<UUID, StackedMob>> mobIterator =
      StackedMob.getStackedMobs().entrySet().iterator();

    while (mobIterator.hasNext()) {
      final StackedMob current = mobIterator.next().getValue();
      current.destroyEntity();
      mobIterator.remove();
    }

    if (all) {
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

    spawning.set(after);
  }

  public void syncTask(final Runnable task) {
    scheduler.runTask(this, task);
  }

  public ScheduledExecutorService getService() {
    return service;
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
    return PLUGIN;
  }

}
