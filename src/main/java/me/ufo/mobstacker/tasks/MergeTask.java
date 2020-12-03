package me.ufo.mobstacker.tasks;

import java.util.NoSuchElementException;
import java.util.UUID;
import me.ufo.shaded.it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import me.ufo.shaded.it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.ufo.shaded.it.unimi.dsi.fastutil.objects.ObjectIterator;
import me.ufo.mobstacker.MSPlugin;
import me.ufo.mobstacker.mob.StackedMob;
import me.ufo.shaded.it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

public final class MergeTask implements Runnable {

  private final MSPlugin plugin;

  public MergeTask(final MSPlugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public void run() {
    if (!plugin.merging) {
      plugin.getLogger().info("== SKIPPING MERGING ==");
      return;
    }

    // if spawning is enabled/disabled
    final boolean after = plugin.isSpawning();
    plugin.setSpawning(false);

    if (plugin.isDebugging()) {
      plugin.getLogger().info("== MERGING START ==");
    }

    int failed = 0;
    int merge = 0;
    int dead = 0;

    final ObjectOpenHashSet<UUID> toRemove = new ObjectOpenHashSet<>();

    final Object2ObjectOpenHashMap<UUID, StackedMob> stackedMobs = StackedMob.getStackedMobs();
    final ObjectIterator<Object2ObjectMap.Entry<UUID, StackedMob>> mobIterator =
      stackedMobs.object2ObjectEntrySet().fastIterator();

    try {
      while (mobIterator.hasNext()) {
        final Object2ObjectMap.Entry<UUID, StackedMob> entry = mobIterator.next();

        final StackedMob current = entry.getValue();

        if (current == null) {
          failed++;
          mobIterator.remove();
          continue;
        }

        if (toRemove.remove(entry.getKey())) {
          merge++;
          mobIterator.remove();
          continue;
        }

        if (current.getEntity().isDead()) {
          dead++;
          mobIterator.remove();
          continue;
        }

        final ObjectOpenHashSet<StackedMob> nearby;
        try {
          nearby = StackedMob.getAllByDistance(current, 8);
        } catch (final IndexOutOfBoundsException | NoSuchElementException e) {
          if (plugin.isDebugging()) {
            plugin.getLogger().info("NON-FATAL ERROR (IGNORE): [MergeTask L72] " +
                                    e.getClass().getSimpleName());
          }
          failed++;
          continue;
        } catch (final NullPointerException e) {
          failed++;
          continue;
        }

        if (!nearby.isEmpty()) {
          for (final StackedMob near : nearby) {
            current.addAndGet(near.getStackedAmount());
            //toRemove.add(near.getUniqueId());
            near.destroyEntity();

            //near.destroyEntity();
            merge++;
          }

          //plugin.syncTask(() -> {
          current.setCustomName();

          //for (final StackedMob near : nearby) {
          //  near.destroyEntity();
          //}
          //});
        }
      }

      // clean up
      /*for (final UUID uuid : toRemove) {
        final StackedMob stackedMob = stackedMobs.get(uuid);

        if (stackedMob == null) {
          continue;
        }

        stackedMob.destroyEntity();
        stackedMobs.remove(uuid);
      }*/
    } catch (final Throwable throwable) {
      if (plugin.isDebugging()) {
        plugin.getLogger().info("NON-CAUGHT ERROR");
        throwable.printStackTrace();
      }
    }

    if (plugin.isDebugging()) {
      plugin.getLogger().info("failed: " + failed);
      plugin.getLogger().info("merge: " + merge);
      plugin.getLogger().info("dead: " + dead);
      plugin.getLogger().info("current: " + stackedMobs.size());
      plugin.getLogger().info("== MERGING END ==");
    }

    plugin.setSpawning(after);
  }

}
