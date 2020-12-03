package me.ufo.mobstacker;

import java.util.UUID;
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
import me.ufo.shaded.it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import me.ufo.shaded.it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import me.ufo.shaded.it.unimi.dsi.fastutil.objects.ObjectIterator;
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

  //private ScheduledExecutorService service;
  private Object2LongOpenHashMap<UUID> hitTimestamps;
  private BukkitTask clearTask;
  private BukkitTask mergeTask;

  public boolean clearing = true;
  public boolean merging = true;

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

    final long start = 20L * 30; // 30 seconds
    final BukkitScheduler scheduler = Bukkit.getScheduler();

    // register merge task
    mergeTask = scheduler.runTaskTimer(this, new MergeTask(this), start + 100, 300L /* after 15 seconds */);

    // register clear task
    clearTask = scheduler.runTaskTimer(this, new ClearTask(this), 6000L - (start + 100), 6000L /* after 5 minutes */);

    // clear any mobs and enable spawning
    scheduler.runTaskLater(this, () -> {
      this.clearMobs(true, true);
    }, start /* after 30 seconds */);
  }

  @Override
  public void onDisable() {
    mergeTask.cancel();
    clearTask.cancel();

    this.clearMobs(true, false);
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
          if (entity.isDead() ||
              entity instanceof Player || entity instanceof ArmorStand ||
              (entity.getCustomName() != null && !entity.getCustomName().isEmpty()) ||
              entity.hasMetadata("NPC")) {
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
